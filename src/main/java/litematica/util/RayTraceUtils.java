package litematica.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import malilib.util.data.EnabledCondition;
import malilib.util.game.RayTraceUtils.RayTraceCalculationData;
import malilib.util.game.RayTraceUtils.RayTraceFluidHandling;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.position.BlockPos;
import malilib.util.position.Direction;
import malilib.util.position.HitResult;
import malilib.util.position.LayerRange;
import malilib.util.position.Vec3d;
import litematica.config.Configs;
import litematica.config.Hotkeys;
import litematica.data.DataManager;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.verifier.BlockPairTypePosition;
import litematica.selection.AreaSelection;
import litematica.selection.BoxCorner;
import litematica.selection.SelectionBox;
import litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import litematica.world.SchematicWorldHandler;

public class RayTraceUtils
{
    private static RayTraceWrapper closestBox;
    private static RayTraceWrapper closestCorner;
    private static RayTraceWrapper closestOrigin;
    private static double closestBoxDistance;
    private static double closestCornerDistance;
    private static double closestOriginDistance;
    private static HitType originType;

    @Nullable
    public static BlockPos getTargetedPosition(World world, Entity entity, double maxDistance, boolean sneakToOffset)
    {
        HitResult trace = malilib.util.game.RayTraceUtils
                .getRayTraceFromEntity(world, entity, RayTraceFluidHandling.NONE, false, maxDistance);

        if (trace.type != HitResult.Type.BLOCK)
        {
            return null;
        }

        BlockPos pos = trace.blockPos;

        // Sneaking puts the position adjacent the targeted block face, not sneaking puts it inside the targeted block
        if (sneakToOffset == entity.isSneaking())
        {
            pos = pos.offset(trace.side);
        }

        return pos;
    }

    @Nonnull
    public static RayTraceWrapper getWrappedRayTraceFromEntity(World world, Entity entity, double range)
    {
        Vec3d eyesPos = EntityWrap.getEntityEyePos(entity);
        Vec3d look = EntityWrap.getScaledLookVector(entity, range);
        Vec3d lookEndPos = eyesPos.add(look);

        HitResult result = malilib.util.game.RayTraceUtils
                .getRayTraceFromEntity(world, entity, RayTraceFluidHandling.NONE, false, range);
        double closestVanilla = result.type != HitResult.Type.MISS ? result.pos.distanceTo(eyesPos) : -1D;

        AreaSelection area = DataManager.getAreaSelectionManager().getCurrentSelection();
        RayTraceWrapper wrapper = null;

        clearTraceVars();

        if (DataManager.getToolMode().getUsesSchematic() == false && area != null)
        {
            for (SelectionBox box : area.getAllSelectionBoxes())
            {
                boolean hitCorner = false;
                hitCorner |= traceToSelectionBoxCorner(box, BoxCorner.CORNER_1, eyesPos, lookEndPos);
                hitCorner |= traceToSelectionBoxCorner(box, BoxCorner.CORNER_2, eyesPos, lookEndPos);

                if (hitCorner == false)
                {
                    traceToSelectionBoxBody(box, eyesPos, lookEndPos);
                }
            }

            BlockPos origin = area.getManualOrigin();

            if (origin != null)
            {
                traceToPosition(origin, eyesPos, lookEndPos, HitType.SELECTION_ORIGIN, null);
            }
        }

        if (DataManager.getToolMode().getUsesSchematic())
        {
            for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getVisibleSchematicPlacements())
            {
                if (placement.isEnabled())
                {
                    traceToPlacementBox(placement, eyesPos, lookEndPos);
                    traceToPosition(placement.getPosition(), eyesPos, lookEndPos, HitType.PLACEMENT_ORIGIN, placement);
                }
            }
        }

        double closestDistance = closestVanilla;

        if (closestBoxDistance >= 0 && (closestVanilla < 0 || closestBoxDistance <= closestVanilla))
        {
            closestDistance = closestBoxDistance;
            wrapper = closestBox;
        }

        // Corners are preferred over box body hits, thus this being after the box check
        if (closestCornerDistance >= 0 && (closestVanilla < 0 || closestCornerDistance <= closestVanilla))
        {
            closestDistance = closestCornerDistance;
            wrapper = closestCorner;
        }

        // Origins are preferred over everything else
        if (closestOriginDistance >= 0 && (closestVanilla < 0 || closestOriginDistance <= closestVanilla))
        {
            closestDistance = closestOriginDistance;

            if (originType == HitType.PLACEMENT_ORIGIN)
            {
                wrapper = closestOrigin;
            }
            else
            {
                wrapper = new RayTraceWrapper(RayTraceWrapper.HitType.SELECTION_ORIGIN);
            }
        }

