package litematica.schematic.container;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

import malilib.util.data.palette.HashMapPalette;
import malilib.util.data.palette.LinearPalette;
import malilib.util.data.palette.Palette;
import malilib.util.data.palette.PaletteResizeHandler;
import malilib.util.position.Vec3i;
import malilib.util.world.BlockState;

public abstract class BaseBlockContainer implements BlockContainer
{
    public static final BlockState AIR_BLOCK_STATE = BlockState.of(Blocks.AIR.getDefaultState());
    protected static final int MAX_BITS_LINEAR = 4;

    protected Palette<BlockState> palette;
    protected final Vec3i size;
    protected final int sizeX;
    protected final int sizeY;
    protected final int sizeZ;
    protected final long sizeLayer;
    protected final long totalVolume;
    protected int entryWidthBits;
    protected boolean hasSetBlockCounts;
    protected long[] blockCounts = new long[0];

    public BaseBlockContainer(Vec3i size)
    {
        this(size, 2);
    }

    protected BaseBlockContainer(Vec3i size, int entryWidthBits)
    {
        this.size = size;
        this.sizeX = size.getX();
        this.sizeY = size.getY();
        this.sizeZ = size.getZ();
        this.totalVolume = (long) this.sizeX * (long) this.sizeY * (long) this.sizeZ;
        this.sizeLayer = (long) this.sizeX * (long) this.sizeZ;

        this.setEntryWidthBits(entryWidthBits);
    }

    @Override
    public Vec3i getSize()
    {
        return this.size;
    }

    @Override
    public Palette<BlockState> getPalette()
    {
        return this.palette;
    }

    @Override
    public long getTotalBlockCount()
    {
        this.calculateBlockCountsIfNeeded();

        final int length = this.blockCounts.length;
        Palette<BlockState> palette = this.getPalette();
        long count = 0;

        for (int id = 0; id < length; ++id)
        {
            BlockState state = palette.getValue(id);

            if (state != null &&
                state != AIR_BLOCK_STATE &&
                state.vanillaState().getMaterial() != Material.AIR)
            {
                count += this.blockCounts[id];
            }
        }

        return count;
    }

    @Override
    public Object2LongOpenHashMap<BlockState> getBlockCountsMap()
    {
        this.calculateBlockCountsIfNeeded();

        Object2LongOpenHashMap<BlockState> map = new Object2LongOpenHashMap<>(this.blockCounts.length);
        Palette<BlockState> palette = this.getPalette();
        final int length = Math.min(palette.getSize(), this.blockCounts.length);

        for (int id = 0; id < length; ++id)
        {
            BlockState state = palette.getValue(id);

            if (state != null)
            {
                map.put(state, this.blockCounts[id]);
            }
        }

        return map;
    }

    protected void setBlockCounts(long[] blockCounts)
    {
        final int length = blockCounts.length;

        if (this.blockCounts == null || this.blockCounts.length < length)
        {
            this.blockCounts = new long[length];
        }

        System.arraycopy(blockCounts, 0, this.blockCounts, 0, length);
        this.hasSetBlockCounts = true;
    }

    protected void setEntryWidthBits(int bitsIn)
    {
        this.entryWidthBits = bitsIn;
    }

    protected abstract void calculateBlockCountsIfNeeded();

    public static Palette<BlockState> createPalette(int bits, PaletteResizeHandler<BlockState> resizeHandler)
    {
        if (bits <= MAX_BITS_LINEAR)
        {
            return new LinearPalette<>(bits, resizeHandler);
        }
        else
        {
            return new HashMapPalette<>(bits, resizeHandler);
        }
    }
}
