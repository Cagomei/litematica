package litematica.gui;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import net.minecraft.util.math.BlockPos;

import malilib.gui.BaseListScreen;
import malilib.gui.BaseScreen;
import malilib.gui.ChooseActionScreen;
import malilib.gui.TextInputScreen;
import malilib.gui.icon.DefaultIcons;
import malilib.gui.icon.Icon;
import malilib.gui.widget.BaseTextFieldWidget;
import malilib.gui.widget.BlockPosEditWidget;
import malilib.gui.widget.CheckBoxWidget;
import malilib.gui.widget.ColorIndicatorWidget;
import malilib.gui.widget.IconWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.button.OnOffButton;
import malilib.gui.widget.list.DataListWidget;
import malilib.listener.EventListener;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.StringUtils;
import malilib.util.data.json.JsonUtils;
import malilib.util.position.Coordinate;
import malilib.util.position.PositionUtils;
import litematica.Reference;
import litematica.data.DataManager;
import litematica.gui.util.LitematicaIcons;
import litematica.gui.widget.list.entry.SchematicPlacementSubRegionEntryWidget;
import litematica.materials.MaterialListPlacement;
import litematica.render.OverlayRenderer;
import litematica.schematic.ISchematic;
import litematica.schematic.SchematicType;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.placement.SchematicPlacementManager;
import litematica.schematic.placement.SubRegionPlacement;
import litematica.schematic.verifier.SchematicVerifier;
import litematica.schematic.verifier.SchematicVerifierManager;

public class SchematicPlacementSettingsScreen extends BaseListScreen<DataListWidget<SubRegionPlacement>>
{
    protected final SchematicPlacement placement;
    protected final SchematicPlacementManager manager;

    protected final LabelWidget originLabel;
    protected final LabelWidget schematicNameLabel;
    protected final LabelWidget subRegionsLabel;
    protected final IconWidget schematicTypeIcon;
    protected final BaseTextFieldWidget nameTextField;
    protected final GenericButton changeSchematicButton;
    protected final GenericButton copyPasteSettingsButton;
    protected final GenericButton mirrorButton;
    protected final GenericButton nameResetButton;
    protected final GenericButton openMaterialListButton;
    protected final GenericButton openPlacementListButton;
    protected final GenericButton openVerifierButton;
    protected final GenericButton resetSubRegionsButton;
    protected final GenericButton rotateButton;
    protected final GenericButton toggleAllRegionsOffButton;
    protected final GenericButton toggleAllRegionsOnButton;
    protected final GenericButton toggleEnclosingBoxButton;
    protected final OnOffButton gridSettingsButton;
    protected final OnOffButton toggleEntitiesButton;
    protected final OnOffButton toggleLockedButton;
    protected final OnOffButton togglePlacementEnabledButton;
    protected final CheckBoxWidget lockXCoordCheckbox;
    protected final CheckBoxWidget lockYCoordCheckbox;
    protected final CheckBoxWidget lockZCoordCheckbox;
    protected final BlockPosEditWidget originEditWidget;
    protected final ColorIndicatorWidget bbColorWidget;

