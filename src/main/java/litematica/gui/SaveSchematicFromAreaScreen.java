package litematica.gui;

import java.nio.file.Path;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;

import malilib.config.option.BooleanConfig;
import malilib.config.option.OptionListConfig;
import malilib.config.value.BaseOptionListConfigValue;
import malilib.gui.widget.BooleanEditWidget;
import malilib.gui.widget.DropDownListWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.BooleanConfigButton;
import malilib.gui.widget.button.OnOffButton;
import malilib.gui.widget.button.OptionListConfigButton;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.game.wrap.GameWrap;
import litematica.config.Configs;
import litematica.data.SchematicHolder;
import litematica.scheduler.TaskScheduler;
import litematica.scheduler.task.LocalCreateSchematicTask;
import litematica.schematic.LoadedSchematic;
import litematica.schematic.Schematic;
import litematica.schematic.SchematicSaveSettings;
import litematica.schematic.SchematicType;
import litematica.schematic.util.SchematicFileUtils;
import litematica.selection.AreaSelection;
import litematica.util.value.SchematicSaveWorldSelection;

public class SaveSchematicFromAreaScreen extends BaseSaveSchematicScreen
{
    protected final AreaSelection selection;
    protected final SchematicSaveSettings settings;
    protected final OptionListConfig<SaveSide> saveSide;
    protected final BooleanConfig customSettingsEnabled = new BooleanConfig("customSettings", false);
    protected final BooleanConfigButton customSettingsButton;
    protected final OptionListConfigButton saveSideButton;
    protected final LabelWidget worldSelectionLabel;
    protected final DropDownListWidget<SchematicSaveWorldSelection> worldSelectionDropdown;
    protected final BooleanEditWidget saveBlocksWidget;
    protected final BooleanEditWidget saveBlockEntitiesWidget;
    protected final BooleanEditWidget saveBlockTicksWidget;
    protected final BooleanEditWidget saveEntitiesWidget;
    protected final BooleanEditWidget exposedBlocksOnlyWidget;

    public SaveSchematicFromAreaScreen(AreaSelection selection)
    {
        super(10, 74, 20 + 170 + 2, 80, "save_schematic_from_area");

        this.selection = selection;
        this.settings = new SchematicSaveSettings();
        this.settings.tryLoad(Configs.Internal.SCHEMATIC_SAVE_SETTINGS.getValue());

        String areaName = selection.getName();
        this.originalName = getFileNameFromDisplayName(areaName);
        this.fileNameTextField.setText(this.originalName);

        SaveSide side = Configs.Internal.SAVE_SIDE.getValue();
        this.saveSide = new OptionListConfig<>("-", side, SaveSide.VALUES);
        this.customSettingsEnabled.setBooleanValue(Configs.Internal.SAVE_WITH_CUSTOM_SETTINGS.getBooleanValue());

        this.customSettingsButton = new BooleanConfigButton(-1, 18, this.customSettingsEnabled, OnOffButton.OnOffStyle.TEXT_ON_OFF, "litematica.button.schematic_save.custom_settings");
        this.saveSideButton = new OptionListConfigButton(-1, 16, this.saveSide, "litematica.button.schematic_save.save_side");

        this.saveBlocksWidget         = new BooleanEditWidget(14, this.settings.saveBlocks,              "litematica.button.schematic_save.save_blocks");
        this.saveBlockEntitiesWidget  = new BooleanEditWidget(14, this.settings.saveBlockEntities,       "litematica.button.schematic_save.save_block_entities");
        this.saveBlockTicksWidget     = new BooleanEditWidget(14, this.settings.saveScheduledBlockTicks, "litematica.button.schematic_save.save_block_ticks");
        this.saveEntitiesWidget       = new BooleanEditWidget(14, this.settings.saveEntities,            "litematica.button.schematic_save.save_entities");
        this.exposedBlocksOnlyWidget  = new BooleanEditWidget(14, this.settings.exposedBlocksOnly,       "litematica.button.schematic_save.exposed_blocks_only");

        this.worldSelectionLabel = new LabelWidget("litematica.gui.label.schematic_save.from_world");
        this.worldSelectionDropdown = new DropDownListWidget<>(14, 10, SchematicSaveWorldSelection.VALUES, SchematicSaveWorldSelection::getDisplayName);
        this.worldSelectionDropdown.setSelectedEntry(SchematicSaveWorldSelection.VANILLA_ONLY);
        this.worldSelectionDropdown.setMaxWidth(180); // TODO the dropdown widget hover overflow render does not account for going over the screen edge

        this.customSettingsEnabled.setValueChangeCallback((n, o) -> this.onCustomSettingsToggled());

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

        this.addPreScreenCloseListener(this::saveSettings);
        this.setTitle("litematica.title.screen.save_schematic_from_area", areaName);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        //this.addWidget(this.schematicTypeDropdown);
        this.addWidget(this.saveSideButton);
        this.addWidget(this.customSettingsButton);
        this.addWidget(this.schematicTypeDropdown);
        this.addWidget(this.saveButton);

        if (this.customSettingsEnabled.getBooleanValue())
        {
            this.addWidget(this.saveBlocksWidget);
            this.addWidget(this.saveBlockEntitiesWidget);
            this.addWidget(this.saveBlockTicksWidget);
            this.addWidget(this.saveEntitiesWidget);
            this.addWidget(this.exposedBlocksOnlyWidget);
            this.addWidget(this.worldSelectionLabel);
            this.addWidget(this.worldSelectionDropdown);
        }
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.schematicTypeDropdown.setPosition(this.fileNameTextField.getX(), this.fileNameTextField.getBottom() + 2);
        this.saveButton.setPosition(this.schematicTypeDropdown.getRight() + 2, this.fileNameTextField.getBottom() + 2);
        //this.saveButton.setPosition(this.fileNameTextField.getX(), this.fileNameTextField.getBottom() + 2);

        int x = this.schematicInfoWidget.getX();
        this.saveSideButton.setPosition(x, this.y + 10);
        this.customSettingsButton.setPosition(x, this.saveSideButton.getBottom() + 1);

        if (this.customSettingsEnabled.getBooleanValue())
        {
            int gap = 1;
            this.saveBlocksWidget.setPosition(x, this.customSettingsButton.getBottom() + gap);
            this.saveBlockEntitiesWidget.setPosition(x, this.saveBlocksWidget.getBottom() + gap);
            this.saveBlockTicksWidget.setPosition(x, this.saveBlockEntitiesWidget.getBottom() + gap);
            this.saveEntitiesWidget.setPosition(x, this.saveBlockTicksWidget.getBottom() + gap);
            this.exposedBlocksOnlyWidget.setPosition(x, this.saveEntitiesWidget.getBottom() + gap);
            this.worldSelectionLabel.setPosition(x, this.exposedBlocksOnlyWidget.getBottom() + 3);
            this.worldSelectionDropdown.setPosition(x, this.worldSelectionLabel.getBottom());
            this.schematicInfoWidget.setY(this.worldSelectionDropdown.getBottom() + 4);
            this.schematicInfoWidget.setHeight(this.getListHeight() - (this.schematicInfoWidget.getY() - this.getListY()));
        }
        else
        {
            this.schematicInfoWidget.setY(this.getListY());
            this.schematicInfoWidget.setHeight(this.getListHeight());
        }
    }

