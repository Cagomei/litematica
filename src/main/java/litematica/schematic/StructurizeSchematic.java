package litematica.schematic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.ListUtils;
import malilib.util.data.Constants;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.DataView;
import malilib.util.data.tag.ListData;
import malilib.util.data.tag.util.DataTypeUtils;
import malilib.util.game.MinecraftVersion;
import malilib.util.position.BlockPos;
import malilib.util.position.Vec3i;
import litematica.schematic.container.ArrayBlockContainer;
import litematica.schematic.container.BlockContainer;
import litematica.schematic.data.EntityData;
import litematica.util.PositionUtils;

public class StructurizeSchematic extends BaseSchematic
{
    // Reference: https://github.com/ldtteam/Structurize/blob/68a5ba820003b1eef1f6f8859e1c926207fbb573/src/main/java/com/ldtteam/structurize/blueprints/v1/BlueprintUtil.java#L643-L676

    public static final int CURRENT_SCHEMATIC_VERSION = 1;
    public static final String FILE_NAME_EXTENSION = "blueprint";

    protected CompoundData originalMetadataTag = new CompoundData();
    protected CompoundData optionalDataTag = new CompoundData();
    protected ListData requiredModsTag = new ListData(Constants.NBT.TAG_STRING);

    public StructurizeSchematic()
    {
        super(SchematicType.STRUCTURIZE);
    }

    @Override
    public boolean read(DataView data)
    {
        if (isValidData(data) == false)
        {
            return false;
        }

        int version = getStructurizeVersion(data);
        this.minecraftDataVersion = data.getIntOrDefault("mcversion", -1);

        /* This can't happen after the isValid() check
        if (version == -1)
        {
            MessageDispatcher.error("litematica.error.schematic_read.no_version");
        }
        */

        if (version == 0 || version > CURRENT_SCHEMATIC_VERSION)
        {
            MessageDispatcher.warning("litematica.error.schematic_read.unknown_schematic_version",
                                      version, CURRENT_SCHEMATIC_VERSION);
        }

        if (this.minecraftDataVersion > CURRENT_MINECRAFT_DATA_VERSION)
        {
            MessageDispatcher.warning("litematica.error.schematic_read.future_data_version",
                                      this.minecraftDataVersion, CURRENT_MINECRAFT_DATA_VERSION);
        }

        return this.readFromTag_v1(data, version);
    }

    @Override
    public Optional<CompoundData> write()
    {
        int regionCount = this.regions.size();

        if (regionCount != 1)
        {
            MessageDispatcher.error("litematica.message.error.schematic_save.wrong_region_count", regionCount, 1);
            return Optional.empty();
        }

        int version = CURRENT_SCHEMATIC_VERSION;
        this.metadata.setSchematicVersion(version);

        SchematicRegion region = ListUtils.getFirstEntry(this.regions.values());
        ArrayBlockContainer container = (ArrayBlockContainer) region.getBlockContainer();
        CompoundData data = new CompoundData();

        if (version == 1 && this.write_v1(data, region, container, version))
        {
            return Optional.of(data);
        }

        return Optional.empty();
    }

    public static boolean isValidData(DataView data)
    {
        return isValidData_v1(data);
    }

    public static boolean isValidData_v1(DataView data)
    {
        // Version 1 or 2
        if (data.contains("size_x", Constants.NBT.TAG_SHORT) &&
            data.contains("size_y", Constants.NBT.TAG_SHORT) &&
            data.contains("size_z", Constants.NBT.TAG_SHORT) &&
            data.contains("version", Constants.NBT.TAG_BYTE) &&
            data.containsList("palette", Constants.NBT.TAG_COMPOUND) &&
            data.contains("blocks", Constants.NBT.TAG_INT_ARRAY))
        {
            int version = data.getByte("version");
            return version == 1 && isSizeValid(readSizeFromTag(data));
        }

        return false;
    }

    public static int getStructurizeVersion(DataView data)
    {
        if (isValidData_v1(data))
        {
            return data.getByte("version");
        }

        return -1;
    }

