package litematica.gui.util;

import java.util.Locale;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.texture.DynamicTexture;

import malilib.util.FileNameUtils;
import malilib.util.data.Identifier;
import litematica.Reference;
import litematica.data.LoadedSchematic;
import litematica.schematic.SchematicMetadata;

public class SchematicInfoCacheBySchematic extends AbstractSchematicInfoCache<LoadedSchematic>
{
    @Override
    @Nullable
    protected SchematicInfo createSchematicInfo(LoadedSchematic loadedSchematic)
    {
        SchematicMetadata metadata = loadedSchematic.schematic.getMetadata();
        String name;

        if (loadedSchematic.file.isPresent())
        {
            name = FileNameUtils.generateSimpleSafeFileName(loadedSchematic.file.get().toAbsolutePath().toString().toLowerCase(Locale.ROOT));
        }
        else
        {
            name = FileNameUtils.generateSimpleSafeFileName(metadata.getSchematicName() + "_" + metadata.getAuthor() + "_" + metadata.getTimeCreated());
        }

        Identifier iconName = new Identifier(Reference.MOD_ID, name);
        DynamicTexture texture = this.createPreviewImage(iconName, metadata);
        return new SchematicInfo(metadata, iconName, texture);
    }
}
