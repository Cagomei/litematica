package litematica.schematic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.google.common.collect.ImmutableMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.Constants;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.DataView;
import malilib.util.data.tag.ListData;
import malilib.util.data.tag.util.DataTypeUtils;
import malilib.util.game.MinecraftVersion;
import malilib.util.game.wrap.RegistryUtils;
import malilib.util.position.BlockPos;
import malilib.util.position.Vec3d;
import malilib.util.position.Vec3i;
import litematica.schematic.container.ArrayBlockContainer;
import litematica.schematic.container.BlockContainer;
import litematica.schematic.data.EntityData;
import litematica.schematic.data.ScheduledBlockTickData;
import litematica.util.PositionUtils;

public class LitematicaSchematic extends BaseSchematic
{
    public static final String FILE_NAME_EXTENSION = "litematic";
    public static final int CURRENT_SCHEMATIC_VERSION = 4;

    public LitematicaSchematic()
    {
        super(SchematicType.LITEMATICA);
    }

    @Override
    public boolean read(DataView data)
    {
        if (isValidData(data) == false)
        {
            return false;
        }

        int version = data.getIntOrDefault("Version", -1);

        /* This can't happen after the isValid() check
        if (version == -1)
        {
            MessageDispatcher.error("litematica.error.schematic_read.no_version");
        }
        */

        if (version == 0 || version > CURRENT_SCHEMATIC_VERSION)
        {
            MessageDispatcher.warning("litematica.error.schematic_read.unknown_schematic_version", version);
        }

        this.regions = this.readRegions(data, version);
        this.minecraftDataVersion = data.getIntOrDefault("MinecraftDataVersion", -1);
        this.metadata = createAndReadMetadata(data).orElse(new SchematicMetadata());
        this.enclosingSize = this.metadata.getEnclosingSize();

        return true;
    }

    @Override
    public CompoundData write()
    {
        CompoundData data = new CompoundData();

        data.putInt("Version", CURRENT_SCHEMATIC_VERSION);
        data.putInt("MinecraftDataVersion", this.minecraftDataVersion);
        data.put("Metadata", this.metadata.write(new CompoundData()));
        data.put("Regions", this.writeRegions());

        return data;
    }

    protected ImmutableMap<String, SchematicRegion> readRegions(DataView data, int version)
    {
        ImmutableMap.Builder<String, SchematicRegion> builder = ImmutableMap.builder();

        DataView regionsTag = data.getCompound("Regions");

        for (String regionName : regionsTag.getKeys())
        {
            if (regionsTag.contains(regionName, Constants.NBT.TAG_COMPOUND) == false)
            {
                MessageDispatcher.error("litematica.error.schematic_read.litematica.invalid_tag_in_regions", regionName);
                continue;
            }

            DataView regionTag = regionsTag.getCompound(regionName);
            BlockPos regionPos = DataTypeUtils.readBlockPos(regionTag.getCompound("Position"));
            BlockPos regionSize = DataTypeUtils.readBlockPos(regionTag.getCompound("Size"));

            if (regionPos == null || regionSize == null)
            {
                MessageDispatcher.error("litematica.error.schematic_read.litematica.missing_pos_or_size", regionName);
                continue;
            }

            ListData beList = regionTag.getList("TileEntities", Constants.NBT.TAG_COMPOUND);
            ListData entityListData = regionTag.getList("Entities", Constants.NBT.TAG_COMPOUND);
            Map<BlockPos, CompoundData> blockEntityMap = null;
            Map<BlockPos, ScheduledBlockTickData> blockTickMap = null;
            List<EntityData> entityList = null;

            if (version >= 2)
            {
                blockEntityMap = this.readBlockEntities_v2(beList);
                entityList = this.readEntities_v2(entityListData);
            }
            else if (version == 1)
            {
                blockEntityMap = this.readBlockEntities_v1(beList);
                entityList = this.readEntities_v1(entityListData);
            }
            else
            {
                MessageDispatcher.error("litematica.error.schematic_read.litematica.version_0", regionName);
            }

            if (version >= 3)
            {
                ListData tickData = regionTag.getList("PendingBlockTicks", Constants.NBT.TAG_COMPOUND);
                blockTickMap = this.readBlockTicks_v3(tickData);
            }

            long[] blockDataArray = regionTag.getLongArray("BlockStates");

            if (blockDataArray == null || blockDataArray.length == 0)
            {
                MessageDispatcher.error("litematica.error.schematic_read.litematica.invalid_block_data_array", regionName);
                continue;
            }

            ListData paletteTag = regionTag.getList("BlockStatePalette", Constants.NBT.TAG_COMPOUND);
            int paletteSize = paletteTag.size();
            Vec3i size = PositionUtils.getAbsoluteSize(regionSize);
            ArrayBlockContainer container = ArrayBlockContainer.createContainer(paletteSize, blockDataArray, size);

            if (container == null)
            {
                MessageDispatcher.error("litematica.error.schematic_read.litematica.region_container", regionName);
                continue;
            }

            if (readPaletteFromLitematicaFormatTag(paletteTag, container.getPalette()) == false)
            {
                MessageDispatcher.error("litematica.error.schematic_read.litematica.palette_read_failed", regionName);
                continue;
            }

            if (blockEntityMap == null)
            {
                blockEntityMap = new HashMap<>();
            }

            if (blockTickMap == null)
            {
                blockTickMap = new HashMap<>();
            }

            if (entityList == null)
            {
                entityList = new ArrayList<>();
            }

            SchematicRegion region = new SchematicRegion(regionPos, regionSize, container, blockEntityMap, blockTickMap, entityList);
            builder.put(regionName, region);
        }

        return builder.build();
    }