    protected static Vec3i readSizeFromTag(DataView tag)
    {
        return new Vec3i(tag.getShort("size_x"),
                         tag.getShort("size_y"),
                         tag.getShort("size_z"));
    }

    public static SchematicMetadata createAndReadMetadata(DataView data, Vec3i size, int version, int dataVersion)
    {
        SchematicMetadata metadata = new SchematicMetadata();

        if (data.contains("Metadata", Constants.NBT.TAG_COMPOUND))
        {
            metadata.read(data.getCompound("Metadata"));
        }

        metadata.setSchematicName(data.getStringOrDefault("name", metadata.getSchematicName()));
        metadata.setSchematicVersion(version);
        metadata.setEnclosingSize(size);
        metadata.setTotalVolume((long) size.getX() * size.getY() * size.getZ());
        metadata.setMinecraftVersion(MinecraftVersion.getOrCreateVersionFromDataVersion(dataVersion));

        if (data.contains("optional_data", Constants.NBT.TAG_COMPOUND))
        {
            CompoundData optionalDataTag = data.getCompound("optional_data");

            if (optionalDataTag.contains("structurize", Constants.NBT.TAG_COMPOUND))
            {
                CompoundData structurizeData = optionalDataTag.getCompound("structurize");

                if (structurizeData.contains("primary_offset", Constants.NBT.TAG_COMPOUND))
                {
                    BlockPos origin = DataTypeUtils.readBlockPos(structurizeData.getCompound("primary_offset"));
                    metadata.setOriginalOrigin(origin);
                }
            }
        }

        return metadata;
    }

    protected boolean readFromTag_v1(DataView data, int version)
    {
        Vec3i size = readSizeFromTag(data);

        if (isSizeValid(size) == false)
        {
            return false;
        }

        this.enclosingSize = size;
        this.originalMetadataTag = data.getCompound("Metadata").copy();
        this.optionalDataTag = data.getCompound("optional_data").copy();
        this.requiredModsTag = data.getList("required_mods", Constants.NBT.TAG_STRING).copy();

        this.minecraftDataVersion = data.getIntOrDefault("mcversion", this.metadata.getMinecraftVersion().dataVersion);
        this.metadata = createAndReadMetadata(data, this.enclosingSize, version, this.minecraftDataVersion);

        BlockContainer container = this.readBlocksFromTag_v1(data, size, this.minecraftDataVersion);

        if (container == null)
        {
            return false;
        }

        ListData beList = data.getList("tile_entities", Constants.NBT.TAG_COMPOUND);
        ListData entityListData = data.getList("entities", Constants.NBT.TAG_COMPOUND);

        Map<BlockPos, CompoundData> blockEntityMap = new HashMap<>();
        List<EntityData> entityList = new ArrayList<>();

        int beErrorCount = this.readBlockEntities(beList, blockEntityMap);
        int entityErrorCount = this.readEntities(entityListData, entityList);

        if (beErrorCount > 0)
        {
            MessageDispatcher.warning("litematica.message.warn.schematic_read.failed_to_read_block_entities",
                                      beErrorCount, blockEntityMap.size());
        }

        if (entityErrorCount > 0)
        {
            MessageDispatcher.warning("litematica.message.warn.schematic_read.failed_to_read_entities",
                                      entityErrorCount, entityList.size());
        }

        SchematicRegion region = new SchematicRegion(BlockPos.ORIGIN, size, container, blockEntityMap,
                                                     new HashMap<>(), entityList, this.minecraftDataVersion);

        this.regions = ImmutableMap.of("Schematic", region);

        return true;
    }

