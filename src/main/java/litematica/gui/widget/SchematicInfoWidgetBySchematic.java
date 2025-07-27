package litematica.gui.widget;

import litematica.data.LoadedSchematic;
import litematica.gui.util.SchematicInfoCacheBySchematic;

public class SchematicInfoWidgetBySchematic extends AbstractSchematicInfoWidget<LoadedSchematic>
{
    public SchematicInfoWidgetBySchematic(int width, int height)
    {
        super(width, height, new SchematicInfoCacheBySchematic());
    }
}
