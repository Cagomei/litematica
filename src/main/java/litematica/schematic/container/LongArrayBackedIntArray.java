package litematica.schematic.container;

import javax.annotation.Nullable;

import malilib.util.MathUtils;

public abstract class LongArrayBackedIntArray implements PackedIntArray
{
    /** The long array that is used to store the data for this BitArray. */
    protected final long[] longArray;
    /** Number of bits a single entry takes up */
    protected final int bitsPerEntry;
    /**
     * The maximum value for a single entry. This also works as a bitmask for a single entry.
     * For instance, if bitsPerEntry were 5, this value would be 31 (ie, {@code 0b00011111}).
     */
    protected final int maxEntryValue;
    /** Number of entries in this bit array (<b>not</b> the length of the long array that backs the bit array) */
    protected final long arraySize;

    public LongArrayBackedIntArray(int bitsPerEntry, long arraySize) throws IndexOutOfBoundsException
    {
        this(bitsPerEntry, arraySize, null);
    }

    public LongArrayBackedIntArray(int bitsPerEntry, long arraySize, @Nullable long[] array) throws IndexOutOfBoundsException
    {
        if (bitsPerEntry < 0 || bitsPerEntry > 31)
        {
            throw new IndexOutOfBoundsException("Invalid bitsPerEntry value: " + bitsPerEntry);
        }

        bitsPerEntry = Math.max(1, Math.min(31, bitsPerEntry));

        this.arraySize = arraySize;
        this.bitsPerEntry = bitsPerEntry;
        this.maxEntryValue = (1 << bitsPerEntry) - 1;
        long requiredArrayLengthLong = MathUtils.roundUp(arraySize * bitsPerEntry, 64L) / 64L;

        if (requiredArrayLengthLong > Integer.MAX_VALUE)
        {
            throw new IndexOutOfBoundsException("Required array length (" + requiredArrayLengthLong + ") is larger than max int value");
        }

        int requiredArrayLength = (int) requiredArrayLengthLong;

        if (array != null)
        {
            if (array.length < requiredArrayLength)
            {
                throw new IndexOutOfBoundsException("Provided array length (" + array.length + ") is less than the required length" + requiredArrayLength);
            }

            this.longArray = array;
        }
        else
        {
            this.longArray = new long[requiredArrayLength];
        }
    }

    @Override
    public long size()
    {
        return this.arraySize;
    }

    @Override
    public int getEntryBitWidth()
    {
        return this.bitsPerEntry;
    }

    @Override
    public long[] getValueCounts()
    {
        long[] counts = new long[(int) this.maxEntryValue + 1];

        for (long i = 0; i < this.arraySize; ++i)
        {
            ++counts[this.getAt(i)];
        }

        return counts;
    }

    public long[] getBackingLongArray()
    {
        return this.longArray;
    }
}
