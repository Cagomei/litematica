package litematica.schematic;

import javax.annotation.Nullable;

import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.DataTypeUtils;
import malilib.util.data.tag.DataView;
import malilib.util.game.MinecraftVersion;
import malilib.util.position.Vec3i;

public class SchematicMetadata
{
    protected String schematicName = "?";
    protected String author = "";
    protected String description = "";
    protected Vec3i enclosingSize = Vec3i.ZERO;
    protected long timeCreated;
    protected long timeModified;
    protected MinecraftVersion minecraftVersion = MinecraftVersion.MC_UNKNOWN;
    protected int schematicVersion;
    protected int regionCount;
    protected int entityCount;
    protected int blockEntityCount;
    protected long totalVolume = -1;
    protected long totalBlocks = -1;
    @Nullable
    protected int[] thumbnailPixelData;

    public String getSchematicName()
    {
        return this.schematicName;
    }

    public String getAuthor()
    {
        return this.author;
    }

    public String getDescription()
    {
        return this.description;
    }

    @Nullable
    public int[] getPreviewImagePixelData()
    {
        return this.thumbnailPixelData;
    }

    public int getRegionCount()
    {
        return this.regionCount;
    }

    public long getTotalVolume()
    {
        return this.totalVolume;
    }

    public long getTotalBlocks()
    {
        return this.totalBlocks;
    }

    public int getEntityCount()
    {
        return this.entityCount;
    }

    public int getBlockEntityCount()
    {
        return this.blockEntityCount;
    }

    public Vec3i getEnclosingSize()
    {
        return this.enclosingSize;
    }

    public long getTimeCreated()
    {
        return this.timeCreated;
    }

    public long getTimeModified()
    {
        return this.timeModified;
    }

    public int getSchematicVersion()
    {
        return this.schematicVersion;
    }

    public MinecraftVersion getMinecraftVersion()
    {
        return this.minecraftVersion;
    }

    public boolean wasModified()
    {
        return this.timeCreated != this.timeModified;
    }

    public void setSchematicName(String schematicName)
    {
        this.schematicName = schematicName;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setPreviewImagePixelData(@Nullable int[] pixelData)
    {
        this.thumbnailPixelData = pixelData;
    }

    public void setRegionCount(int regionCount)
    {
        this.regionCount = regionCount;
    }

    public void setEntityCount(int entityCount)
    {
        this.entityCount = entityCount;
    }

    public void setBlockEntityCount(int blockEntityCount)
    {
        this.blockEntityCount = blockEntityCount;
    }

    public void setTotalVolume(long totalVolume)
    {
        this.totalVolume = totalVolume;
    }

    public void setTotalBlocks(long totalBlocks)
    {
        this.totalBlocks = totalBlocks;
    }

    public void setEnclosingSize(Vec3i enclosingSize)
    {
        this.enclosingSize = enclosingSize;
    }

    public void setTimeCreated(long timeCreated)
    {
        this.timeCreated = timeCreated;
    }

    public void setTimeModified(long timeModified)
    {
        this.timeModified = timeModified;
    }

    public void setTimeModifiedToNow()
    {
        this.timeModified = System.currentTimeMillis();
    }

    public void setTimeModifiedToNowIfNotRecentlyCreated()
    {
        long currentTime = System.currentTimeMillis();

        // Allow 10 minutes to set the description and thumbnail image etc.
        // without marking the schematic as modified
        if (currentTime - this.timeCreated > 10L * 60L * 1000L)
        {
            this.timeModified = currentTime;
        }
    }

    public void setSchematicVersion(int schematicVersion)
    {
        this.schematicVersion = schematicVersion;
    }

    public void setMinecraftVersion(MinecraftVersion minecraftVersion)
    {
        this.minecraftVersion = minecraftVersion;
    }

    public void copyFrom(SchematicMetadata other)
    {
        this.schematicName = other.schematicName;
        this.author = other.author;
        this.description = other.description;
        this.enclosingSize = other.enclosingSize;
        this.timeCreated = other.timeCreated;
        this.timeModified = other.timeModified;
        this.regionCount = other.regionCount;
        this.totalVolume = other.totalVolume;
        this.totalBlocks = other.totalBlocks;

        if (other.thumbnailPixelData != null)
        {
            this.thumbnailPixelData = new int[other.thumbnailPixelData.length];
            System.arraycopy(other.thumbnailPixelData, 0, this.thumbnailPixelData, 0, this.thumbnailPixelData.length);
        }
        else
        {
            this.thumbnailPixelData = null;
        }
    }

    public CompoundData write()
    {
        CompoundData tag = new CompoundData();

        tag.putString("Name", this.schematicName);
        tag.putString("Author", this.author);
        tag.putString("Description", this.description);
        tag.put("EnclosingSize", DataTypeUtils.createVec3iTag(this.enclosingSize));

        tag.putLong("TimeCreated", this.timeCreated);
        tag.putLong("TimeModified", this.timeModified);
        tag.putInt("SchematicVersion", this.schematicVersion);

        if (this.minecraftVersion != null)
        {
            tag.putString("McVersion", this.minecraftVersion.displayName);
            tag.putInt("McDataVersion", this.minecraftVersion.dataVersion);
            tag.putInt("McProtocolVersion", this.minecraftVersion.protocolVersion);
        }

        tag.putInt("RegionCount", this.regionCount);
        tag.putInt("EntityCount", this.entityCount);
        tag.putInt("BlockEntityCount", this.blockEntityCount);
        tag.putLong("TotalVolume", this.totalVolume);
        tag.putLong("TotalBlocks", this.totalBlocks);

        if (this.thumbnailPixelData != null)
        {
            tag.putIntArray("PreviewImageData", this.thumbnailPixelData);
        }

        return tag;
    }

    public void read(DataView dataIn)
    {
        this.schematicName = dataIn.getStringOrDefault("Name", "?");
        this.author = dataIn.getStringOrDefault("Author", "?");
        this.description = dataIn.getStringOrDefault("Description", "");
        this.enclosingSize = DataTypeUtils.readVec3iOrDefault(dataIn, "EnclosingSize", Vec3i.ZERO);

        this.timeCreated = dataIn.getLongOrDefault("TimeCreated", -1);
        this.timeModified = dataIn.getLongOrDefault("TimeModified", -1);
        this.schematicVersion = dataIn.getIntOrDefault("SchematicVersion", -1);

        int mcDataVersion = dataIn.getIntOrDefault("McDataVersion", -1);
        this.minecraftVersion = MinecraftVersion.getVersionOrCreateSnapshotVersionByDataVersion(mcDataVersion);

        this.regionCount = dataIn.getIntOrDefault("RegionCount", -1);
        this.entityCount = dataIn.getIntOrDefault("EntityCount", -1);
        this.blockEntityCount = dataIn.getIntOrDefault("BlockEntityCount", -1);
        this.totalVolume = dataIn.getLongOrDefault("TotalVolume", -1);
        this.totalBlocks = dataIn.getLongOrDefault("TotalBlocks", -1);

        this.thumbnailPixelData = dataIn.getIntArrayOrDefault("PreviewImageData", null);
    }
}
