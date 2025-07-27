package litematica.schematic.container;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import net.minecraft.block.state.IBlockState;

import malilib.util.position.Vec3i;
import litematica.schematic.old.container.ILitematicaBlockStatePalette;

public interface BlockContainer
{
    /**
     * @return the size/dimensions of this container
     */
    Vec3i getSize();

    /**
     * @return the total number of non-air blocks
     */
    long getTotalBlockCount();

    /**
     * @return the block state count per unique state
     */
    Object2LongOpenHashMap<IBlockState> getBlockCountsMap();

    /**
     * @return the block state palette used in this container
     */
    ILitematicaBlockStatePalette getPalette();

    /**
     * @return the block state at the given position in the container.
     * If the position is out of bounds, then AIR is returned.
     */
    IBlockState getBlockState(int x, int y, int z);

    /**
     * Set the block state in the given position.
     * If the position is out of bounds, then nothing happens.
     */
    void setBlockState(int x, int y, int z, IBlockState state);

    /**
     * Creates and returns a copy of this block container
     * @return a copy of this container
     */
    BlockContainer copy();
}
