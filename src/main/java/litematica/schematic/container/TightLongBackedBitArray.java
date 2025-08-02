package litematica.schematic.container;

import javax.annotation.Nullable;
import org.apache.commons.lang3.Validate;

import malilib.util.MathUtils;

public class TightLongBackedBitArray
{
    /** The long array that is used to store the data for this BitArray. */
    private final long[] longArray;
    /** Number of bits a single entry takes up */
    private final int bitsPerEntry;
    /**
     * The maximum value for a single entry. This also works as a bitmask for a single entry.
     * For instance, if bitsPerEntry were 5, this value would be 31 (ie, {@code 0b00011111}).
     */
    private final long maxEntryValue;
    /** Number of entries in this bit array (<b>not</b> the length of the long array that backs the bit array) */
    private final long arraySize;

    public TightLongBackedBitArray(int bitsPerEntryIn, long arraySizeIn)
    {
        this(bitsPerEntryIn, arraySizeIn, null);
    }

    public TightLongBackedBitArray(int bitsPerEntry, long arraySize, @Nullable long[] array)
    {
        Validate.inclusiveBetween(1L, 32L, bitsPerEntry);
        this.arraySize = arraySize;
        this.bitsPerEntry = bitsPerEntry;
        this.maxEntryValue = (1L << bitsPerEntry) - 1L;

        if (array != null)
        {
            this.longArray = array;
        }
        else
        {
            this.longArray = new long[(int) (MathUtils.roundUp(arraySize * bitsPerEntry, 64L) / 64L)];
        }
    }

    public void setAt(long index, int value)
    {
        long startOffset = index * (long) this.bitsPerEntry;
        int startArrIndex = (int) (startOffset >> 6); // startOffset / 64
        int endArrIndex = (int) (((index + 1L) * (long) this.bitsPerEntry - 1L) >> 6);
        int startBitOffset = (int) (startOffset & 0x3F); // startOffset % 64
        this.longArray[startArrIndex] = this.longArray[startArrIndex] & ~(this.maxEntryValue << startBitOffset) | (value & this.maxEntryValue) << startBitOffset;

        if (startArrIndex != endArrIndex)
        {
            int endOffset = 64 - startBitOffset;
            int j1 = this.bitsPerEntry - endOffset;
            this.longArray[endArrIndex] = this.longArray[endArrIndex] >>> j1 << j1 | (value & this.maxEntryValue) >> endOffset;
        }
    }

    public int getAt(long index)
    {
        long startOffset = index * this.bitsPerEntry;
        int startArrIndex = (int) (startOffset >> 6); // startOffset / 64
        int endArrIndex = (int) (((index + 1L) * this.bitsPerEntry - 1L) >> 6);
        int startBitOffset = (int) (startOffset & 0x3F); // startOffset % 64

        if (startArrIndex == endArrIndex)
        {
            return (int) (this.longArray[startArrIndex] >>> startBitOffset & this.maxEntryValue);
        }
        else
        {
            int endOffset = 64 - startBitOffset;
            return (int) ((this.longArray[startArrIndex] >>> startBitOffset | this.longArray[endArrIndex] << endOffset) & this.maxEntryValue);
        }
    }

    public long[] getValueCounts()
    {
        long[] counts = new long[(int) this.maxEntryValue + 1];
        final long size = this.arraySize;

        for (long i = 0; i < size; ++i)
        {
            ++counts[this.getAt(i)];
        }

        return counts;
    }

    public long[] getBackingLongArray()
    {
        return this.longArray;
    }

    public long size()
    {
        return this.arraySize;
    }
}
