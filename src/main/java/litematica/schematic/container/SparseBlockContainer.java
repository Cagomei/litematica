package litematica.schematic.container;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import malilib.util.position.Vec3i;
import malilib.util.world.BlockState;

public class SparseBlockContainer extends BaseBlockContainer
{
    protected final Long2ObjectOpenHashMap<BlockState> blocks = new Long2ObjectOpenHashMap<>();

    public SparseBlockContainer(Vec3i size)
    {
        super(size);

        this.palette = new NonResizingHashMapPalette<>(1024);
        this.blockCounts = new long[1024];
    }

    @Override
    public BlockState getBlockState(int x, int y, int z)
    {
        long pos = (long) y << 32 | (long) (z & 0xFFFF) << 16 | (long) (x & 0xFFFF);
        BlockState state = this.blocks.get(pos);
        return state != null ? state : AIR_BLOCK_STATE;
    }

    @Override
    public void setBlockState(int x, int y, int z, BlockState state)
    {
        long pos = (long) y << 32 | (long) (z & 0xFFFF) << 16 | (long) (x & 0xFFFF);

        BlockState oldState = this.blocks.put(pos, state);
        int id = this.palette.idFor(state);

        if (id >= this.blockCounts.length)
        {
            long[] oldArr = this.blockCounts;
            this.blockCounts = new long[oldArr.length * 2];
            System.arraycopy(oldArr, 0, this.blockCounts, 0, oldArr.length);
        }

        if (oldState != state)
        {
            if (oldState != null)
            {
                int oldId = this.palette.idFor(oldState);
                --this.blockCounts[oldId];
            }

            ++this.blockCounts[id];
        }
    }

    @Override
    public BlockContainer copy()
    {
        SparseBlockContainer copy = new SparseBlockContainer(this.size);
        copy.blocks.putAll(this.blocks);
        copy.blockCounts = this.blockCounts.clone();
        copy.palette = this.palette.copy(null);
        return copy;
    }

    @Override
    protected void calculateBlockCountsIfNeeded()
    {
    }

    public Long2ObjectOpenHashMap<BlockState> getBlockMap()
    {
        return this.blocks;
    }
}
