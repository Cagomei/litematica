package litematica.schematic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.ByteBufUtils;
import malilib.util.ListUtils;
import malilib.util.data.Constants;
import malilib.util.data.palette.Palette;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.DataView;
import malilib.util.data.tag.ListData;
import malilib.util.data.tag.util.DataTypeUtils;
import malilib.util.game.BlockUtils;
import malilib.util.game.MinecraftVersion;
import malilib.util.position.BlockPos;
import malilib.util.position.Vec3d;
import malilib.util.position.Vec3i;
import malilib.util.world.BlockState;
import litematica.schematic.container.ArrayBlockContainer;
import litematica.schematic.container.BlockContainer;
import litematica.schematic.container.TightLongBackedBitArray;
import litematica.schematic.data.EntityData;
import litematica.util.PositionUtils;

public class SpongeSchematic extends BaseSchematic
{
    public static final int CURRENT_SCHEMATIC_VERSION = 2;
    public static final String FILE_NAME_EXTENSION = "schem";

    protected CompoundData originalMetadataTag = new CompoundData();

    public SpongeSchematic()
    {
        super(SchematicType.SPONGE);
    }

    @Override
    public boolean read(DataView data)
    {
        int version = getSpongeVersion(data);

        if (version == 1 || version == 2)
        {
            return this.readFromTag_v1_2(data, version);
        }

        if (version == 3)
        {
            return this.readFromTag_v3(data, version);
        }

        return false;
    }

    @Override
    public Optional<CompoundData> write()
    {
        CompoundData data = new CompoundData();
        int regionCount = this.regions.size();

        if (regionCount != 1)
        {
            MessageDispatcher.error("litematica.message.error.schematic_save.wrong_region_count", regionCount, 1);
            return Optional.empty();
        }

        int version = CURRENT_SCHEMATIC_VERSION;    // TODO add a way to specify this
        this.metadata.setSchematicVersion(version);

        SchematicRegion region = ListUtils.getFirstEntry(this.regions.values());
        ArrayBlockContainer container = (ArrayBlockContainer) region.getBlockContainer();
        boolean success = false;

        if (version == 3)
        {
            success = this.write_v3(data, region, container, version);
        }
        else if (version == 1 || version == 2)
        {
            success = this.write_v1_2(data, region, container, version);
        }

        return success ? Optional.of(data) : Optional.empty();
    }

    public static boolean isValidData(DataView data)
    {
        return isValidData_v1_2(data) ||
               isValidData_v3(data);
    }

    public static boolean isValidData_v1_2(DataView data)
    {
        // Version 1 or 2
        if (data.contains("Width", Constants.NBT.TAG_ANY_NUMERIC) &&
            data.contains("Height", Constants.NBT.TAG_ANY_NUMERIC) &&
            data.contains("Length", Constants.NBT.TAG_ANY_NUMERIC) &&
            data.contains("Version", Constants.NBT.TAG_INT) &&
            data.contains("Palette", Constants.NBT.TAG_COMPOUND) &&
            data.contains("BlockData", Constants.NBT.TAG_BYTE_ARRAY))
        {
            int version = data.getInt("Version");
            return (version == 1 || version == 2) && isSizeValid(readSizeFromTag(data));
        }

        return false;
    }

    public static boolean isValidData_v3(DataView data)
    {
        // Version 3
        if (data.contains("Schematic", Constants.NBT.TAG_COMPOUND))
        {
            DataView schTag = data.getCompound("Schematic");

            if (schTag.contains("Width", Constants.NBT.TAG_ANY_NUMERIC) &&
                schTag.contains("Height", Constants.NBT.TAG_ANY_NUMERIC) &&
                schTag.contains("Length", Constants.NBT.TAG_ANY_NUMERIC) &&
                schTag.contains("Version", Constants.NBT.TAG_INT) &&
                schTag.contains("Blocks", Constants.NBT.TAG_COMPOUND) &&
                schTag.getCompound("Blocks").contains("Palette", Constants.NBT.TAG_COMPOUND) &&
                schTag.getCompound("Blocks").contains("Data", Constants.NBT.TAG_BYTE_ARRAY))
            {
                return isSizeValid(readSizeFromTag(schTag));
            }
        }

        return false;
    }