        clearTraceVars();

        if (wrapper == null || closestDistance < 0)
        {
            wrapper = new RayTraceWrapper();
        }

        return wrapper;
    }

    private static void clearTraceVars()
    {
        closestBox = null;
        closestCorner = null;
        closestOrigin = null;
        closestBoxDistance = -1D;
        closestCornerDistance = -1D;
        closestOriginDistance = -1D;
    }

    private static boolean traceToSelectionBoxCorner(SelectionBox box, BoxCorner corner, Vec3d start, Vec3d end)
    {
        BlockPos pos = box.getCornerPosition(corner);

        if (pos != null)
        {
            AxisAlignedBB bb = PositionUtils.createAABBForPosition(pos);
            RayTraceResult hit = bb.calculateIntercept(start.toVanilla(), end.toVanilla());

            if (hit != null)
            {
                double dist = hit.hitVec.distanceTo(start.toVanilla());

                if (closestCornerDistance < 0 || dist < closestCornerDistance)
                {
                    closestCornerDistance = dist;
                    closestCorner = new RayTraceWrapper(box, corner, Vec3d.of(hit.hitVec));
                }

                return true;
            }
        }

        return false;
    }

    private static boolean traceToSelectionBoxBody(SelectionBox box, Vec3d start, Vec3d end)
    {
        AxisAlignedBB bb = PositionUtils.createEnclosingAABB(box.getCorner1(), box.getCorner2());
        RayTraceResult hit = bb.calculateIntercept(start.toVanilla(), end.toVanilla());

        if (hit != null)
        {
            double dist = hit.hitVec.distanceTo(start.toVanilla());

            if (closestBoxDistance < 0 || dist < closestBoxDistance)
            {
                closestBoxDistance = dist;
                closestBox = new RayTraceWrapper(box, BoxCorner.NONE, Vec3d.of(hit.hitVec));
            }

            return true;
        }

        return false;
    }

    private static boolean traceToPlacementBox(SchematicPlacement placement, Vec3d start, Vec3d end)
    {
        net.minecraft.util.math.Vec3d startVanilla = start.toVanilla();
        net.minecraft.util.math.Vec3d endVanilla = end.toVanilla();
        ImmutableMap<String, SelectionBox> boxes = placement.getSubRegionBoxes(EnabledCondition.ENABLED);
        boolean hitSomething = false;

        for (Map.Entry<String, SelectionBox> entry : boxes.entrySet())
        {
            String boxName = entry.getKey();
            SelectionBox box = entry.getValue();

            AxisAlignedBB bb = PositionUtils.createEnclosingAABB(box.getCorner1(), box.getCorner2());
            RayTraceResult trace = bb.calculateIntercept(startVanilla, endVanilla);

            if (trace != null)
            {
                double dist = trace.hitVec.distanceTo(startVanilla);

                if (closestBoxDistance < 0 || dist < closestBoxDistance)
                {
                    closestBoxDistance = dist;
                    closestBox = new RayTraceWrapper(placement, Vec3d.of(trace.hitVec), boxName);
                    hitSomething = true;
                }
            }
        }

        return hitSomething;
    }

    private static boolean traceToPosition(BlockPos pos, Vec3d start, Vec3d end, HitType type, @Nullable SchematicPlacement placement)
    {
        AxisAlignedBB bb = PositionUtils.createAABBForPosition(pos);
        RayTraceResult trace = bb.calculateIntercept(start.toVanilla(), end.toVanilla());

        if (trace != null)
        {
            double dist = trace.hitVec.distanceTo(start.toVanilla());

            if (closestOriginDistance < 0 || dist < closestOriginDistance)
            {
                closestOriginDistance = dist;
                originType = type;

                if (type == HitType.PLACEMENT_ORIGIN)
                {
                    closestOrigin = new RayTraceWrapper(placement, Vec3d.of(trace.hitVec), null);
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Ray traces to the closest position on the given list
     */
    @Nullable
    public static HitResult traceToPositions(List<BlockPos> posList, Entity entity, double range)
    {
        if (posList.isEmpty())
        {
            return null;
        }

        Vec3d eyesPos = EntityWrap.getEntityEyePos(entity);
        Vec3d look = EntityWrap.getScaledLookVector(entity, range);
        Vec3d lookEndPos = eyesPos.add(look);

        double closest = -1D;
        HitResult trace = null;

        for (BlockPos pos : posList)
        {
            if (pos != null)
            {
                AxisAlignedBB bb = PositionUtils.createAABBForPosition(pos);
                RayTraceResult hit = bb.calculateIntercept(eyesPos.toVanilla(), lookEndPos.toVanilla());

                if (hit != null)
                {
                    double dist = hit.hitVec.distanceTo(eyesPos.toVanilla());

                    if (closest < 0 || dist < closest)
                    {
                        trace = HitResult.block(pos, Direction.of(hit.sideHit), Vec3d.of(hit.hitVec));
                        closest = dist;
                    }
                }
            }
        }

        return trace;
    }

    /**
     * Ray traces to the closest position on the given list
     */
    @Nullable
    public static BlockPairTypePosition traceToVerifierResultPositions(List<BlockPairTypePosition> posList, Entity entity, double range)
    {
        if (posList.isEmpty())
        {
            return null;
        }

        Vec3d eyesPos = EntityWrap.getEntityEyePos(entity);
        Vec3d look = EntityWrap.getScaledLookVector(entity, range);
        Vec3d lookEndPos = eyesPos.add(look);

        BlockPairTypePosition closestPos = null;
        double closest = -1D;

        for (BlockPairTypePosition pos : posList)
        {
            AxisAlignedBB bb = PositionUtils.createAABBForPosition(pos.posLong);
            RayTraceResult hit = bb.calculateIntercept(eyesPos.toVanilla(), lookEndPos.toVanilla());

            if (hit != null)
            {
                double dist = hit.hitVec.distanceTo(eyesPos.toVanilla());

                if (closest < 0 || dist < closest)
                {
                    closestPos = pos;
                    closest = dist;
                }
            }
        }

        return closestPos;
    }

    @Nullable
    public static HitResult traceToSchematicWorld(Entity entity, double range, boolean respectRenderRange)
    {
        World world = SchematicWorldHandler.getSchematicWorld();
        boolean invert = Hotkeys.INVERT_SCHEMATIC_RENDER_STATE.getKeyBind().isKeyBindHeld();

        if (world == null ||
            (respectRenderRange &&
                (Configs.Visuals.MAIN_RENDERING_TOGGLE.getBooleanValue() == false ||
                 Configs.Visuals.SCHEMATIC_RENDERING.getBooleanValue() == invert)))
        {
            return null;
        }

        Vec3d start = EntityWrap.getEntityEyePos(entity);
        Vec3d look = EntityWrap.getScaledLookVector(entity, range);
        Vec3d end = start.add(look);

        LayerRange layerRange = respectRenderRange ? DataManager.getRenderLayerRange() : null;

        return malilib.util.game.RayTraceUtils.rayTraceBlocks(world, start, end,
                                                              malilib.util.game.RayTraceUtils::checkRayCollision,
                                                              malilib.util.game.RayTraceUtils.RayTraceFluidHandling.SOURCE_ONLY,
                                                              malilib.util.game.RayTraceUtils.BLOCK_FILTER_NON_AIR,
                                                              false, true, layerRange, 200);
    }

    @Nullable
    public static RayTraceWrapper getGenericTrace(World worldClient,
                                                  Entity entity,
                                                  double range,
                                                  boolean respectRenderRange)
    {
        return getGenericTrace(worldClient, entity, range, respectRenderRange, RayTraceFluidHandling.ANY);
    }

    @Nullable
    public static RayTraceWrapper getGenericTrace(World worldClient,
                                                  Entity entity,
                                                  double range,
                                                  boolean respectRenderRange,
                                                  RayTraceFluidHandling fluidHandling)
    {
        HitResult traceClient = malilib.util.game.RayTraceUtils
                .getRayTraceFromEntity(worldClient, entity, fluidHandling, false, range);
        HitResult traceSchematic = traceToSchematicWorld(entity, range, respectRenderRange);
        Vec3d eyesPos = EntityWrap.getEntityEyePos(entity);
        HitType type = HitType.MISS;
        HitResult trace = null;
        double distClosest = -1D;

        if (traceSchematic != null && traceSchematic.type == HitResult.Type.BLOCK)
        {
            double dist = eyesPos.squareDistanceTo(traceSchematic.pos);

            if (distClosest < 0 || dist < distClosest)
            {
                trace = traceSchematic;
                distClosest = eyesPos.squareDistanceTo(traceSchematic.pos);
                type = HitType.SCHEMATIC_BLOCK;
            }
        }

        if (traceClient.type == HitResult.Type.BLOCK)
        {
            double dist = eyesPos.squareDistanceTo(traceClient.pos);

            if (distClosest < 0 || dist < distClosest)
            {
                trace = traceClient;
                type = HitType.VANILLA;
            }
        }

        if (trace != null)
        {
            return new RayTraceWrapper(type, trace);
        }

        return null;
    }

    @Nullable
    public static RayTraceWrapper getSchematicWorldTraceWrapperIfClosest(World worldClient, Entity entity, double range)
    {
        RayTraceWrapper trace = getGenericTrace(worldClient, entity, range, true);

        if (trace != null && trace.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            return trace;
        }

        return null;
    }

    @Nullable
    public static BlockPos getSchematicWorldTraceIfClosest(World worldClient, Entity entity, double range)
    {
        RayTraceWrapper trace = getSchematicWorldTraceWrapperIfClosest(worldClient, entity, range);
        return trace != null ? BlockPos.of(trace.getRayTraceResult().blockPos) : null;
    }

    @Nullable
    public static BlockPos getPickBlockLastTrace(World worldClient, Entity entity, double maxRange, boolean adjacentOnly)
    {
        Vec3d eyesPos = EntityWrap.getEntityEyePos(entity);
        Vec3d look = EntityWrap.getScaledLookVector(entity, maxRange);
        Vec3d lookEndPos = eyesPos.add(look);

        HitResult traceVanilla = malilib.util.game.RayTraceUtils.getRayTraceFromEntity(worldClient, entity, RayTraceFluidHandling.NONE, false, maxRange);

        if (traceVanilla.type != HitResult.Type.BLOCK)
        {
            return null;
        }

        final double closestVanilla = traceVanilla.pos.squareDistanceTo(eyesPos);

        BlockPos closestVanillaPos = traceVanilla.blockPos;
        World worldSchematic = SchematicWorldHandler.getSchematicWorld();
        List<HitResult> list = rayTraceSchematicWorldBlocksToList(worldSchematic, eyesPos, lookEndPos, 24);
        HitResult furthestTrace = null;
        double furthestDist = -1D;
        boolean vanillaPosReplaceable = worldClient.getBlockState(closestVanillaPos).getBlock().isReplaceable(worldClient, closestVanillaPos);

        if (list.isEmpty() == false)
        {
            for (HitResult trace : list)
            {
                double dist = trace.pos.squareDistanceTo(eyesPos);
                BlockPos pos = trace.blockPos;

                // Comparing with >= instead of > fixes the case where the player's head is inside the first schematic block,
                // in which case the distance to the block at index 0 is the same as the block at index 1, since
                // the trace leaves the first block at the same point where it enters the second block.
                if ((furthestDist < 0 || dist >= furthestDist) &&
                    (closestVanilla < 0 || dist < closestVanilla || (pos.equals(closestVanillaPos) && vanillaPosReplaceable)) &&
                     (vanillaPosReplaceable || pos.equals(closestVanillaPos) == false))
                {
                    furthestDist = dist;
                    furthestTrace = trace;
                }

                if (closestVanilla >= 0 && dist > closestVanilla)
                {
                    break;
                }
            }
        }

        // Didn't trace to any schematic blocks, but hit a vanilla block.
        // Check if there is a schematic block adjacent to the vanilla block
        // (which means that it has a non-full-cube collision box, since
        // it wasn't hit by the trace), and no block in the client world.
        // Note that this method is only used for the "pickBlockLast" type
        // of pick blocking, not for the "first" variant, where this would
        // probably be annoying if you want to pick block the client world block.
        if (furthestTrace == null)
        {
            BlockPos pos = closestVanillaPos.offset(traceVanilla.side);
            LayerRange layerRange = DataManager.getRenderLayerRange();

            if (layerRange.isPositionWithinRange(pos) &&
                worldSchematic.getBlockState(pos).getMaterial() != Material.AIR &&
                worldClient.getBlockState(pos).getMaterial() == Material.AIR)
            {
                return pos;
            }
        }

        // Traced to schematic blocks, check that the furthest position
        // is next to a vanilla block, ie. in a position where it could be placed normally
        if (furthestTrace != null)
        {
            BlockPos pos = furthestTrace.blockPos;

            if (adjacentOnly)
            {
                BlockPos placementPos = vanillaPosReplaceable ? closestVanillaPos : closestVanillaPos.offset(traceVanilla.side);

                if (pos.equals(placementPos) == false)
                {
                    return null;
                }
            }

            return pos;
        }

        return null;
    }

    public static List<HitResult> rayTraceSchematicWorldBlocksToList(World world, Vec3d start, Vec3d end, int maxSteps)
    {
        if (Double.isNaN(start.x) || Double.isNaN(start.y) || Double.isNaN(start.z) ||
            Double.isNaN(end.x) || Double.isNaN(end.y) || Double.isNaN(end.z))
        {
            return ImmutableList.of();
        }

        RayTraceCalculationData data = new RayTraceCalculationData(start, end, RayTraceFluidHandling.SOURCE_ONLY,
                                                                   malilib.util.game.RayTraceUtils.BLOCK_FILTER_NON_AIR, DataManager.getRenderLayerRange());
        List<HitResult> hits = new ArrayList<>();

        while (--maxSteps >= 0)
        {
            if (malilib.util.game.RayTraceUtils.checkRayCollision(data, world, false))
            {
                hits.add(HitResult.of(data.trace));
            }

            if (malilib.util.game.RayTraceUtils.rayTraceAdvance(data))
            {
                break;
            }
        }

        return hits;
    }

    public static class RayTraceWrapper
    {
        private final HitType type;
        private final BoxCorner corner;
        private final Vec3d hitVec;
        @Nullable
        private final HitResult trace;
        @Nullable
        private final SelectionBox box;
        @Nullable
        private final SchematicPlacement schematicPlacement;
        @Nullable
        private final String placementRegionName;

        public RayTraceWrapper()
        {
            this.type = HitType.MISS;
            this.corner = BoxCorner.NONE;
            this.hitVec = Vec3d.ZERO;
            this.trace = null;
            this.box = null;
            this.schematicPlacement = null;
            this.placementRegionName = null;
        }

        public RayTraceWrapper(HitResult trace)
        {
            this.type = HitType.VANILLA;
            this.corner = BoxCorner.NONE;
            this.hitVec = trace.pos;
            this.trace = trace;
            this.box = null;
            this.schematicPlacement = null;
            this.placementRegionName = null;
        }

        public RayTraceWrapper(HitType type)
        {
            this.type = type;
            this.corner = BoxCorner.NONE;
            this.hitVec = Vec3d.ZERO;
            this.trace = null;
            this.box = null;
            this.schematicPlacement = null;
            this.placementRegionName = null;
        }

        public RayTraceWrapper(HitType type, HitResult trace)
        {
            this.type = type;
            this.corner = BoxCorner.NONE;
            this.hitVec = trace.pos;
            this.trace = trace;
            this.box = null;
            this.schematicPlacement = null;
            this.placementRegionName = null;
        }

        public RayTraceWrapper(SelectionBox box, BoxCorner corner, Vec3d hitVec)
        {
            this.type = corner == BoxCorner.NONE ? HitType.SELECTION_BOX_BODY : HitType.SELECTION_BOX_CORNER;
            this.corner = corner;
            this.hitVec = hitVec;
            this.trace = null;
            this.box = box;
            this.schematicPlacement = null;
            this.placementRegionName = null;
        }

        public RayTraceWrapper(SchematicPlacement placement, Vec3d hitVec, @Nullable String regionName)
        {
            this.type = regionName != null ? HitType.PLACEMENT_SUBREGION : HitType.PLACEMENT_ORIGIN;
            this.corner = BoxCorner.NONE;
            this.hitVec = hitVec;
            this.trace = null;
            this.box = null;
            this.schematicPlacement = placement;
            this.placementRegionName = regionName;
        }

        public HitType getHitType()
        {
            return this.type;
        }

        @Nullable
        public HitResult getRayTraceResult()
        {
            return this.trace;
        }

        @Nullable
        public SelectionBox getHitSelectionBox()
        {
            return this.box;
        }

        @Nullable
        public SchematicPlacement getHitSchematicPlacement()
        {
            return this.schematicPlacement;
        }

        @Nullable
        public String getHitSchematicPlacementRegionName()
        {
            return this.placementRegionName;
        }

        public Vec3d getHitVec()
        {
            return this.hitVec;
        }

        public BoxCorner getHitCorner()
        {
            return this.corner;
        }

        public enum HitType
        {
            MISS,
            VANILLA,
            SELECTION_BOX_BODY,
            SELECTION_BOX_CORNER,
            SELECTION_ORIGIN,
            PLACEMENT_SUBREGION,
            PLACEMENT_ORIGIN,
            SCHEMATIC_BLOCK;
        }
    }
}
