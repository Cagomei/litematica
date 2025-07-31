package litematica.scheduler.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.Chunk.EnumCreateEntityType;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.SimpleBooleanStorage;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.DataTypeUtils;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.position.BlockPos;
import malilib.util.position.BlockPos.MutBlockPos;
import malilib.util.position.ChunkPos;
import malilib.util.position.IntBoundingBox;
import malilib.util.position.Vec3d;
import malilib.util.position.Vec3i;
import malilib.util.world.BlockState;
import litematica.Litematica;
import litematica.render.infohud.InfoHud;
import litematica.scheduler.tasks.TaskProcessChunkBase;
import litematica.schematic.Schematic;
import litematica.schematic.SchematicMetadata;
import litematica.schematic.SchematicRegion;
import litematica.schematic.SchematicType;
import litematica.schematic.container.BlockContainer;
import litematica.schematic.data.EntityData;
import litematica.schematic.data.ScheduledBlockTickData;
import litematica.selection.AreaSelection;
import litematica.selection.SelectionBox;
import litematica.util.PositionUtils;

public class LocalCreateSchematicTask extends TaskProcessChunkBase
{
    protected final AreaSelection area;
    protected final SaveSettings settings;
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
    protected int totalBlockEntities;
    protected long totalBlockTicks;

    public LocalCreateSchematicTask(AreaSelection area,
                                    SaveSettings settings,
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
        this.readBlockData(cPos, regionName, box, minCorner, this.world);

        if (this.settings.saveScheduledBlockTicks.getBooleanValue())
        {
            this.readBlockTickData(regionName, box, minCorner, this.world);
        }

        if (this.settings.saveEntities.getBooleanValue())
        {
            this.readEntityData(regionName, box, selectionBox.getCorner1(), this.world);
        }
    }

    protected void readBlockData(ChunkPos cPos, String regionName, IntBoundingBox box, BlockPos minCorner, World world)
    {
        boolean saveBlocks = this.settings.saveBlocks.getBooleanValue();
        boolean saveBlockEntities = this.settings.saveBlockEntities.getBooleanValue();

        if (saveBlocks == false && saveBlockEntities == false)
        {
            return;
        }

        BlockContainer container = this.blockContainers.computeIfAbsent(regionName, key -> null); // TODO
        Map<BlockPos, CompoundData> blockEntityMap = this.blockEntityMaps.computeIfAbsent(regionName, key -> new HashMap<>());
        Chunk chunk = world.getChunk(cPos.x, cPos.z);
        MutBlockPos mutPos = new MutBlockPos();

        int minCornerX = minCorner.getX();
        int minCornerY = minCorner.getY();
        int minCornerZ = minCorner.getZ();

        for (int y = box.minY; y <= box.maxY; y++)
        {
            for (int z = box.minZ; z <= box.maxZ; z++)
            {
                for (int x = box.minX; x <= box.maxX; x++)
                {
                    IBlockState state = chunk.getBlockState(x, y, z);
                    mutPos.set(x, y, z);

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

                    if (saveBlockEntities && state.getBlock().hasTileEntity())
                    {
                        TileEntity te = chunk.getTileEntity(mutPos, EnumCreateEntityType.CHECK);

                        if (te != null)
                        {
                            try
                            {
                                NBTTagCompound nbt = new NBTTagCompound();
                                te.writeToNBT(nbt);

                                BlockPos relPos = new BlockPos(relX, relY, relZ);
                                CompoundData data = DataTypeUtils.fromVanillaCompound(nbt);
                                DataTypeUtils.putVec3i(data, relPos);

                                blockEntityMap.put(relPos, data);
                                this.totalBlockEntities++;
                            }
                            catch (Exception e)
                            {
                                Litematica.LOGGER.warn("Failed to save BlockEntity {} at {}", te, mutPos);
                            }
                        }
                    }
                }
            }
        }
    }

    protected boolean shouldSaveBlock(IBlockState state, MutBlockPos mutPos)
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

    protected void readBlockTickData(String regionName, IntBoundingBox box, BlockPos minCorner, World world)
    {
        Map<BlockPos, ScheduledBlockTickData> blockTickMap = this.blockTickMaps.computeIfAbsent(regionName, key -> new HashMap<>());

        if (world instanceof WorldServer)
        {
            // The vanilla method checks for "x < maxX" etc.
            IntBoundingBox expandedBox = IntBoundingBox.createProper(
                    box.minX,     box.minY,     box.minZ,
                    box.maxX + 1, box.maxY + 1, box.maxZ + 1);
            List<NextTickListEntry> pendingTicks = world.getPendingBlockUpdates(expandedBox.toVanillaBox(), false);

            if (pendingTicks != null)
            {
                final int listSize = pendingTicks.size();
                final long currentTime = world.getTotalWorldTime();

                // The getPendingBlockUpdates() method doesn't check the y-coordinate... :-<
                for (int i = 0; i < listSize; ++i)
                {
                    NextTickListEntry entry = pendingTicks.get(i);

                    if (entry.position.getY() >= box.minY && entry.position.getY() <= box.maxY)
                    {
                        BlockPos relPos = new BlockPos(entry.position.getX() - minCorner.getX(),
                                                       entry.position.getY() - minCorner.getY(),
                                                       entry.position.getZ() - minCorner.getZ());

                        // Store the delay, i.e. relative time
                        long delay = entry.scheduledTime - currentTime;
                        long sortOrder = 0; // TODO

                        ScheduledBlockTickData tickData = new ScheduledBlockTickData(relPos,
                                                                                     entry.getBlock(),
                                                                                     entry.priority,
                                                                                     delay,
                                                                                     sortOrder);

                        blockTickMap.put(relPos, tickData);
                        this.totalBlockTicks++;
                    }
                }
            }
        }
    }