    public static int getSpongeVersion(DataView data)
    {
        if (isValidData_v1_2(data))
        {
            return data.getInt("Version");
        }
        else if (isValidData_v3(data))
        {
            return data.getCompound("Schematic").getInt("Version");
        }

        return -1;
    }

    protected static Vec3i readSizeFromTag(DataView tag)
    {
        return new Vec3i(tag.getShort("Width"),
                         tag.getShort("Height"),
                         tag.getShort("Length"));
    }

    public static SchematicMetadata createAndReadMetadata(DataView metaTag, Vec3i size, int version)
    {
        SchematicMetadata metadata = new SchematicMetadata();
        metadata.read(metaTag);
        metadata.setSchematicVersion(version);
        metadata.setEnclosingSize(size);
        metadata.setTotalVolume((long) size.getX() * size.getY() * size.getZ());

        if (metaTag.contains("Date", Constants.NBT.TAG_LONG) &&
            metadata.getTimeCreated() <= 0)
        {
            long time = metaTag.getLong("Date");
            metadata.setTimeCreated(time);
        }

        if (metaTag.contains("WorldEdit", Constants.NBT.TAG_COMPOUND))
        {
            CompoundData weTag = metaTag.getCompound("WorldEdit");
            BlockPos origin = DataTypeUtils.readBlockPosFromArrayTag(weTag, "Origin");

            if (origin != null)
            {
                metadata.setOriginalOrigin(origin);
            }
        }

        return metadata;
    }

    protected boolean readFromTag_v1_2(DataView data, int version)
    {
        Vec3i size = readSizeFromTag(data);

        if (isSizeValid(size) == false)
        {
            return false;
        }

        BlockContainer container = this.readBlocksFromTag_v1_2(data, size);

        if (container == null)
        {
            return false;
        }

        Map<BlockPos, CompoundData> blockEntityMap = new HashMap<>();
        int errorCount = this.readBlockEntitiesFromTag_v1_2(data, blockEntityMap, version);

        if (errorCount > 0)
        {
            MessageDispatcher.warning("litematica.message.warn.schematic_read.failed_to_read_block_entities",
                                      errorCount, blockEntityMap.size());
        }

        List<EntityData> entityList = new ArrayList<>();
        errorCount = this.readEntitiesFromTag_v1_2(data, entityList, version);

        if (errorCount > 0)
        {
            MessageDispatcher.warning("litematica.message.warn.schematic_read.failed_to_read_entities",
                                      errorCount, entityList.size());
        }

        this.enclosingSize = size;
        this.originalMetadataTag = data.getCompound("Metadata").copy();
        this.metadata = createAndReadMetadata(this.originalMetadataTag, this.enclosingSize, version);

        this.minecraftDataVersion = data.getIntOrDefault("DataVersion", this.metadata.getMinecraftVersion().dataVersion);
        SchematicRegion region = new SchematicRegion(BlockPos.ORIGIN, size, container, blockEntityMap,
                                                     new HashMap<>(), entityList, this.minecraftDataVersion);

        this.regions = ImmutableMap.of("Schematic", region);

        return true;
    }

