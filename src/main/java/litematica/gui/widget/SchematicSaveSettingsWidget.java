package litematica.gui.widget;

import malilib.gui.widget.BooleanEditWidget;
import malilib.gui.widget.ContainerWidget;
import malilib.gui.widget.DropDownListWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.OptionListConfigButton;
import malilib.util.game.wrap.GameWrap;
import litematica.schematic.SchematicSaveSettings;
import litematica.schematic.SchematicType;
import litematica.util.value.SchematicSaveWorldSelection;

public class SchematicSaveSettingsWidget extends ContainerWidget
{
    protected final SchematicSaveSettings settings;

    protected final OptionListConfigButton saveSideButton;
    protected final BooleanEditWidget saveBlocksWidget;
    protected final BooleanEditWidget saveBlockEntitiesWidget;
    protected final BooleanEditWidget saveBlockTicksWidget;
    protected final BooleanEditWidget saveEntitiesWidget;
    protected final BooleanEditWidget exposedBlocksOnlyWidget;
    protected final LabelWidget worldSelectionLabel;
    protected final DropDownListWidget<SchematicSaveWorldSelection> worldSelectionDropdown;
    protected boolean clientOnlyWarningsEnabled;

    public SchematicSaveSettingsWidget(int width, int height, SchematicSaveSettings settings)
    {
        super(width, height);

        this.settings = settings;

        this.saveSideButton = new OptionListConfigButton(-1, 16, this.settings.saveSide, "litematica.button.schematic_save.save_side");
        this.saveBlocksWidget         = new BooleanEditWidget(14, this.settings.saveBlocks,              "litematica.button.schematic_save.save_blocks");
        this.saveBlockEntitiesWidget  = new BooleanEditWidget(14, this.settings.saveBlockEntities,       "litematica.button.schematic_save.save_block_entities");
        this.saveBlockTicksWidget     = new BooleanEditWidget(14, this.settings.saveScheduledBlockTicks, "litematica.button.schematic_save.save_block_ticks");
        this.saveEntitiesWidget       = new BooleanEditWidget(14, this.settings.saveEntities,            "litematica.button.schematic_save.save_entities");
        this.exposedBlocksOnlyWidget  = new BooleanEditWidget(14, this.settings.exposedBlocksOnly,       "litematica.button.schematic_save.exposed_blocks_only");

        this.worldSelectionLabel = new LabelWidget("litematica.gui.label.schematic_save.from_world");
        this.worldSelectionDropdown = new DropDownListWidget<>(14, 10, SchematicSaveWorldSelection.VALUES, SchematicSaveWorldSelection::getDisplayName);
        this.worldSelectionDropdown.setSelectedEntry(settings.worldSelection.getValue());
        this.worldSelectionDropdown.setSelectionListener(this.settings.worldSelection::setValue);
        this.worldSelectionDropdown.setMaxWidth(180); // TODO the dropdown widget hover overflow render does not account for going over the screen edge
        this.worldSelectionDropdown.setHoverInfoRequiresShift(true);
        this.worldSelectionDropdown.translateAndAddHoverString("litematica.hover.schematic_save_settings.world_selection");

        String hoverKey;

        if (GameWrap.isSinglePlayer())
        {
            hoverKey = "litematica.hover.button.schematic_save.save_side.single_player";
            this.saveSideButton.getHoverInfoFactory().removeAll();
            this.saveSideButton.setEnabled(false);
        }
        else
        {
            hoverKey = "litematica.hover.button.schematic_save.save_side.info";
        }

        this.saveSideButton.setHoverInfoRequiresShift(true);
        this.saveSideButton.translateAndAddHoverString(hoverKey);
        this.saveBlockTicksWidget.setShowAsOffIfDisabled(true);

        this.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, 0xC0000000);
        this.getBorderRenderer().getNormalSettings().setBorderWidthAndColor(1, 0xFFC0C0C0);
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        this.addWidget(this.saveSideButton);
        this.addWidget(this.worldSelectionLabel);
        this.addWidget(this.worldSelectionDropdown);

        this.addWidget(this.saveBlocksWidget);
        this.addWidget(this.saveBlockEntitiesWidget);
        this.addWidget(this.saveBlockTicksWidget);
        this.addWidget(this.saveEntitiesWidget);
        this.addWidget(this.exposedBlocksOnlyWidget);

        // Apply to sub-widgets after their sub-widgets have been added first
        this.setClientOnlyWarnings(this.clientOnlyWarningsEnabled);
    }

    @Override
    public void updateSubWidgetPositions()
    {
        super.updateSubWidgetPositions();

        int x = this.getX() + 5;
        int y = this.getY() + 5;
        int gap = 1;

        this.saveSideButton.setPosition(x, y);
        this.worldSelectionLabel.setPosition(x, this.saveSideButton.getBottom() + 3);
        this.worldSelectionDropdown.setPosition(x, this.worldSelectionLabel.getBottom());

        this.saveBlocksWidget.setPosition(x, this.worldSelectionDropdown.getBottom() + 3);
        this.saveBlockEntitiesWidget.setPosition(x, this.saveBlocksWidget.getBottom() + gap);
        this.saveBlockTicksWidget.setPosition(x, this.saveBlockEntitiesWidget.getBottom() + gap);
        this.saveEntitiesWidget.setPosition(x, this.saveBlockTicksWidget.getBottom() + gap);
        this.exposedBlocksOnlyWidget.setPosition(x, this.saveEntitiesWidget.getBottom() + gap);
    }

    protected void onCustomSettingsToggled()
    {
        this.reAddSubWidgets();
        this.updateSubWidgetPositions();
    }

    public SchematicSaveSettings getSaveSettings(SchematicType schematicType)
    {
        SchematicSaveSettings effectiveSettings = this.settings.copy();

        effectiveSettings.worldSelection.setValue(this.worldSelectionDropdown.getSelectedEntry());
        effectiveSettings.schematicType = schematicType;

        return effectiveSettings;
    }

    public void setClientOnlyWarnings(boolean clientOnlyWarningsEnabled)
    {
        this.clientOnlyWarningsEnabled = clientOnlyWarningsEnabled;

        int color = clientOnlyWarningsEnabled ? 0xFFFFAA00 : 0xFFFFFFFF;

        this.saveBlockTicksWidget.setEnabled(! clientOnlyWarningsEnabled);
        this.saveBlockEntitiesWidget.setNormalStateLabelColor(color);
        this.saveEntitiesWidget.setNormalStateLabelColor(color);

        // First remove possible already added hover strings
        this.saveBlockTicksWidget.getHoverInfoFactory().removeAll();
        this.saveBlockEntitiesWidget.getHoverInfoFactory().removeAll();
        this.saveEntitiesWidget.getHoverInfoFactory().removeAll();

        if (clientOnlyWarningsEnabled)
        {
            this.saveBlockTicksWidget.translateAndAddHoverString("litematica.hover.schematic_save_settings.multiplayer_warn.block_ticks");
            this.saveBlockEntitiesWidget.translateAndAddHoverString("litematica.hover.schematic_save_settings.multiplayer_warn.block_entities");
            this.saveEntitiesWidget.translateAndAddHoverString("litematica.hover.schematic_save_settings.multiplayer_warn.entities");
        }
    }
}