    protected void readEntityData(String regionName, IntBoundingBox box, BlockPos regionPosAbs, World world)
    {
        List<EntityData> entityList = this.entityLists.computeIfAbsent(regionName, key -> new ArrayList<>());
        AxisAlignedBB bb = PositionUtils.createAABBFrom(box);
        List<Entity> entities = world.getEntitiesInAABBexcluding(null, bb, e -> (e instanceof EntityPlayer) == false);
        int regionOriginX = regionPosAbs.getX();
        int regionOriginY = regionPosAbs.getY();
        int regionOriginZ = regionPosAbs.getZ();

        for (Entity entity : entities)
        {
            UUID uuid = entity.getUniqueID();

            // This entity was already saved to some region
            if (this.existingEntities.contains(uuid))
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
                NBTTagCompound tag = new NBTTagCompound();

                if (entity.writeToNBTOptional(tag))
                {
                    Vec3d relPos = new Vec3d(x - regionOriginX, y - regionOriginY, z - regionOriginZ);

                    CompoundData data = DataTypeUtils.fromVanillaCompound(tag);
                    DataTypeUtils.writeVec3dToListTag(data, relPos);

                    entityList.add(new EntityData(relPos, data));
                    this.existingEntities.add(uuid);
                    this.totalEntities++;
                }
            }
            catch (Exception e)
            {
                Litematica.LOGGER.warn("Failed to save entity {} at {}", entity, entity.getPositionVector());
            }
        }
        
    }

    protected void onDataCollected()
    {
        ImmutableMap.Builder<String, SchematicRegion> regionBuilder = ImmutableMap.builder();

        for (SelectionBox box : this.subRegions.values())
        {
            String regionName = box.getName();
            BlockContainer container = this.blockContainers.getOrDefault(regionName, null); // TODO
            Map<BlockPos, CompoundData> blockEntityMap = this.blockEntityMaps.getOrDefault(regionName, new HashMap<>());
            Map<BlockPos, ScheduledBlockTickData> blockTickMap = this.blockTickMaps.getOrDefault(regionName, new HashMap<>());
            List<EntityData> entityList = this.entityLists.getOrDefault(regionName, new ArrayList<>());

            BlockPos relPos = box.getCorner1().subtract(this.origin);
            Vec3i size = box.getSize();

            SchematicRegion region = new SchematicRegion(relPos, size, container, blockEntityMap, blockTickMap, entityList);
            regionBuilder.put(regionName, region);
        }

        ImmutableMap<String, SchematicRegion> regionMap = regionBuilder.build();
        Schematic schematic = this.settings.schematicType.createSchematic();
        SchematicMetadata meta = schematic.getMetadata();

        meta.setTotalVolume(PositionUtils.getTotalVolume(this.subRegions.values()));
        meta.setEnclosingSize(PositionUtils.getEnclosingAreaSize(this.subRegions.values()));
        meta.setTotalBlocks(this.totalBlocks);
        meta.setEntityCount(this.totalEntities);
        meta.setBlockEntityCount(this.totalBlockEntities);
        meta.setBlockTickCount(this.totalBlockTicks);

        meta.setAuthor(GameWrap.getPlayerName());
        meta.setSchematicName(this.area.getName());
        meta.setOriginalOrigin(this.origin);

        long time = System.currentTimeMillis();
        meta.setTimeCreated(time);
        meta.setTimeModified(time);

        this.schematicListener.accept(schematic);
    }

    @Override
    protected void onStop()
    {
        if (this.finished == false)
        {
            MessageDispatcher.warning().translate("litematica.message.error.schematic_save_interrupted");
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        this.notifyListener();
    }

    public static class SaveSettings
    {
        public final SimpleBooleanStorage saveBlocks              = new SimpleBooleanStorage(true);
        public final SimpleBooleanStorage saveBlockEntities       = new SimpleBooleanStorage(true);
        public final SimpleBooleanStorage saveEntities            = new SimpleBooleanStorage(true);
        public final SimpleBooleanStorage saveScheduledBlockTicks = new SimpleBooleanStorage(true);
        public final SimpleBooleanStorage saveFromClientWorld     = new SimpleBooleanStorage(true);
        public final SimpleBooleanStorage saveFromSchematicWorld  = new SimpleBooleanStorage(false);
        public final SimpleBooleanStorage clientWorldFirst        = new SimpleBooleanStorage(true);
        public final SimpleBooleanStorage exposedBlocksOnly       = new SimpleBooleanStorage(false);
        public final Set<Block>           ignoreBlocks            = new HashSet<>();
        public final Set<IBlockState>     ignoreBlockStates       = new HashSet<>();
        public SchematicType schematicType = SchematicType.LITEMATICA;
    }
}
