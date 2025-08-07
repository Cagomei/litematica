package litematica.gui;

import malilib.gui.BaseScreen;
import litematica.gui.widget.SchematicInfoWidgetBySchematic;
import litematica.schematic.LoadedSchematic;

public class SchematicInfoPopupScreen extends BaseScreen
{
    protected final SchematicInfoWidgetBySchematic schematicInfoWidget;

    public SchematicInfoPopupScreen(LoadedSchematic loadedSchematic, int height)
    {
        this.backgroundColor = 0xFF000000;
        this.renderBorder = true;
        this.useTitleHierarchy = false;

        this.schematicInfoWidget = new SchematicInfoWidgetBySchematic(190, height - 30);
        this.schematicInfoWidget.setActiveEntry(loadedSchematic);

        Runnable clearTask = this.schematicInfoWidget::clearCache;
        this.addPreInitListener(clearTask);
        this.addPreScreenCloseListener(clearTask);

        this.setTitle("litematica.title.screen.schematic_info_popup");
        this.setScreenWidthAndHeight(200, height);
        this.centerOnScreen();
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();
        this.addWidget(this.schematicInfoWidget);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.schematicInfoWidget.setPosition(this.x + 5, this.y + 20);
    }
}