    public SchematicPlacementSettingsScreen(SchematicPlacement placement)
    {
        super(10, 82, 150, 104);

        this.placement = placement;
        this.manager = DataManager.getSchematicPlacementManager();

        this.nameTextField = new BaseTextFieldWidget(300, 14, placement.getName());
        this.nameTextField.setListener(this::setName);
        this.nameTextField.setUpdateListenerAlways(false);
        this.originLabel = new LabelWidget("litematica.label.schematic_placement_settings.placement_origin");
        this.subRegionsLabel = new LabelWidget();

        this.schematicNameLabel = new LabelWidget();

        this.changeSchematicButton     = GenericButton.create(14, "litematica.button.schematic_placement_settings.change_schematic", this::changeSchematicButtonClicked);
        this.copyPasteSettingsButton   = GenericButton.create(18, "litematica.button.schematic_placement_settings.export_import_settings", this::clickCopyPasteSettings);
        this.mirrorButton              = GenericButton.create(18, this::getMirrorButtonLabel, this::mirror);
        this.nameResetButton           = GenericButton.create(DefaultIcons.RESET_12, this::resetName);
        this.openMaterialListButton    = GenericButton.create(18, "litematica.button.misc.material_list", this::openMaterialList);
        this.openPlacementListButton   = GenericButton.create(18, "litematica.button.change_menu.schematic_placements", this::openPlacementList);
        this.openVerifierButton        = GenericButton.create(18, "litematica.button.misc.schematic_verifier", this::openVerifier);
        this.resetSubRegionsButton     = GenericButton.create(18, "litematica.button.schematic_placement_settings.reset_sub_regions", this::resetSubRegions);
        this.rotateButton              = GenericButton.create(18, this::getRotateButtonLabel, this::rotate);
        this.toggleAllRegionsOffButton = GenericButton.create(18, "litematica.button.schematic_placement_settings.toggle_all_off", this::toggleAllRegionsOff);
        this.toggleAllRegionsOnButton  = GenericButton.create(18, "litematica.button.schematic_placement_settings.toggle_all_on", this::toggleAllRegionsOn);
        this.toggleEnclosingBoxButton  = GenericButton.create(this::getEnclosingBoxButtonIcon, this::toggleEnclosingBoxRendering);

        this.gridSettingsButton   = OnOffButton.onOff(18, "litematica.button.schematic_placement_settings.grid_settings",
                                                      this.placement.getGridSettings()::isEnabled, this::clickGridSettings);
        this.toggleEntitiesButton = OnOffButton.onOff(18, "litematica.button.schematic_placement_settings.ignore_entities",
                                                      this.placement::ignoreEntities, this::toggleIgnoreEntities);
        this.toggleLockedButton   = OnOffButton.onOff(18, "litematica.button.schematic_placement_settings.locked",
                                                      this.placement::isLocked, this.placement::toggleLocked);
        this.togglePlacementEnabledButton = OnOffButton.onOff(18, "litematica.button.schematic_placement_settings.enabled",
                                                              this.placement::isEnabled, this::toggleEnabled);
        this.originEditWidget = new BlockPosEditWidget(90, 72, 2, true, placement.getPosition(), this::setOrigin);
        this.originEditWidget.setUpdateImmediateFromTextInput(true);

        this.lockXCoordCheckbox = new CheckBoxWidget(null, "litematica.hover.checkmark.schematic_placement_settings.lock_coordinate");
        this.lockYCoordCheckbox = new CheckBoxWidget(null, "litematica.hover.checkmark.schematic_placement_settings.lock_coordinate");
        this.lockZCoordCheckbox = new CheckBoxWidget(null, "litematica.hover.checkmark.schematic_placement_settings.lock_coordinate");
        this.bbColorWidget = new ColorIndicatorWidget(16, 16, () -> this.placement.getBoundingBoxColor().intValue, this::setBoundingBoxColor);

        SchematicType<?> type = placement.getSchematic().getType();
        Icon icon = placement.isSchematicInMemoryOnly() ? type.getInMemoryIcon() : type.getIcon();
        this.schematicTypeIcon = new IconWidget(icon);

        this.copyPasteSettingsButton.setRenderButtonBackgroundTexture(true);
        this.changeSchematicButton.translateAndAddHoverString("litematica.hover.button.schematic_placement_settings.change_schematic");
        this.copyPasteSettingsButton.translateAndAddHoverString("litematica.hover.button.schematic_placement_settings.copy_paste_settings");
        this.gridSettingsButton.translateAndAddHoverString("litematica.hover.button.schematic_placement_settings.grid_settings");
        this.nameTextField.translateAndAddHoverString("litematica.hover.schematic_placement_settings.rename_placement");
        this.nameResetButton.translateAndAddHoverString("litematica.hover.button.schematic_placement_settings.reset_name");
        this.toggleLockedButton.translateAndAddHoverString("litematica.hover.button.schematic_placement_settings.lock");
        this.bbColorWidget.translateAndAddHoverString("litematica.hover.button.schematic_placement_settings.change_bb_color");

        this.toggleEnclosingBoxButton.getHoverInfoFactory().setStringListProvider("_default", this::getEnclosingBoxButtonHoverText);
        this.resetSubRegionsButton.setEnabledStatusSupplier(() -> this.placement.isRegionPlacementModified() && this.isNotLocked());

        BooleanSupplier enabledSupplier = this::isNotLocked;
        this.mirrorButton.setEnabledStatusSupplier(enabledSupplier);
        this.rotateButton.setEnabledStatusSupplier(enabledSupplier);
        this.originEditWidget.setEnabledStatusSupplier(enabledSupplier);

        this.lockXCoordCheckbox.setBooleanStorage(() -> this.isCoordinateLocked(Coordinate.X), (val) -> this.setCoordinateLocked(val, Coordinate.X));
        this.lockYCoordCheckbox.setBooleanStorage(() -> this.isCoordinateLocked(Coordinate.Y), (val) -> this.setCoordinateLocked(val, Coordinate.Y));
        this.lockZCoordCheckbox.setBooleanStorage(() -> this.isCoordinateLocked(Coordinate.Z), (val) -> this.setCoordinateLocked(val, Coordinate.Z));

        this.setTitle("litematica.title.screen.schematic_placement_settings", Reference.MOD_VERSION);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.bbColorWidget);
        this.addWidget(this.changeSchematicButton);
        this.addWidget(this.copyPasteSettingsButton);
        this.addWidget(this.gridSettingsButton);
        this.addWidget(this.lockXCoordCheckbox);
        this.addWidget(this.lockYCoordCheckbox);
        this.addWidget(this.lockZCoordCheckbox);
        this.addWidget(this.mirrorButton);
        this.addWidget(this.nameResetButton);
        this.addWidget(this.nameTextField);
        this.addWidget(this.openMaterialListButton);
        this.addWidget(this.openPlacementListButton);
        this.addWidget(this.openVerifierButton);
        this.addWidget(this.originEditWidget);
        this.addWidget(this.originLabel);
        this.addWidget(this.resetSubRegionsButton);
        this.addWidget(this.rotateButton);
        this.addWidget(this.schematicNameLabel);
        this.addWidget(this.schematicTypeIcon);
        this.addWidget(this.subRegionsLabel);
        this.addWidget(this.toggleAllRegionsOffButton);
        this.addWidget(this.toggleAllRegionsOnButton);
        this.addWidget(this.toggleEnclosingBoxButton);
        this.addWidget(this.toggleEntitiesButton);
        this.addWidget(this.toggleLockedButton);
        this.addWidget(this.togglePlacementEnabledButton);

