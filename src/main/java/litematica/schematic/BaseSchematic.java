package litematica.schematic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;

import net.minecraft.client.Minecraft;

import malilib.util.data.Constants;
import malilib.util.data.palette.Palette;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.DataTypeUtils;
import malilib.util.data.tag.ListData;
import malilib.util.game.BlockUtils;
import malilib.util.position.BlockPos;
import malilib.util.position.Vec3d;
import malilib.util.position.Vec3i;
import malilib.util.world.BlockState;
import litematica.mixin.IMixinDataFixer;
import litematica.schematic.container.BlockContainer;
import litematica.schematic.data.EntityData;

public abstract class BaseSchematic implements Schematic
{
    public static final int CURRENT_MINECRAFT_DATA_VERSION = ((IMixinDataFixer) Minecraft.getMinecraft().getDataFixer()).getVersion();

    protected final SchematicType type;

    protected ImmutableMap<String, SchematicRegion> regions = ImmutableMap.of();
    protected SchematicMetadata metadata = new SchematicMetadata();
    protected Vec3i enclosingSize = Vec3i.ZERO;
    protected int minecraftDataVersion = CURRENT_MINECRAFT_DATA_VERSION;

    protected BaseSchematic(SchematicType type)
    {
        this.type = type;
    }

    @Override
    public SchematicType getType()
    {
        return this.type;
    }

    @Override
    public SchematicMetadata getMetadata()
    {
        return this.metadata;
    }

    @Override
    public Vec3i getEnclosingSize()
    {
        return this.enclosingSize;
    }

    @Override
    public ImmutableMap<String, SchematicRegion> getRegions()
    {
        return this.regions;
    }

    public static boolean isSizeValid(@Nullable Vec3i size)
    {
        return size != null && size.getX() > 0 && size.getY() > 0 && size.getZ() > 0;
    }

    public static void copyContainerContents(BlockContainer from, BlockContainer to)
    {
        Vec3i sizeFrom = from.getSize();
        Vec3i sizeTo = to.getSize();
        final int sizeX = Math.min(sizeFrom.getX(), sizeTo.getX());
        final int sizeY = Math.min(sizeFrom.getY(), sizeTo.getY());
        final int sizeZ = Math.min(sizeFrom.getZ(), sizeTo.getZ());

        for (int y = 0; y < sizeY; ++y)
        {
            for (int z = 0; z < sizeZ; ++z)
            {
                for (int x = 0; x < sizeX; ++x)
                {
                    BlockState state = from.getBlockState(x, y, z);
                    to.setBlockState(x, y, z, state);
                }
            }
        }
    }

    public static boolean readPaletteFromLitematicaFormatTag(ListData listData, Palette<BlockState> palette)
    {
        final int size = listData.size();
        List<BlockState> list = new ArrayList<>(size);

        for (int id = 0; id < size; ++id)
        {
            CompoundData compound = listData.getCompoundAt(id);
            BlockState state = BlockUtils.readBlockState(compound);
            list.add(state);
        }

        return palette.setMapping(list);
    }

    public static List<EntityData> readEntitiesFromList(ListData listData)
    {
        final int size = listData.size();
        List<EntityData> entityList = new ArrayList<>(size);

        for (int i = 0; i < size; ++i)
        {
            CompoundData compound = listData.getCompoundAt(i);
            Vec3d posVec = DataTypeUtils.readVec3dFromListTag(compound);

            if (posVec != null && compound.isEmpty() == false)
            {
                entityList.add(new EntityData(posVec, compound));
            }
        }

        return entityList;
    }

    public static Map<BlockPos, CompoundData> readBlockEntitiesFromList(ListData listData)
    {
        final int size = listData.size();
        Map<BlockPos, CompoundData> map = new HashMap<>();

        for (int i = 0; i < size; ++i)
        {
            CompoundData compound = listData.getCompoundAt(i);
            BlockPos pos = DataTypeUtils.readBlockPos(compound);
            DataTypeUtils.removeBlockPosFromTag(compound);

            if (pos != null && compound.isEmpty() == false)
            {
                map.put(pos, compound);
            }
        }

        return map;
    }

    public static ListData writePaletteToLitematicaFormatTag(Palette<BlockState> palette)
    {
        final int size = palette.getSize();
        List<BlockState> mapping = palette.getMapping();
        ListData listData = new ListData(Constants.NBT.TAG_COMPOUND);

        for (int id = 0; id < size; ++id)
        {
            CompoundData compound = new CompoundData();
            BlockUtils.writeBlockState(compound, mapping.get(id));
            listData.add(compound);
        }

        return listData;
    }

    public static ListData writeEntitiesToList(List<EntityData> entityList)
    {
        ListData listData = new ListData(Constants.NBT.TAG_COMPOUND);

        for (EntityData info : entityList)
        {
            listData.add(info.data);
        }

        return listData;
    }

    public static ListData writeBlockEntitiesToList(Map<BlockPos, CompoundData> blockEntities)
    {
        ListData listData = new ListData(Constants.NBT.TAG_COMPOUND);

        for (Map.Entry<BlockPos, CompoundData> entry : blockEntities.entrySet())
        {
            CompoundData compound = entry.getValue();
            DataTypeUtils.putVec3i(compound, entry.getKey());
            listData.add(compound);
        }

        return listData;
    }
}
