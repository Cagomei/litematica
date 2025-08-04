package litematica.schematic;

import java.util.Optional;
import com.google.common.collect.ImmutableMap;

import malilib.util.data.Constants;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.DataView;
import malilib.util.data.tag.util.DataTypeUtils;
import malilib.util.game.MinecraftVersion;
import malilib.util.position.BlockPos;
import malilib.util.position.Vec3i;
import litematica.schematic.container.BlockContainer;
import litematica.schematic.container.SparseBlockContainer;

public class VanillaSchematic extends BaseSchematic
{
    public static final String FILE_NAME_EXTENSION = "nbt";

    public VanillaSchematic()
    {
        super(SchematicType.VANILLA);
    }

    @Override
    public boolean read(DataView dataIn)
    {
        return false;
    }

    @Override
    public CompoundData write()
    {
        CompoundData data = new CompoundData();
        return data;
    }

    public static Vec3i readSizeFromTag(DataView data)
    {
        BlockPos pos = DataTypeUtils.readBlockPosFromListTag(data, "size");
        return pos != null ? pos : Vec3i.ZERO;
    }

    public static boolean isValidData(DataView data)
    {
        if (data.containsList("blocks", Constants.NBT.TAG_COMPOUND) &&
            data.containsList("palette", Constants.NBT.TAG_COMPOUND) &&
            //data.contains("DataVersion", Constants.NBT.TAG_INT) &&
            data.containsList("size", Constants.NBT.TAG_INT))
        {
            return isSizeValid(readSizeFromTag(data));
        }

        return false;
    }

    public static BlockContainer createDefaultBlockContainer(Vec3i containerSize)
    {
        return new SparseBlockContainer(containerSize);
    }

    public static Optional<SchematicMetadata> createAndReadMetadata(DataView data)
    {
        if (isValidData(data) == false)
        {
            return Optional.empty();
        }

        SchematicMetadata metadata = new SchematicMetadata();
        Vec3i size = DataTypeUtils.readBlockPosFromArrayTagOrDefault(data, "size", BlockPos.ORIGIN);

        metadata.setAuthor(data.getString("author"));
        metadata.setEnclosingSize(size);
        metadata.setTotalVolume((long) size.getX() * size.getY() * size.getZ());
        metadata.setMinecraftVersion(MinecraftVersion.getOrCreateVersionFromDataVersion(data.getInt("DataVersion")));
        metadata.setEntityCount(data.getList("entities", Constants.NBT.TAG_COMPOUND).size());

        return Optional.of(metadata);
    }

    public static Optional<Schematic> fromData(DataView data)
    {
        VanillaSchematic schematic = new VanillaSchematic();

        if (schematic.read(data))
        {
            return Optional.of(schematic);
        }

        return Optional.empty();
    }

    public static Optional<Schematic> fromRegions(ImmutableMap<String, SchematicRegion> regions)
    {
        if (regions.size() == 1)
        {
            VanillaSchematic schematic = new VanillaSchematic();

            schematic.regions = regions;
            schematic.minecraftDataVersion = CURRENT_MINECRAFT_DATA_VERSION;

            return Optional.of(schematic);
        }

        return Optional.empty();
    }
}
