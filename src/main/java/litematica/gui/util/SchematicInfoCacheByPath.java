package litematica.gui.util;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.NBTTagCompound;

import malilib.util.FileNameUtils;
import malilib.util.data.Identifier;
import malilib.util.nbt.NbtUtils;
import litematica.Reference;
import litematica.schematic.LoadedSchematic;
import litematica.schematic.Schematic;
import litematica.schematic.SchematicMetadata;

public class SchematicInfoCacheByPath extends AbstractSchematicInfoCache<Path>
{
    @Override
    @Nullable
    protected SchematicInfo createSchematicInfo(Path file)
    {
        // TODO Use a partial NBT read method to only read the metadata tag
        // TODO (that's only beneficial if it's stored before the bulk schematic data in the stream)
        NBTTagCompound tag = NbtUtils.readNbtFromFile(file);
        // FIXME

        if (tag != null)
        {
            Optional<LoadedSchematic> loadedSchematicOpt = LoadedSchematic.tryLoadSchematic(file);

            if (loadedSchematicOpt.isPresent())
            {
                Schematic schematic = loadedSchematicOpt.get().schematic;
                SchematicMetadata metadata = schematic.getMetadata();
                String filePath = FileNameUtils.generateSimpleSafeFileName(file.toAbsolutePath().toString().toLowerCase(Locale.ROOT));
                Identifier iconName = new Identifier(Reference.MOD_ID, filePath);
                DynamicTexture texture = this.createPreviewImage(iconName, metadata);
                return new SchematicInfo(metadata, iconName, texture);
            }
        }

        return null;
    }
}