        this.updateLabels();
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int x = this.x + 12;
        int y = this.y + 17;
        this.schematicTypeIcon.setPosition(x, y + 2);

        this.nameTextField.setPosition(x + 16, y + 1);
        this.nameTextField.setWidth(Math.min(240, this.screenWidth - 300));
        this.nameResetButton.setX(this.nameTextField.getRight() + 4);
        this.nameResetButton.centerVerticallyInside(this.nameTextField);
        this.schematicNameLabel.setPosition(x + 2, this.nameTextField.getBottom() + 3);

        this.subRegionsLabel.setPosition(x + 1, this.getListY() - 9);

        this.changeSchematicButton.setPosition(x, this.schematicNameLabel.getBottom());

        this.copyPasteSettingsButton.setRight(this.getRight() - 140);
        this.copyPasteSettingsButton.setY(this.y + 6);
        this.gridSettingsButton.setRight(this.copyPasteSettingsButton.getRight());
        this.gridSettingsButton.setY(this.copyPasteSettingsButton.getBottom() + 1);
        this.toggleAllRegionsOffButton.setRight(this.gridSettingsButton.getRight());
        this.toggleAllRegionsOffButton.setY(this.gridSettingsButton.getBottom() + 1);
        this.toggleAllRegionsOnButton.setRight(this.toggleAllRegionsOffButton.getX() - 4);
        this.toggleAllRegionsOnButton.setY(this.toggleAllRegionsOffButton.getY());

