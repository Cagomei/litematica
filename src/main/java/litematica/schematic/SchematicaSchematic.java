package litematica.schematic;

import java.util.Optional;
import com.google.common.collect.ImmutableMap;

import malilib.util.data.Constants;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.DataView;
import malilib.util.position.Vec3i;
import litematica.schematic.container.BlockContainer;

public class SchematicaSchematic extends BaseSchematic
{
    public static final String FILE_NAME_EXTENSION = ".schematic";

    public SchematicaSchematic()
    {
        super(SchematicType.SCHEMATICA);
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

    public static boolean isValidData(DataView data)
    {
        if (data.contains("Width", Constants.NBT.TAG_SHORT) &&
            data.contains("Height", Constants.NBT.TAG_SHORT) &&
            data.contains("Length", Constants.NBT.TAG_SHORT) &&
            data.contains("Blocks", Constants.NBT.TAG_BYTE_ARRAY) &&
            data.contains("Data", Constants.NBT.TAG_BYTE_ARRAY))
        {
            return isSizeValid(SpongeSchematic.readSizeFromTag(data));
        }

        return false;
    }

    public static BlockContainer createDefaultBlockContainer(Vec3i containerSize)
    {
        return null;
    }

    public static Optional<SchematicMetadata> createAndReadMetadata(DataView data)
    {
        if (isValidData(data) == false)
        {
            return Optional.empty();
        }

        SchematicMetadata metadata = new SchematicMetadata();

        Vec3i size = SpongeSchematic.readSizeFromTag(data);
        metadata.setEnclosingSize(size);
        metadata.setTotalVolume((long) size.getX() * size.getY() * size.getZ());
        metadata.setEntityCount(data.getList("Entities", Constants.NBT.TAG_COMPOUND).size());
        metadata.setBlockEntityCount(data.getList("TileEntities", Constants.NBT.TAG_COMPOUND).size());

        return Optional.of(metadata);
    }

    public static Optional<Schematic> fromData(DataView data)
    {
        SchematicaSchematic schematic = new SchematicaSchematic();

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
            SchematicaSchematic schematic = new SchematicaSchematic();

            schematic.regions = regions;
            schematic.minecraftDataVersion = CURRENT_MINECRAFT_DATA_VERSION;

            return Optional.of(schematic);
        }

        return Optional.empty();
    }
}