    protected Map<BlockPos, CompoundData> readBlockEntities_v2(ListData listData)
    {
        Map<BlockPos, CompoundData> map = new HashMap<>();
        final int size = listData.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundData beData = listData.getCompoundAt(i).copy();
            BlockPos pos = DataTypeUtils.readBlockPos(beData);
            DataTypeUtils.removeBlockPosFromTag(beData);

            if (pos != null && beData.isEmpty() == false)
            {
                map.put(pos, beData);
            }
        }

        return map;
    }

    protected Map<BlockPos, CompoundData> readBlockEntities_v1(ListData list)
    {
        Map<BlockPos, CompoundData> tileMap = new HashMap<>();
        final int size = list.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundData wrapperData = list.getCompoundAt(i);
            CompoundData beData = wrapperData.getCompound("TileNBT").copy();

            // Note: This within-schematic relative position is not inside the tile tag!
            BlockPos pos = DataTypeUtils.readBlockPos(wrapperData);

            if (pos != null && beData.isEmpty() == false)
            {
                tileMap.put(pos, beData);
            }
        }

        return tileMap;
    }

    protected Map<BlockPos, ScheduledBlockTickData> readBlockTicks_v3(ListData list)
    {
        Map<BlockPos, ScheduledBlockTickData> tickMap = new HashMap<>();
        final int size = list.size();
        int tickIdCounter = 0;

        for (int i = 0; i < size; ++i)
        {
            CompoundData tag = list.getCompoundAt(i);

            if (tag.contains("Block", Constants.NBT.TAG_STRING) &&
                tag.contains("Time", Constants.NBT.TAG_ANY_NUMERIC)) // XXX these were accidentally saved as longs in version 3
            {
                Block block = RegistryUtils.getBlockByIdStr(tag.getString("Block"));

                if (block != null && block != Blocks.AIR)
                {
                    BlockPos pos = DataTypeUtils.readBlockPos(tag);
                    int priority = tag.getInt("Priority");
                    long delay = tag.getInt("Time");
                    int tickId = tag.getIntOrDefault("TickId", tickIdCounter++);
                    // Note: the time is a relative delay at this point
                    ScheduledBlockTickData entry = new ScheduledBlockTickData(pos, block, priority, delay, tickId);

                    tickMap.put(pos, entry);
                }
            }
        }

        return tickMap;
    }

    protected List<EntityData> readEntities_v2(ListData listData)
    {
        final int size = listData.size();
        List<EntityData> entityList = new ArrayList<>(size);

        for (int i = 0; i < size; ++i)
        {
            CompoundData entityData = listData.getCompoundAt(i).copy();
            Vec3d pos = DataTypeUtils.readVec3dFromListTag(entityData);

            if (pos != null && entityData.isEmpty() == false)
            {
                entityList.add(new EntityData(pos, entityData));
            }
        }

        return entityList;
    }

    protected List<EntityData> readEntities_v1(ListData list)
    {
        List<EntityData> entityList = new ArrayList<>();
        final int size = list.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundData wrapperData = list.getCompoundAt(i);
            Vec3d pos = DataTypeUtils.readVec3d(wrapperData);
            CompoundData entityData = wrapperData.getCompound("EntityData").copy();

            if (pos != null && entityData.isEmpty() == false)
            {
                // Update the correct position to the Entity NBT, where it is stored in version 2 - not needed in memory yet
                //DataTypeUtils.writeVec3dToListTag(entityData, pos);
                entityList.add(new EntityData(pos, entityData));
            }
        }

        return entityList;
    }

    protected CompoundData writeRegions()
    {
        CompoundData data = new CompoundData();
        return data;
    }

    public static boolean isValidData(DataView data)
    {
        return data.contains("Metadata", Constants.NBT.TAG_COMPOUND) &&
               data.contains("Regions", Constants.NBT.TAG_COMPOUND) &&
               data.contains("Version", Constants.NBT.TAG_INT);
    }

    public static BlockContainer createDefaultBlockContainer(Vec3i containerSize)
    {
        return new ArrayBlockContainer(containerSize);
    }

    public static Optional<SchematicMetadata> createAndReadMetadata(DataView data)
    {
        if (isValidData(data) == false)
        {
            return Optional.empty();
        }

        SchematicMetadata metadata = new SchematicMetadata();
        metadata.read(data.getCompound("Metadata"));

        if (metadata.getMinecraftVersion().equals(MinecraftVersion.MC_UNKNOWN))
        {
            metadata.setMinecraftVersion(MinecraftVersion.getOrCreateVersionFromDataVersion(data.getInt("MinecraftDataVersion")));
        }

        if (metadata.getSchematicVersion() <= 0)
        {
            metadata.setSchematicVersion(data.getInt("Version"));
        }

        if (metadata.getRegionCount() <= 0)
        {
            metadata.setRegionCount(data.getCompound("Regions").size());
        }

        if (metadata.getEntityCount() < 0 || metadata.getBlockEntityCount() < 0)
        {
            DataView regionsTag = data.getCompound("Regions");
            int entityCount = 0;
            long blockEntityCount = 0;

            for (String key : regionsTag.getKeys())
            {
                CompoundData tag = regionsTag.getCompound(key);
                entityCount += tag.getList("Entities", Constants.NBT.TAG_COMPOUND).size();
                blockEntityCount += tag.getList("TileEntities", Constants.NBT.TAG_COMPOUND).size();
            }

            metadata.setEntityCount(entityCount);
            metadata.setBlockEntityCount(blockEntityCount);
        }

        return Optional.of(metadata);
    }

    public static Optional<Schematic> fromData(DataView data)
    {
        LitematicaSchematic schematic = new LitematicaSchematic();

        if (schematic.read(data))
        {
            return Optional.of(schematic);
        }

        return Optional.empty();
    }

    public static Optional<Schematic> fromRegions(ImmutableMap<String, SchematicRegion> regions)
    {
        if (regions.size() < 1)
        {
            return Optional.empty();
        }

        LitematicaSchematic schematic = new LitematicaSchematic();

        schematic.regions = regions;
        schematic.enclosingSize = PositionUtils.getEnclosingAreaSize(regions.values());
        schematic.minecraftDataVersion = CURRENT_MINECRAFT_DATA_VERSION;
        schematic.metadata.setSchematicVersion(CURRENT_SCHEMATIC_VERSION);

        return Optional.of(schematic);
    }
}
