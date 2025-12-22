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
}