    protected boolean readFromTag_v3(DataView data, int version)
    {
        CompoundData schTag = data.getCompound("Schematic");
        Vec3i size = readSizeFromTag(schTag);

        if (isSizeValid(size) == false)
        {
            return false;
        }

        CompoundData blockTag = schTag.getCompound("Blocks");
        BlockContainer container = this.readBlocksFromTag_v3(blockTag, size);

        if (container == null)
        {
            return false;
        }

        Map<BlockPos, CompoundData> blockEntityMap = new HashMap<>();
        int errorCount = this.readBlockEntitiesFromTag_v3(blockTag, blockEntityMap);

        if (errorCount > 0)
        {
            MessageDispatcher.warning("litematica.message.warn.schematic_read.failed_to_read_block_entities",
                                      errorCount, blockEntityMap.size());
        }

        List<EntityData> entityList = new ArrayList<>();
        errorCount = readEntitiesFromTag_v3(schTag, entityList);

        if (errorCount > 0)
        {
            MessageDispatcher.warning("litematica.message.warn.schematic_read.failed_to_read_entities",
                                      errorCount, entityList.size());
        }

        this.minecraftDataVersion = schTag.getIntOrDefault("DataVersion", -1);
        SchematicRegion region = new SchematicRegion(BlockPos.ORIGIN, size, container, blockEntityMap,
                                                     new HashMap<>(), entityList, this.minecraftDataVersion);

        this.enclosingSize = size;
        this.originalMetadataTag = schTag.getCompound("Metadata").copy();
        this.regions = ImmutableMap.of("Schematic", region);
        this.metadata = createAndReadMetadata(this.originalMetadataTag, this.enclosingSize, version);

        return true;
    }

    @Nullable
    protected BlockContainer readBlocksFromTag_v1_2(DataView data, Vec3i size)
    {
        CompoundData paletteTag = data.getCompound("Palette");
        byte[] blockData = data.getByteArray("BlockData");
        int paletteSize = paletteTag.size();

        ArrayBlockContainer container = ArrayBlockContainer.createContainer(size, paletteSize, blockData);

        if (this.readPaletteFromCompound(paletteTag, container.getPalette()) == false)
        {
            MessageDispatcher.error("litematica.message.error.schematic_read.sponge.failed_to_read_blocks");
            return null;
        }

        return container;
    }

    @Nullable
    protected BlockContainer readBlocksFromTag_v3(DataView data, Vec3i size)
    {
        CompoundData paletteTag = data.getCompound("Palette");
        byte[] blockData = data.getByteArray("Data");
        int paletteSize = paletteTag.size();

        ArrayBlockContainer container = ArrayBlockContainer.createContainer(size, paletteSize, blockData);

        if (this.readPaletteFromCompound(paletteTag, container.getPalette()) == false)
        {
            MessageDispatcher.error("litematica.message.error.schematic_read.sponge.failed_to_read_blocks");
            return null;
        }

        return container;
    }

    protected boolean readPaletteFromCompound(DataView data, Palette<BlockState> palette)
    {
        final int size = data.size();
        List<BlockState> list = new ArrayList<>(size);

        for (int i = 0; i < size; ++i)
        {
            list.add(null);
        }

        Set<String> keys = data.getKeys();

        for (String key : keys)
        {
            Optional<BlockState> stateOptional = BlockUtils.getBlockStateFromString(key);
            int id = data.getInt(key);
            BlockState state;

            if (stateOptional.isPresent())
            {
                state = stateOptional.get();
            }
            else
            {
                MessageDispatcher.warning().translate("litematica.message.error.schematic_read.sponge.palette.unknown_block", key);
                state = BlockState.AIR;
            }

            if (id < 0 || id >= size)
            {
                MessageDispatcher.error().translate("litematica.message.error.schematic_read.sponge.palette.invalid_id", id);
                return false;
            }

            list.set(id, state);
        }

        return palette.setMapping(list);
    }

    protected int readBlockEntitiesFromTag_v1_2(DataView tag, Map<BlockPos, CompoundData> blockEntityMapOut, int version)
    {
        String tagName = version == 1 ? "TileEntities" : "BlockEntities";
        ListData listData = tag.getList(tagName, Constants.NBT.TAG_COMPOUND);
        final int size = listData.size();
        int errorCount = 0;

        for (int i = 0; i < size; ++i)
        {
            CompoundData beTag = listData.getCompoundAt(i).copy();
            BlockPos pos = DataTypeUtils.readBlockPosFromArrayTag(beTag, "Pos");

            if (pos == null)
            {
                ++errorCount;
                continue;
            }

            beTag.putString("id", beTag.getString("Id"));

            // Remove the Sponge tags from the data that is kept in memory
            beTag.remove("Id");
            beTag.remove("Pos");

            if (version == 1)
            {
                beTag.remove("ContentVersion");
            }

            blockEntityMapOut.put(pos, beTag);
        }

        return errorCount;
    }