    @Override
    protected void saveSchematic()
    {
        boolean overwrite = isShiftDown();
        Path file = this.getSchematicFileIfCanSave(overwrite);
        SchematicSaveSettings effectiveSettings = this.getSaveSettings();

        if (file == null || effectiveSettings == null)
        {
            return;
        }

        if (shouldSaveOnDedicatedServerSide(this.saveSide.getValue()))
        {
            this.saveSchematicOnServer(effectiveSettings, file, overwrite);
        }
        else
        {
            this.saveSchematicOnClient(effectiveSettings, file, overwrite);
        }
    }

    public static boolean shouldSaveOnDedicatedServerSide(SaveSide side)
    {
        if (GameWrap.isSinglePlayer())
        {
            return false;
        }

        boolean supportsServerSideSaving = false; // TODO

        return side == SaveSide.SERVER || (side == SaveSide.AUTO && supportsServerSideSaving);
    }

    @Nullable
    protected SchematicSaveSettings getSaveSettings()
    {
        SchematicType schematicType = this.schematicTypeDropdown.getSelectedEntry();

        if (schematicType == null)
        {
            return null;
        }

        SchematicSaveSettings effectiveSettings;

        if (this.customSettingsEnabled.getBooleanValue())
        {
            effectiveSettings = this.settings.copy();
            effectiveSettings.worldSelection.setValue(this.worldSelectionDropdown.getSelectedEntry());
        }
        else
        {
            effectiveSettings = new SchematicSaveSettings();
        }

        effectiveSettings.schematicType = schematicType;

        return effectiveSettings;
    }

    protected void saveSchematicOnClient(SchematicSaveSettings settings, Path file, boolean overwrite)
    {
        LocalCreateSchematicTask task = new LocalCreateSchematicTask(this.selection, settings,
                                                                     sch -> this.writeSchematicToFile(sch, file, overwrite));

        TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 10);
    }

    protected void saveSchematicOnServer(SchematicSaveSettings settings, Path file, boolean overwrite)
    {
        /*
        SchematicSavePacketHandler.INSTANCE.requestSchematicSaveAllAtOnce(this.selection, settings,
                                                                          sch -> this.writeSchematicToFile(sch, file, overwrite));
        */
    }

    protected void writeSchematicToFile(Schematic schematic, Path file, boolean overwrite)
    {
        if (SchematicFileUtils.writeToFile(schematic, file, overwrite))
        {
            this.onSchematicSaved(file);
        }
        else
        {
            LoadedSchematic loadedSchematic = new LoadedSchematic(schematic);
            SchematicHolder.INSTANCE.addSchematic(loadedSchematic, false);
            MessageDispatcher.error(8000).translate("litematica.message.error.save_schematic.failed_to_save_from_area",
                                                    file.getFileName().toString());
        }
    }

    protected void onSchematicSaved(Path file)
    {
        this.onSchematicChange();
        MessageDispatcher.success("litematica.message.success.save_schematic_new", file.getFileName().toString());
    }

    protected void onCustomSettingsToggled()
    {
        this.reAddActiveWidgets();
        this.updateWidgetPositions();
    }

    protected void saveSettings()
    {
        Configs.Internal.SAVE_SIDE.setValue(this.saveSide.getValue());
        Configs.Internal.SAVE_WITH_CUSTOM_SETTINGS.setBooleanValue(this.customSettingsEnabled.getBooleanValue());
        // TODO save the actual save settings
    }

    public static class SaveSide extends BaseOptionListConfigValue
    {
        public static final SaveSide AUTO   = new SaveSide("auto",   "litematica.name.save_side.auto");
        public static final SaveSide CLIENT = new SaveSide("client", "litematica.name.save_side.client");
        public static final SaveSide SERVER = new SaveSide("server", "litematica.name.save_side.server");

        public static final ImmutableList<SaveSide> VALUES = ImmutableList.of(AUTO, CLIENT, SERVER);

        public SaveSide(String name, String translationKey)
        {
            super(name, translationKey);
        }
    }
}