        x = this.getRight() - 134;
        this.togglePlacementEnabledButton.setPosition(x, this.y + 6);

        this.toggleLockedButton.setPosition(x, this.togglePlacementEnabledButton.getBottom() + 1);
        this.toggleEnclosingBoxButton.setPosition(this.toggleLockedButton.getRight() + 2,
                                                  this.toggleLockedButton.getY() + 1);
        this.bbColorWidget.setPosition(this.toggleEnclosingBoxButton.getRight() + 3, this.toggleEnclosingBoxButton.getY());

        this.toggleEntitiesButton.setPosition(x, this.toggleLockedButton.getBottom() + 1);

        this.originLabel.setPosition(x + 2, this.toggleEntitiesButton.getBottom() + 4);
        this.originEditWidget.setPosition(x + 2, this.originLabel.getBottom() + 1);

        this.rotateButton.setPosition(x, this.originEditWidget.getBottom() + 1);
        this.mirrorButton.setPosition(x, this.rotateButton.getBottom() + 1);
        this.resetSubRegionsButton.setPosition(x, this.mirrorButton.getBottom() + 1);

        x = this.originEditWidget.getRight() + 2;
        y = this.originEditWidget.getY();
        this.lockXCoordCheckbox.setPosition(x, y + 2);
        this.lockYCoordCheckbox.setPosition(x, y + 20);
        this.lockZCoordCheckbox.setPosition(x, y + 38);