    protected int readBlockEntitiesFromTag_v3(DataView tag, Map<BlockPos, CompoundData> blockEntityMapOut)
    {
        ListData listData = tag.getList("BlockEntities", Constants.NBT.TAG_COMPOUND);
        final int size = listData.size();
        int errorCount = 0;

        for (int i = 0; i < size; ++i)
        {
            CompoundData beTag = listData.getCompoundAt(i);
            BlockPos pos = DataTypeUtils.readBlockPosFromArrayTag(beTag, "Pos");

            if (pos == null || beTag.contains("Data", Constants.NBT.TAG_COMPOUND) == false)
            {
                ++errorCount;
                continue;
            }

            CompoundData dataTag = beTag.getCompound("Data").copy();
            blockEntityMapOut.put(pos, dataTag);
        }

        return errorCount;
    }

    protected int readEntitiesFromTag_v1_2(DataView tag, List<EntityData> entityListOut, int version)
    {
        ListData listData = tag.getList("Entities", Constants.NBT.TAG_COMPOUND);
        final int size = listData.size();
        int errorCount = 0;

        for (int i = 0; i < size; ++i)
        {
            CompoundData entityTag = listData.getCompoundAt(i).copy();
            Vec3d pos = DataTypeUtils.readVec3dFromListTag(entityTag);

            if (pos == null)
            {
                ++errorCount;
                continue;
            }

            entityTag.putString("id", entityTag.getString("Id"));
            entityTag.remove("Id"); // Remove the Sponge tags from the data that is kept in memory

            if (version == 1)
            {
                entityTag.remove("ContentVersion");
            }

            entityListOut.add(new EntityData(pos, entityTag));
        }

        return errorCount;
    }

    protected static int readEntitiesFromTag_v3(DataView tag, List<EntityData> entityListOut)
    {
        ListData listData = tag.getList("Entities", Constants.NBT.TAG_COMPOUND);
        final int size = listData.size();
        int errorCount = 0;

        for (int i = 0; i < size; ++i)
        {
            CompoundData entityTag = listData.getCompoundAt(i);
            Vec3d pos = DataTypeUtils.readVec3dFromListTag(entityTag);

            if (pos == null || entityTag.contains("Data", Constants.NBT.TAG_COMPOUND) == false)
            {
                ++errorCount;
                continue;
            }

            CompoundData entityData = entityTag.getCompound("Data").copy();
            entityListOut.add(new EntityData(pos, entityData));
        }

        return errorCount;
    }

    protected static void writeMetadataToTag(CompoundData tag, CompoundData originalMetaTag, SchematicMetadata meta)
    {
        CompoundData metaTag = originalMetaTag.copy();

        meta.write(metaTag);

        if (meta.getTimeCreated() > 0 && originalMetaTag.contains("Date", Constants.NBT.TAG_LONG) == false)
        {
            metaTag.putLong("Date", meta.getTimeCreated());
        }

        tag.put("Metadata", metaTag);
    }

    // Note that the tag passed in will be a nested tag in v3
    protected static boolean writeBlockDataToTag(ArrayBlockContainer blockContainer, CompoundData tag, int version)
    {
        byte[] blockData = convertBitArrayToVarIntByteArray(blockContainer);

        if (blockData == null)
        {
            return false;
        }

        CompoundData paletteTag = writePaletteToTag(blockContainer.getPalette().getMapping());
        tag.put("Palette", paletteTag);

        if (version < 3)
        {
            tag.putByteArray("BlockData", blockData);
        }
        else
        {
            tag.putByteArray("Data", blockData);
        }

        return true;
    }

