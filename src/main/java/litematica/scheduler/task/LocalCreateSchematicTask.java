package litematica.scheduler.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.Chunk.EnumCreateEntityType;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.util.DataTypeUtils;
import malilib.util.game.MinecraftVersion;
import malilib.util.game.wrap.BlockWrap;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.game.wrap.RegistryUtils;
import malilib.util.position.BlockPos;
import malilib.util.position.BlockPos.MutBlockPos;
import malilib.util.position.ChunkPos;
import malilib.util.position.IntBoundingBox;
import malilib.util.position.Vec3d;
import malilib.util.position.Vec3i;
import malilib.util.world.BlockState;
import malilib.util.world.ScheduledBlockTickData;
import litematica.Litematica;
import litematica.mixin.access.NextTickListEntryMixin;
import litematica.render.infohud.InfoHud;
import litematica.scheduler.tasks.TaskProcessChunkBase;
import litematica.schematic.BaseSchematic;
import litematica.schematic.Schematic;
import litematica.schematic.SchematicMetadata;
import litematica.schematic.SchematicRegion;
import litematica.schematic.SchematicSaveSettings;
import litematica.schematic.container.BlockContainer;
import litematica.schematic.data.EntityData;
import litematica.selection.AreaSelection;
import litematica.selection.SelectionBox;
import litematica.util.PositionUtils;

public class LocalCreateSchematicTask extends TaskProcessChunkBase
{
    protected final AreaSelection area;
    protected final SchematicSaveSettings settings;
    protected final BlockPos origin;
    protected final ImmutableMap<String, SelectionBox> subRegions;
    protected final ArrayListMultimap<ChunkPos, SelectionBox> selectionBoxesPerChunk;
    protected final Map<String, BlockContainer> blockContainers = new HashMap<>();
    protected final Map<String, Map<BlockPos, CompoundData>> blockEntityMaps = new HashMap<>();
    protected final Map<String, Map<BlockPos, ScheduledBlockTickData>> blockTickMaps = new HashMap<>();
    protected final Map<String, List<EntityData>> entityLists = new HashMap<>();
    protected final Set<UUID> existingEntities = new HashSet<>();
    protected final Consumer<Schematic> schematicListener;
    protected long totalBlocks;
    protected int totalEntities;
    protected long totalBlockEntities;
    protected long totalBlockTicks;

    public LocalCreateSchematicTask(AreaSelection area,
                                    SchematicSaveSettings settings,
                                    Consumer<Schematic> schematicListener)
    {
        super("litematica.hud.task_name.local_create_schematic");

        Collection<SelectionBox> allBoxes = area.getAllSelectionBoxes();

        this.area = area.copy();
        this.settings = settings;
        this.schematicListener = schematicListener;
        this.origin = area.getEffectiveOrigin();
        this.subRegions = area.getAllSelectionBoxesMap();
        this.selectionBoxesPerChunk = PositionUtils.getPerChunkBoxes(allBoxes);

        this.setCompletionListener(this::onDataCollected);
        this.addPerChunkBoxes(allBoxes);
    }

    @Override
    protected boolean canProcessChunk(ChunkPos pos)
    {
        return this.areSurroundingChunksLoaded(pos, this.worldClient, 1);
    }

    @Override
    protected boolean processChunk(ChunkPos pos)
    {
        for (SelectionBox box : this.selectionBoxesPerChunk.get(pos))
        {
            this.readAllFromBox(pos, box);
        }

        return true;
    }

    protected void readAllFromBox(ChunkPos cPos, SelectionBox selectionBox)
    {
        IntBoundingBox box = PositionUtils.getBoundsWithinChunkForBox(selectionBox, cPos.x, cPos.z);

        if (box == null)
        {
            return;
        }

        String regionName = selectionBox.getName();
        BlockPos minCorner = malilib.util.position.PositionUtils.getMinCorner(selectionBox.getCorner1(), selectionBox.getCorner2());
        BlockContainer container = this.blockContainers.computeIfAbsent(regionName, key -> this.createBlockContainer(selectionBox));
        Map<BlockPos, CompoundData> blockEntityMap = this.blockEntityMaps.computeIfAbsent(regionName, key -> new HashMap<>());

        this.readBlockData(cPos, container, blockEntityMap, box, minCorner, this.settings, this.world);

        if (this.settings.saveScheduledBlockTicks.getBooleanValue())
        {
            Map<BlockPos, ScheduledBlockTickData> blockTickMap = this.blockTickMaps.computeIfAbsent(regionName, key -> new HashMap<>());
            this.totalBlockTicks += this.readBlockTickData(blockTickMap, box, minCorner, this.world, this.world.getTotalWorldTime());
        }

        if (this.settings.saveEntities.getBooleanValue())
        {
            List<EntityData> entityList = this.entityLists.computeIfAbsent(regionName, key -> new ArrayList<>());
            this.totalEntities += this.readEntityData(entityList, this.existingEntities, box, selectionBox.getCorner1(), this.world);
        }
    }

