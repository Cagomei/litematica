package litematica.schematic.data;

import net.minecraft.block.Block;

import malilib.util.position.BlockPos;

public class ScheduledBlockTickData
{
    public final BlockPos pos;
    public final Block block;
    public final int priority;
    public final long delay;
    public final long tickId;

    public ScheduledBlockTickData(BlockPos pos, Block block, int priority, long delay, long tickId)
    {
        this.pos = pos;
        this.block = block;
        this.priority = priority;
        this.delay = delay;
        this.tickId = tickId;
    }
}