        y = this.getBottom() - 20;
        this.openMaterialListButton.setPosition(this.x + 10, y);
        this.openVerifierButton.setPosition(this.openMaterialListButton.getRight() + 2, y);
        this.openPlacementListButton.setRight(this.getRight() - 10);
        this.openPlacementListButton.setY(y);
    }

    @Override
    public void updateWidgetStates()
    {
        this.updateLabels();
        super.updateWidgetStates();
    }

    @Override
    protected DataListWidget<SubRegionPlacement> createListWidget()
    {
        Supplier<List<SubRegionPlacement>> supplier = this.placement::getAllSubRegions;
        DataListWidget<SubRegionPlacement> listWidget = new DataListWidget<>(supplier, true);

        listWidget.setListEntryWidgetFixedHeight(20);
        listWidget.getEntrySelectionHandler()
                .setAllowSelection(true)
                .setSelectionListener(this::onSelectionChange);
        listWidget.addDefaultSearchBar();
        listWidget.setEntryFilterStringFunction((p) -> Collections.singletonList(p.getDisplayName()));
        listWidget.setDataListEntryWidgetFactory((d, cd) -> new SchematicPlacementSubRegionEntryWidget(d, cd, this.placement));

        return listWidget;
    }

    protected void onSelectionChange(@Nullable SubRegionPlacement placement)
    {
        boolean clearSelection = placement == null || placement.getName().equals(this.placement.getSelectedSubRegionName());
        this.placement.setSelectedSubRegionName(clearSelection ? null : placement.getName());
    }

    protected boolean isNotLocked()
    {
        return this.placement.isLocked() == false;
    }

    protected boolean isCoordinateLocked(Coordinate coordinate)
    {
        return this.placement.isCoordinateLocked(coordinate);
    }

    protected void setCoordinateLocked(boolean locked, Coordinate coordinate)
    {
        this.placement.setCoordinateLocked(coordinate, locked);
    }

    protected void setOrigin(BlockPos origin)
    {
        this.manager.setOrigin(this.placement, origin);
    }

    protected boolean clickCopyPasteSettings(int mouseButton, GenericButton button)
    {
        if (isShiftDown())
        {
            // Shift + left click: Copy settings to clip board
            if (mouseButton == 0)
            {
                JsonObject origJson = this.placement.getSettingsShareJson();
                setStringToClipboard(JsonUtils.jsonToString(origJson, true));
                MessageDispatcher.success("litematica.message.info.settings_copied_to_clipboard");
                return true;
            }
            // Ctrl + Shift + Right click: load settings from clip board
            else if (mouseButton == 1 && isCtrlDown())
            {
                String str = getStringFromClipboard();

                if (this.loadSettingsFromString(str))
                {
                    MessageDispatcher.success("litematica.message.info.settings_loaded_from_clipboard");
                    this.initScreen();
                }

                return true;
            }
        }
        else if (mouseButton == 0)
        {
            String titleKey = "litematica.title.screen.schematic_placement_settings.copy_or_load_settings";
            JsonObject origJson = this.placement.getSettingsShareJson();

            TextInputScreen screen = new TextInputScreen(titleKey, origJson.toString(), this::loadSettingsFromStringWithMessage);
            screen.setParent(this);
            openPopupScreen(screen);
            return true;
        }

        return false;
    }

    protected boolean loadSettingsFromStringWithMessage(String str)
    {
        if (this.loadSettingsFromString(str))
        {
            MessageDispatcher.success("litematica.message.info.settings_loaded_from_string");
            return true;
        }

        return false;
    }

    protected boolean loadSettingsFromString(String str)
    {
        if (this.manager.loadPlacementSettingsFromSharedString(this.placement, str))
        {
            this.originEditWidget.setPosNoUpdate(this.placement.getPosition());
            this.updateWidgetStates();
        }

        return true;
    }

    protected void clickGridSettings()
    {
        if (BaseScreen.isShiftDown())
        {
            this.placement.getGridSettings().toggleEnabled();
            this.manager.updateGridPlacementsFor(this.placement);
        }
        else
        {
            PlacementGridSettingsScreen screen = new PlacementGridSettingsScreen(this.placement);
            screen.setParent(this);
            BaseScreen.openPopupScreen(screen);
        }
    }

    protected void changeSchematicButtonClicked()
    {
        BaseScreen screen = new ChooseActionScreen<>(300, "litematica.title.screen.confirm.change_schematic_in_placement",
                                                     ChangeSchematicType.VALUES, ChangeSchematicType::getDisplayName, ChangeSchematicType.FROM_FILE,
                                                     this::openChangeSchematicScreen,
                                                     (EventListener) null,
                                                     "litematica.button.misc.continue",
                                                     "litematica.button.misc.cancel",
                                                     "litematica.label.confirm.change_schematic_in_placement");
        openPopupScreenWithCurrentScreenAsParent(screen);
    }

    protected void openChangeSchematicScreen(ChangeSchematicType type)
    {
        if (type == ChangeSchematicType.FROM_FILE)
        {
            openScreenWithParent(new SchematicSelectorScreen(this::changeSchematicInPlacement));
        }
        else if (type == ChangeSchematicType.LOADED)
        {
            openScreenWithParent(new SelectLoadedSchematicScreen(this::changeSchematicInPlacement));
        }
    }

    protected void changeSchematicInPlacement(ISchematic newSchematic)
    {
        String oldSchematicName = this.placement.getSchematic().getMetadata().getName();
        String newSchematicName = newSchematic.getMetadata().getName();
        boolean unnamed = this.placement.getName().equals(oldSchematicName);

        DataManager.getSchematicPlacementManager().changeSchematicInPlacement(this.placement, newSchematic);

        if (unnamed)
        {
            this.resetName();
        }

        this.updateWidgetStates();

        MessageDispatcher.success("litematica.message.info.schematic_changed_in_placement", newSchematicName);
    }

    protected boolean mirror(int mouseButton, GenericButton button)
    {
        boolean reverse = mouseButton == 1;
        this.manager.setMirror(this.placement, PositionUtils.cycleMirror(this.placement.getMirror(), reverse));
        return true;
    }

    protected boolean rotate(int mouseButton, GenericButton button)
    {
        boolean reverse = mouseButton == 1;
        this.manager.setRotation(this.placement, PositionUtils.cycleRotation(this.placement.getRotation(), reverse));
        return true;
    }

    protected void openMaterialList()
    {
        MaterialListPlacement materialList = new MaterialListPlacement(this.placement, true);
        DataManager.setMaterialList(materialList); // Remember the last opened material list for the hotkey to (re-) open it
        BaseScreen.openScreen(new MaterialListScreen(materialList));
    }

    protected void openPlacementList()
    {
        openScreenWithParent(new SchematicPlacementsListScreen());
    }

    protected void openVerifier()
    {
        SchematicVerifier verifier = SchematicVerifierManager.INSTANCE.getOrCreateVerifierForPlacement(this.placement);
        BaseScreen.openScreen(new SchematicVerifierScreen(verifier));
    }

    protected void setName(String name)
    {
        this.placement.setName(name);
    }

    protected void resetName()
    {
        this.placement.setName(this.placement.getSchematic().getMetadata().getName());
        this.nameTextField.setText(this.placement.getName());
    }

    protected void resetSubRegions()
    {
        this.manager.resetAllSubRegionsToSchematicValues(this.placement);
        this.initScreen();
        this.updateWidgetStates();
    }

    protected void toggleAllRegionsOff()
    {
        this.manager.setSubRegionsEnabled(this.placement, false, this.getListWidget().getFilteredDataList());
        this.initScreen();
    }

    protected void toggleAllRegionsOn()
    {
        this.manager.setSubRegionsEnabled(this.placement, true, this.getListWidget().getFilteredDataList());
        this.initScreen();
    }

    protected void toggleEnclosingBoxRendering()
    {
        this.placement.toggleRenderEnclosingBox();
        this.toggleEnclosingBoxButton.updateHoverStrings();
    }

    protected void toggleEnabled()
    {
        this.manager.toggleEnabled(this.placement);
    }

    protected void toggleIgnoreEntities()
    {
        this.manager.toggleIgnoreEntities(this.placement);
    }

    protected void setBoundingBoxColor(int color)
    {
        this.placement.setBoundingBoxColor(color);
        OverlayRenderer.getInstance().updatePlacementCache();
    }

    protected void updateLabels()
    {
        String key = "litematica.label.schematic_placement_settings.sub_regions";
        int regionCount = this.placement.getSubRegionCount();
        this.subRegionsLabel.translateSetLines(key, regionCount);

        ISchematic schematic = this.placement.getSchematic();
        Path file = schematic.getFile();
        String fileName = file != null ? file.getFileName().toString() :
                          StringUtils.translate("litematica.hover.schematic_list.in_memory_only");

        key = "litematica.label.schematic_placement_settings.schematic_name";
        this.schematicNameLabel.translateSetLines(key, schematic.getMetadata().getName(), fileName);
    }

    protected Icon getEnclosingBoxButtonIcon()
    {
        return this.placement.shouldRenderEnclosingBox() ? LitematicaIcons.ENCLOSING_BOX_ENABLED :
                       LitematicaIcons.ENCLOSING_BOX_DISABLED;
    }

    protected List<String> getEnclosingBoxButtonHoverText()
    {
        String key;

        if (this.placement.shouldRenderEnclosingBox())
        {
            key = "litematica.hover.button.schematic_placement_settings.enclosing_box.on";
        }
        else
        {
            key = "litematica.hover.button.schematic_placement_settings.enclosing_box.off";
        }

        return ImmutableList.of(StringUtils.translate(key));
    }

    protected String getMirrorButtonLabel()
    {
        String val = litematica.util.PositionUtils.getMirrorName(this.placement.getMirror());
        String key = "litematica.button.schematic_placement_settings.mirror_value";
        return StringUtils.translate(key, val);
    }

    protected String getRotateButtonLabel()
    {
        String val = litematica.util.PositionUtils.getRotationNameShort(this.placement.getRotation());
        String key = "litematica.button.schematic_placement_settings.rotation_value";
        return StringUtils.translate(key, val);
    }

    protected enum ChangeSchematicType
    {
        FROM_FILE   ("litematica.label.schematic_placement_change_schematic.type.from_file"),
        LOADED      ("litematica.label.schematic_placement_change_schematic.type.loaded");

        public static final ImmutableList<ChangeSchematicType> VALUES = ImmutableList.copyOf(values());

        private final String translationKey;

        ChangeSchematicType(String translationKey)
        {
            this.translationKey = translationKey;
        }

        public String getDisplayName()
        {
            return StringUtils.translate(this.translationKey);
        }
    }
}
