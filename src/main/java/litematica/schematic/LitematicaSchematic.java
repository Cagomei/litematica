package litematica.schematic;

import java.util.Optional;
import com.google.common.collect.ImmutableMap;

import malilib.util.data.Constants;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.DataView;
import malilib.util.game.MinecraftVersion;
import malilib.util.position.Vec3i;
import litematica.schematic.container.ArrayBlockContainer;
import litematica.schematic.container.BlockContainer;

public class LitematicaSchematic extends BaseSchematic
{
    public static final String FILE_NAME_EXTENSION = "litematic";
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
        metadata.setMinecraftVersion(MinecraftVersion.getOrCreateVersionFromDataVersion(data.getInt("MinecraftDataVersion")));
        metadata.setSchematicVersion(data.getInt("Version"));

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
        LitematicaSchematic schematic = new LitematicaSchematic();

        schematic.regions = regions;
        schematic.minecraftDataVersion = CURRENT_MINECRAFT_DATA_VERSION;

        return Optional.of(schematic);
    }
}
