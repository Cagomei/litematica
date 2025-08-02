package litematica.schematic;

import java.util.Optional;
import com.google.common.collect.ImmutableMap;

import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.DataView;
import malilib.util.position.Vec3i;
import litematica.schematic.container.ArrayBlockContainer;
import litematica.schematic.container.BlockContainer;

public class LitematicaSchematic extends BaseSchematic
{
    public static final String FILE_NAME_EXTENSION = ".litematic";
    public static final int CURRENT_SCHEMATIC_VERSION = 4;

    public LitematicaSchematic()
    {
        super(SchematicType.LITEMATICA);
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
        return new ArrayBlockContainer(containerSize);
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
        LitematicaSchematic schematic = new LitematicaSchematic();

        schematic.regions = regions;
        schematic.minecraftDataVersion = CURRENT_MINECRAFT_DATA_VERSION;

        return Optional.of(schematic);
    }
}