    protected BlockContainer createBlockContainer(SelectionBox selectionBox)
    {
        Vec3i containerSize = PositionUtils.getAbsoluteAreaSize(selectionBox);
        return this.createBlockContainer(containerSize);
    }

    protected BlockContainer createBlockContainer(Vec3i containerSize)
    {
        return this.settings.schematicType.createContainer(containerSize);
    }

    protected void readBlockData(ChunkPos cPos,
                                 BlockContainer container,
                                 Map<BlockPos, CompoundData> blockEntityMapOut,
                                 IntBoundingBox box,
                                 BlockPos minCorner,
                                 SchematicSaveSettings settings,
                                 World world)
    {
        boolean saveBlocks = settings.saveBlocks.getBooleanValue();
        boolean saveBlockEntities = settings.saveBlockEntities.getBooleanValue();

        if (saveBlocks == false && saveBlockEntities == false)
        {
            return;
        }

        Chunk chunk = world.getChunk(cPos.x, cPos.z);
        MutBlockPos mutPos = new MutBlockPos();

        int minCornerX = minCorner.getX();
        int minCornerY = minCorner.getY();
        int minCornerZ = minCorner.getZ();
        int errorCount = 0;

        for (int y = box.minY; y <= box.maxY; y++)
        {
            for (int z = box.minZ; z <= box.maxZ; z++)
            {
                for (int x = box.minX; x <= box.maxX; x++)
                {
                    mutPos.set(x, y, z);
                    IBlockState state = chunk.getBlockState(x, y, z).getActualState(world, mutPos);

                    if (this.shouldSaveBlock(state, mutPos) == false)
                    {
                        continue;
                    }

                    int relX = x - minCornerX;
                    int relY = y - minCornerY;
                    int relZ = z - minCornerZ;

                    if (saveBlocks)
                    {
                        container.setBlockState(relX, relY, relZ, BlockState.of(state));
                        this.totalBlocks++;
                    }

                    if (saveBlockEntities == false || state.getBlock().hasTileEntity() == false)
                    {
                        continue;
                    }

                    TileEntity te = chunk.getTileEntity(mutPos, EnumCreateEntityType.CHECK);

                    if (te == null)
                    {
                        continue;
                    }

                    CompoundData data = BlockWrap.writeBlockEntityToTag(te);

                    if (data != null)
                    {
                        BlockPos relPos = new BlockPos(relX, relY, relZ);
                        DataTypeUtils.putVec3i(data, relPos);

                        blockEntityMapOut.put(relPos, data);
                        this.totalBlockEntities++;
                    }
                    else
                    {
                        Litematica.LOGGER.warn("Failed to save BlockEntity {} at {}", te, mutPos);
                        errorCount++;
                    }
                }
            }
        }

        if (errorCount > 0)
        {
            MessageDispatcher.warning("litematica.message.warn.schematic_read.failed_to_read_block_entities",
                                      errorCount, blockEntityMapOut.size());
        }
    }

    protected boolean shouldSaveBlock(BlockState state, MutBlockPos mutPos)
    {
        if (state.getBlock() == Blocks.AIR)
        {
            return false;
        }

        if (this.settings.ignoreBlockStates.contains(state) ||
            this.settings.ignoreBlocks.contains(state.getBlock()))
        {
            return false;
        }

        if (this.settings.exposedBlocksOnly.getBooleanValue())
        {
            return true;    // TODO
        }

        return true;
    }

    protected long readBlockTickData(Map<BlockPos, ScheduledBlockTickData> blockTickMap,
                                     IntBoundingBox box,
                                     BlockPos regionMinCorner,
                                     World world,
                                     long currentWorldTick)
    {
        long blockTickCount = 0;

        if ((world instanceof WorldServer) == false)
        {
            return 0;
        }

        // The vanilla method checks for "x < maxX" etc.
        IntBoundingBox expandedBox = IntBoundingBox.createProper(
                box.minX,     box.minY,     box.minZ,
                box.maxX + 1, box.maxY + 1, box.maxZ + 1);
        List<NextTickListEntry> pendingTicks = world.getPendingBlockUpdates(expandedBox.toVanillaBox(), false);

        if (pendingTicks == null)
        {
            return 0;
        }

        // The getPendingBlockUpdates() method doesn't check the y-coordinate... :-<
        for (NextTickListEntry entry : pendingTicks)
        {
            if (entry.position.getY() < box.minY || entry.position.getY() > box.maxY)
            {
                continue;
            }

            BlockPos relPos = new BlockPos(entry.position.getX() - regionMinCorner.getX(),
                                           entry.position.getY() - regionMinCorner.getY(),
                                           entry.position.getZ() - regionMinCorner.getZ());

            // Store the delay, i.e. relative time
            long delay = entry.scheduledTime - currentWorldTick;
            long tickId = ((NextTickListEntryMixin) entry).litematica$getTickId();

            ScheduledBlockTickData tickData = new ScheduledBlockTickData(relPos,
                                                                         RegistryUtils.getBlockIdStr(entry.getBlock()),
                                                                         entry.priority,
                                                                         delay,
                                                                         tickId);

            blockTickMap.put(relPos, tickData);
            blockTickCount++;
        }

        return blockTickCount;
    }

