package litematica.schematic.container;

import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;

import net.minecraft.network.PacketBuffer;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.palette.Palette;
import malilib.util.data.palette.PaletteResizeHandler;
import malilib.util.position.Vec3i;
import malilib.util.world.BlockState;

public class ArrayBlockContainer extends BaseBlockContainer implements PaletteResizeHandler<BlockState>
{
    protected TightLongBackedBitArray storage;
    protected boolean checkForFreedIds = true;

    public ArrayBlockContainer(Vec3i size)
    {
        this(size, true);
    }

    public ArrayBlockContainer(Vec3i size, boolean checkForFreedIds)
    {
        this(size, 2, null);

        this.checkForFreedIds = checkForFreedIds;
    }

    protected ArrayBlockContainer(Vec3i size, int entryWidthBits, @Nullable long[] backingLongArray)
    {
        super(size, entryWidthBits);

        this.setBackingArray(backingLongArray);
    }

    @Override
    protected void setEntryWidthBits(int entryWidthBits)
    {
        entryWidthBits = Math.max(2, entryWidthBits);

        if (entryWidthBits != this.entryWidthBits)
        {
            this.entryWidthBits = entryWidthBits;
            this.palette = createPalette(entryWidthBits, this);
        }
    }

    protected void setBackingArray(@Nullable long[] backingLongArray) throws IndexOutOfBoundsException
    {
        if (backingLongArray != null)
        {
            this.storage = new TightLongBackedBitArray(this.entryWidthBits, this.totalVolume, backingLongArray);
        }
        else
        {
            this.storage = new TightLongBackedBitArray(this.entryWidthBits, this.totalVolume);
        }
    }

    @Override
    public BlockState getBlockState(int x, int y, int z)
    {
        long storageIndex = this.getIndex(x, y, z);
        int valueId = this.storage.getAt(storageIndex);
        BlockState state = this.palette.getValue(valueId);
        return state == null ? AIR_BLOCK_STATE : state;
    }

    @Override
    public void setBlockState(int x, int y, int z, BlockState state)
    {
        long storageIndex = this.getIndex(x, y, z);
        int valueId = this.palette.idFor(state);
        this.storage.setAt(storageIndex, valueId);
        this.hasSetBlockCounts = false; // Force a re-count when next queried
    }

    @Override
    public int onResize(int bits, BlockState state, Palette<BlockState> oldPalette)
    {
        if (this.checkForFreedIds)
        {
            long[] counts = this.storage.getValueCounts();
            final int countsSize = counts.length;

            // Check if there are any IDs that are not in use anymore
            for (int id = 0; id < countsSize; ++id)
            {
                // Found an ID that is not in use anymore, use that instead of increasing the palette size
                if (counts[id] == 0)
                {
                    if (this.palette.overrideMapping(id, state))
                    {
                        return id;
                    }
                }
            }
        }

        TightLongBackedBitArray oldArray = this.storage;
        TightLongBackedBitArray newArray = new TightLongBackedBitArray(bits, this.totalVolume);

        // This creates the new palette with the increased size
        this.setEntryWidthBits(bits);
        // Copy over the full old palette mapping
        this.palette.setMapping(oldPalette.getMapping());

        final long size = oldArray.size();

        for (long index = 0; index < size; ++index)
        {
            newArray.setAt(index, oldArray.getAt(index));
        }

        this.storage = newArray;

        return this.palette.idFor(state);
    }

    protected long getIndex(int x, int y, int z)
    {
        return ((long) y * this.sizeLayer) + (long) z * (long) this.sizeX + (long) x;
    }

    public TightLongBackedBitArray getBitArray()
    {
        return this.storage;
    }

    public long[] getBackingLongArray()
    {
        return this.storage.getBackingLongArray();
    }

    @Override
    protected void calculateBlockCountsIfNeeded()
    {
        if (this.hasSetBlockCounts == false)
        {
            long[] counts = this.storage.getValueCounts();
            this.setBlockCounts(counts);
        }
    }

    @Override
    public BlockContainer copy()
    {
        ArrayBlockContainer newContainer = ArrayBlockContainer.of(this.size, this.entryWidthBits, this.storage.getBackingLongArray().clone());
        newContainer.palette = this.palette.copy(newContainer);

        return newContainer;
    }

    public static SpongeBlockStateConverterResults convertVarIntByteArrayToPackedLongArray(Vec3i size,
                                                                                           int entryWidthBits,
                                                                                           byte[] blockStates)
    {
        int volume = size.getX() * size.getY() * size.getZ();
        TightLongBackedBitArray bitArray = new TightLongBackedBitArray(entryWidthBits, volume);
        PacketBuffer buf = new PacketBuffer(Unpooled.wrappedBuffer(blockStates));
        long[] blockCounts = new long[1 << entryWidthBits];

        for (int i = 0; i < volume; ++i)
        {
            int id = buf.readVarInt();
            bitArray.setAt(i, id);
            ++blockCounts[id];
        }

        return new SpongeBlockStateConverterResults(bitArray.getBackingLongArray(), blockCounts);
    }

    public static ArrayBlockContainer of(Vec3i size, int entryWidthBits, @Nullable long[] backingLongArray)
    {
        try
        {
            return new ArrayBlockContainer(size, entryWidthBits, backingLongArray);
        }
        catch (Exception e)
        {
            MessageDispatcher.error("litematica.message.error.schematic_container.invalid_array_length",
                                    backingLongArray != null ? backingLongArray.length : "<null>");
        }

        return new ArrayBlockContainer(size, entryWidthBits, null);
    }

    public static int getRequiredBitWidth(int paletteSize)
    {
        return Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1));
    }

    public static ArrayBlockContainer createContainer(Vec3i size, int paletteSize)
    {
        int entryWidthBits = getRequiredBitWidth(paletteSize);
        ArrayBlockContainer container = ArrayBlockContainer.of(size, entryWidthBits, null);
        //container.palette = createPalette(bits, container);
        return container;
    }

    public static ArrayBlockContainer createContainer(Vec3i size, int paletteSize, @Nullable long[] blockStates)
    {
        int entryWidthBits = getRequiredBitWidth(paletteSize);
        ArrayBlockContainer container = ArrayBlockContainer.of(size, entryWidthBits, blockStates);
        //container.palette = createPalette(bits, container);
        return container;
    }

    public static ArrayBlockContainer createContainer(Vec3i size, int paletteSize, byte[] blockData)
    {
        int entryWidthBits = getRequiredBitWidth(paletteSize);
        SpongeBlockStateConverterResults results = convertVarIntByteArrayToPackedLongArray(size, entryWidthBits, blockData);
        ArrayBlockContainer container = ArrayBlockContainer.of(size, entryWidthBits, results.backingArray);
        //container.palette = createPalette(bits, container);
        container.setBlockCounts(results.blockCounts);
        return container;
    }

    public static class SpongeBlockStateConverterResults
    {
        public final long[] backingArray;
        public final long[] blockCounts;

        protected SpongeBlockStateConverterResults(long[] backingArray, long[] blockCounts)
        {
            this.backingArray = backingArray;
            this.blockCounts = blockCounts;
        }
    }
}