    @Nullable
    public static byte[] convertBitArrayToVarIntByteArray(ArrayBlockContainer container)
    {
        TightLongBackedBitArray bitArray = container.getBitArray();
        final int entrySize = ByteBufUtils.getVarIntSize(container.getPalette().getSize() - 1);
        final long volume = bitArray.size();
        final long length = entrySize * volume;

        if (length > Integer.MAX_VALUE)
        {
            MessageDispatcher.error(8000).translate("litematica.message.error.schematic_save.convert_data.sponge_array_size_overflow", length);
            return null;
        }

        byte[] arr = new byte[(int) length];
        ByteBuf buf = Unpooled.wrappedBuffer(arr);
        buf.writerIndex(0);

        for (int i = 0; i < volume; ++i)
        {
            ByteBufUtils.writeVarInt(buf, bitArray.getAt(i));
        }

        return arr;
    }

    protected static CompoundData writePaletteToTag(List<BlockState> mapping)
    {
        final int size = mapping.size();
        CompoundData tag = new CompoundData();

        for (int id = 0; id < size; ++id)
        {
            BlockState state = mapping.get(id);
            tag.putInt(state.vanillaState().toString(), id);
        }

        return tag;
    }

    protected static void writeBlockEntitiesToTag_v1_2(Map<BlockPos, CompoundData> blockEntityMap, CompoundData tag, int version)
    {
        String tagName = version == 1 ? "TileEntities" : "BlockEntities";
        ListData listData = new ListData(Constants.NBT.TAG_COMPOUND);

        for (Map.Entry<BlockPos, CompoundData> entry : blockEntityMap.entrySet())
        {
            CompoundData beTag = entry.getValue().copy();
            DataTypeUtils.writeVec3iToArrayTag(beTag, "Pos", entry.getKey());

            // Add the Sponge tag and remove the vanilla/Litematica tag
            beTag.putString("Id", beTag.getString("id"));
            beTag.remove("id");

            if (version == 1)
            {
                beTag.putInt("ContentVersion", 1);
            }

            listData.add(beTag);
        }

        tag.put(tagName, listData);
    }

    protected static void writeBlockEntitiesToTag_v3(Map<BlockPos, CompoundData> blockEntityMap, CompoundData tag)
    {
        ListData listData = new ListData(Constants.NBT.TAG_COMPOUND);

        for (Map.Entry<BlockPos, CompoundData> entry : blockEntityMap.entrySet())
        {
            CompoundData beTag = new CompoundData();
            CompoundData beData = entry.getValue().copy();

            beTag.put("Data", beData);
            beTag.putString("Id", beData.getString("id"));
            DataTypeUtils.writeVec3iToArrayTag(beTag, "Pos", entry.getKey());

            listData.add(beTag);
        }

        tag.put("BlockEntities", listData);
    }

    protected static void writeEntitiesToTag_v1_2(List<EntityData> entityList, CompoundData tag, int version)
    {
        ListData listData = new ListData(Constants.NBT.TAG_COMPOUND);

        for (EntityData data : entityList)
        {
            CompoundData entityData = data.data.copy();
            DataTypeUtils.writeVec3dToListTag(entityData, data.pos);

            // Add the Sponge tag and remove the vanilla/Litematica tag
            entityData.putString("Id", entityData.getString("id"));
            entityData.remove("id");

            if (version == 1)
            {
                entityData.putInt("ContentVersion", 1);
            }

            listData.add(entityData);
        }

        tag.put("Entities", listData);
    }

    protected static void writeEntitiesToTag_v3(List<EntityData> entityList, CompoundData tag)
    {
        ListData listData = new ListData(Constants.NBT.TAG_COMPOUND);

        for (EntityData data : entityList)
        {
            CompoundData entityTag = new CompoundData();
            CompoundData entityData = data.data.copy();

            entityTag.put("Data", entityData);
            entityTag.putString("Id", entityData.getString("id"));
            DataTypeUtils.writeVec3dToListTag(entityTag, data.pos);

            listData.add(entityData);
        }

        tag.put("Entities", listData);
    }

