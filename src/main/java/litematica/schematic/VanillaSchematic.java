package litematica.schematic;

import java.util.Optional;
import com.google.common.collect.ImmutableMap;

import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.DataView;
import malilib.util.position.Vec3i;
import litematica.schematic.container.BlockContainer;

public class VanillaSchematic extends BaseSchematic
{
    public static final String FILE_NAME_EXTENSION = ".nbt";

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

    public static boolean isValidData(DataView data)
    {
        return false;
    }

    public static BlockContainer createDefaultBlockContainer(Vec3i containerSize)
    {
        return null;
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