    @Nullable
    protected BlockContainer readBlocksFromTag_v1(DataView data, Vec3i size, int dataVersion)
    {
        ListData paletteTag = data.getList("palette", Constants.NBT.TAG_COMPOUND);
        int paletteSize = paletteTag.size();
        int[] blockDataArray = data.getIntArray("blocks");
        long numBlocks = (long) size.getX() * size.getY() * size.getZ();
        int expectedLength = (int) Math.ceil(numBlocks / 2.0);

        if (blockDataArray == null || blockDataArray.length != expectedLength)
        {
            String len = blockDataArray == null ? "<null>" : String.valueOf(blockDataArray.length);
            MessageDispatcher.error("litematica.error.schematic_read.structurize.invalid_block_data_array", len);
            return null;
        }

        ArrayBlockContainer container = ArrayBlockContainer.createContainerIntArrayShortData(size, paletteSize, blockDataArray);

        if (container == null)
        {
            MessageDispatcher.error("litematica.error.schematic_read.litematica.region_container");
            return null;
        }

        if (readPaletteFromLitematicaFormatTag(paletteTag, container.getPalette(), dataVersion) == false)
        {
            MessageDispatcher.error("litematica.error.schematic_read.palette_read_failed");
            return null;
        }

        return container;
    }

    protected boolean write_v1(CompoundData data, SchematicRegion region, ArrayBlockContainer container, int version)
    {
        Optional<int[]> blockDataOpt = ArrayBlockContainer.convertToIntArrayOfShorts(container);

        if (blockDataOpt.isPresent() == false)
        {
            MessageDispatcher.error("litematica.error.schematic_write.block_write_failed");
            return false;
        }

        data.putByte("version", (byte) version);
        data.putInt("mcversion", this.minecraftDataVersion);
        data.putShort("size_x", (short) this.enclosingSize.getX());
        data.putShort("size_y", (short) this.enclosingSize.getY());
        data.putShort("size_z", (short) this.enclosingSize.getZ());
        data.putString("name", this.metadata.getSchematicName());

        CompoundData metaTag = this.originalMetadataTag.copy();
        this.metadata.write(metaTag);
        data.put("Metadata", metaTag);

        data.put("optional_data", this.optionalDataTag);
        data.put("required_mods", this.requiredModsTag);    // TODO generate this

        data.put("palette", writePaletteToLitematicaFormatTag(container.getPalette()));
        data.put("entities", this.getEntitiesAsListData(region.getEntityList()));
        data.put("tile_entities", this.getBlockEntitiesAsListData(region.getBlockEntityMap()));
        data.putIntArray("blocks", blockDataOpt.get());

        return true;
    }

    public static Optional<SchematicMetadata> createAndReadMetadata(DataView data)
    {
        int schematicVersion = getStructurizeVersion(data);

        if (schematicVersion <= 0)
        {
            return Optional.empty();
        }

        Vec3i size = readSizeFromTag(data); // The validity was already checked in getStructurizeVersion()
        int dataVersion = data.getInt("mcversion");
        SchematicMetadata metadata = createAndReadMetadata(data, size, schematicVersion, dataVersion);

        metadata.setEntityCount(data.getList("entities", Constants.NBT.TAG_COMPOUND).size());
        metadata.setBlockEntityCount(data.getList("tile_entities", Constants.NBT.TAG_COMPOUND).size());

        return Optional.of(metadata);
    }

    public static Optional<Schematic> fromData(DataView data)
    {
        StructurizeSchematic schematic = new StructurizeSchematic();

        if (schematic.read(data))
        {
            return Optional.of(schematic);
        }

        return Optional.empty();
    }

    public static Optional<Schematic> fromRegions(ImmutableMap<String, SchematicRegion> regions)
    {
        Optional<SchematicRegion> regionOpt = getOrConvertToSingleRegion(regions, SchematicType.STRUCTURIZE);

        if (regionOpt.isPresent() == false)
        {
            return Optional.empty();
        }

        SchematicRegion region = regionOpt.get();
        StructurizeSchematic schematic = new StructurizeSchematic();

        schematic.regions = regions;
        schematic.enclosingSize = PositionUtils.getAbsoluteSize(region.getSize());
        schematic.minecraftDataVersion = region.getMinecraftDataVersion();
        schematic.metadata.setSchematicVersion(CURRENT_SCHEMATIC_VERSION);

        return Optional.of(schematic);
    }

    public static BlockContainer createDefaultBlockContainer(Vec3i containerSize)
    {
        return new ArrayBlockContainer(containerSize);
    }
}