    protected void writeSizeAndVersions(CompoundData dataTag, Vec3i size, int version)
    {
        dataTag.putShort("Width", (short) size.getX());
        dataTag.putShort("Height", (short) size.getY());
        dataTag.putShort("Length", (short) size.getZ());

        dataTag.putInt("Version", version);
        dataTag.putInt("DataVersion", this.minecraftDataVersion);
    }

    public boolean write_v1_2(CompoundData dataTag, SchematicRegion region, ArrayBlockContainer blockContainer, int version)
    {
        Vec3i size = PositionUtils.getAbsoluteSize(region.getSize());
        writeMetadataToTag(dataTag, this.originalMetadataTag, this.metadata);
        this.writeSizeAndVersions(dataTag, size, version);

        dataTag.putInt("PaletteMax", blockContainer.getPalette().getSize() - 1);

        if (writeBlockDataToTag(blockContainer, dataTag, version) == false)
        {
            return false;
        }

        writeBlockEntitiesToTag_v1_2(region.getBlockEntityMap(), dataTag, version);
        writeEntitiesToTag_v1_2(region.getEntityList(), dataTag, version);

        return true;
    }

    public boolean write_v3(CompoundData data, SchematicRegion region, ArrayBlockContainer blockContainer, int version)
    {
        CompoundData dataTag = new CompoundData();
        data.put("Schematic", dataTag);

        Vec3i size = region.getSize();
        writeMetadataToTag(dataTag, this.originalMetadataTag, this.metadata);
        this.writeSizeAndVersions(dataTag, size, version);

        CompoundData blockTag = new CompoundData();

        if (writeBlockDataToTag(blockContainer, blockTag, version) == false)
        {
            return false;
        }

        dataTag.put("Blocks", blockTag);
        writeBlockEntitiesToTag_v3(region.getBlockEntityMap(), blockTag);
        writeEntitiesToTag_v3(region.getEntityList(), dataTag);

        return true;
    }

    public static Optional<SchematicMetadata> createAndReadMetadata(DataView data)
    {
        int spongeVersion = getSpongeVersion(data);

        if (spongeVersion <= 0)
        {
            return Optional.empty();
        }

        DataView wrapperTag = data;

        if (spongeVersion == 3)
        {
            wrapperTag = data.getCompound("Schematic");
        }
        else if (spongeVersion != 1 && spongeVersion != 2)
        {
            return Optional.empty();
        }

        Vec3i size = readSizeFromTag(wrapperTag); // The validity was already checked in getSpongeVersion()
        DataView metaTag = wrapperTag.getCompound("Metadata");
        SchematicMetadata metadata = createAndReadMetadata(metaTag, size, spongeVersion);

        metadata.setMinecraftVersion(MinecraftVersion.getOrCreateVersionFromDataVersion(wrapperTag.getInt("DataVersion")));
        metadata.setEntityCount(wrapperTag.getList("Entities", Constants.NBT.TAG_COMPOUND).size());

        if (spongeVersion == 1 || spongeVersion == 2)
        {
            String beTagName = spongeVersion == 1 ? "TileEntities" : "BlockEntities";
            metadata.setBlockEntityCount(wrapperTag.getList(beTagName, Constants.NBT.TAG_COMPOUND).size());
        }
        else if (spongeVersion == 3)
        {
            metadata.setBlockEntityCount(wrapperTag.getCompound("Blocks").getList("BlockEntities", Constants.NBT.TAG_COMPOUND).size());
        }

        return Optional.of(metadata);
    }

    public static Optional<Schematic> fromData(DataView data)
    {
        SpongeSchematic schematic = new SpongeSchematic();

        if (schematic.read(data))
        {
            return Optional.of(schematic);
        }

        return Optional.empty();
    }

    public static Optional<Schematic> fromRegions(ImmutableMap<String, SchematicRegion> regions)
    {
        Optional<SchematicRegion> regionOpt = getOrConvertToSingleRegion(regions, SchematicType.SPONGE);

        if (regionOpt.isPresent() == false)
        {
            return Optional.empty();
        }

        SchematicRegion region = regionOpt.get();
        SpongeSchematic schematic = new SpongeSchematic();

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
