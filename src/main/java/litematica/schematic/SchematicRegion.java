package litematica.schematic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import malilib.util.nbt.CompoundData;
import malilib.util.position.BlockPos;
import malilib.util.position.Vec3i;
import litematica.schematic.container.BlockContainer;
import litematica.schematic.data.EntityData;
import litematica.schematic.data.ScheduledBlockTickData;

public class SchematicRegion
{
    protected final BlockPos pos;
    protected final Vec3i size;
    protected final BlockContainer blockContainer;
    protected Map<BlockPos, CompoundData> blockEntityData = new HashMap<>();
    protected Map<BlockPos, ScheduledBlockTickData> blockTickData = new HashMap<>();
    protected List<EntityData> entityData = new ArrayList<>();

    public SchematicRegion(BlockPos pos, Vec3i size, BlockContainer blockContainer)
    {
        this.pos = pos;
        this.size = size;
        this.blockContainer = blockContainer;
    }

    /**
     * @return the relative position of this region in relation to the origin of the entire schematic
     */
    public BlockPos getRelativePosition()
    {
        return this.pos;
    }

    /**
     * @return the size of this region.
     * <b>Note:</b> The size can be negative on any axis, if the second corner is on the negative side
     * on that axis compared to the primary/origin corner.
     */
    public Vec3i getSize()
    {
        return this.size;
    }

    /**
     * @return the block state container used for storing the block states in this region
     */
    public BlockContainer getBlockContainer()
    {
        return this.blockContainer;
    }

    /**
     * @return the BlockEntity map for this region
     */
    public Map<BlockPos, CompoundData> getBlockEntityMap()
    {
        return this.blockEntityData;
    }

    /**
     * @return the entity list for this region
     */
    public List<EntityData> getEntityList()
    {
        return this.entityData;
    }

    /**
     * @return the map of scheduled block tick data in this region
     */
    public Map<BlockPos, ScheduledBlockTickData> getBlockTickMap()
    {
        return this.blockTickData;
    }
}