    protected int readEntityData(List<EntityData> entityListOut,
                                 Set<UUID> existingEntities,
                                 IntBoundingBox box,
                                 BlockPos regionPosAbs,
                                 World world)
    {
        AxisAlignedBB bb = PositionUtils.createAABBFrom(box);
        List<Entity> entities = world.getEntitiesInAABBexcluding(null, bb, e -> (e instanceof EntityPlayer) == false);
        int regionOriginX = regionPosAbs.getX();
        int regionOriginY = regionPosAbs.getY();
        int regionOriginZ = regionPosAbs.getZ();
        int entityCount = 0;
        int errorCount = 0;

        for (Entity entity : entities)
        {
            UUID uuid = EntityWrap.getUuid(entity);

            // This entity was already saved to some region
            if (existingEntities.contains(uuid))
            {
                continue;
            }

            double x = EntityWrap.getX(entity);
            double y = EntityWrap.getY(entity);
            double z = EntityWrap.getZ(entity);

            // Only take entities whose origin is within the region
            if (x < bb.minX || x > bb.maxX ||
                y < bb.minY || y > bb.maxY ||
                z < bb.minZ || z > bb.maxZ)
            {
                continue;
            }

            try
            {
                CompoundData data = EntityWrap.writeEntityToTag(entity);

                if (data != null)
                {
                    Vec3d relPos = new Vec3d(x - regionOriginX, y - regionOriginY, z - regionOriginZ);
                    DataTypeUtils.writeVec3dToListTag(data, relPos);

                    entityListOut.add(new EntityData(relPos, data));
                    existingEntities.add(uuid);
                    entityCount++;
                }
            }
            catch (Exception e)
            {
                Litematica.LOGGER.warn("Failed to save entity {} at {}", entity, entity.getPositionVector());
                errorCount++;
            }
        }

        if (errorCount > 0)
        {
            MessageDispatcher.warning("litematica.message.warn.schematic_read.failed_to_read_entities",
                                      errorCount, entityListOut.size());
        }

        return entityCount;
    }

    protected void onDataCollected()
    {
        ImmutableMap<String, SchematicRegion> regions = this.buildSchematicRegions();
        Optional<Schematic> schematicOpt = this.settings.schematicType.createSchematicFromRegions(regions);

        if (schematicOpt.isPresent())
        {
            Schematic schematic = schematicOpt.get();
            this.setMetadataValues(schematic.getMetadata(), regions.size());
            this.schematicListener.accept(schematic);
        }
        else
        {
            MessageDispatcher.error(8000).translate("litematica.message.error.save_schematic.failed_to_create_schematic");
        }
    }

    protected ImmutableMap<String, SchematicRegion> buildSchematicRegions()
    {
        ImmutableMap.Builder<String, SchematicRegion> regionBuilder = ImmutableMap.builder();

        for (SelectionBox box : this.subRegions.values())
        {
            String regionName = box.getName();
            BlockContainer container = this.blockContainers.getOrDefault(regionName, this.createBlockContainer(Vec3i.ZERO));
            Map<BlockPos, CompoundData> blockEntityMap = this.blockEntityMaps.getOrDefault(regionName, new HashMap<>());
            Map<BlockPos, ScheduledBlockTickData> blockTickMap = this.blockTickMaps.getOrDefault(regionName, new HashMap<>());
            List<EntityData> entityList = this.entityLists.getOrDefault(regionName, new ArrayList<>());

            BlockPos relPos = box.getCorner1().subtract(this.origin);
            Vec3i size = box.getSize();
            int dv = BaseSchematic.CURRENT_MINECRAFT_DATA_VERSION;

            SchematicRegion region = new SchematicRegion(relPos, size, container, blockEntityMap, blockTickMap, entityList, dv);
            regionBuilder.put(regionName, region);
        }

        return regionBuilder.build();
    }

    protected void setMetadataValues(SchematicMetadata meta, int regionCount)
    {
        Collection<SelectionBox> boxes = this.subRegions.values();
        meta.setRegionCount(regionCount);
        meta.setEnclosingSize(PositionUtils.getEnclosingAreaSizeOfBoxes(boxes));
        meta.setTotalVolume(PositionUtils.getTotalVolume(boxes));
        meta.setTotalBlocks(this.totalBlocks);
        meta.setEntityCount(this.totalEntities);
        meta.setBlockEntityCount(this.totalBlockEntities);
        meta.setBlockTickCount(this.totalBlockTicks);

        meta.setAuthor(GameWrap.getPlayerName());
        meta.setSchematicName(this.area.getName());
        meta.setOriginalOrigin(this.origin);

        long time = System.currentTimeMillis();
        meta.setTimeCreated(time);
        meta.setMinecraftVersion(MinecraftVersion.CURRENT_VERSION);
    }

    @Override
    protected void onStop()
    {
        if (this.finished == false)
        {
            MessageDispatcher.warning().translate("litematica.message.error.schematic_save.interrupted");
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        this.notifyListener();
    }
}
