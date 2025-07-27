package litematica.gui.widget.list.entry;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import malilib.gui.icon.DefaultIcons;
import malilib.gui.icon.Icon;
import malilib.gui.widget.IconWidget;
import malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import malilib.gui.widget.list.entry.DataListEntryWidgetData;
import malilib.render.text.StyledTextLine;
import malilib.util.FileNameUtils;
import malilib.util.StringUtils;
import litematica.data.LoadedSchematic;
import litematica.gui.util.SchematicTypeIcons;
import litematica.gui.widget.AbstractSchematicInfoWidget;
import litematica.schematic.Schematic;
import litematica.schematic.old.ISchematic;

public class BaseSchematicEntryWidget extends BaseDataListEntryWidget<LoadedSchematic>
{
    protected final IconWidget modificationNoticeIcon;
    protected SimpleDateFormat dateFormat;

    public BaseSchematicEntryWidget(LoadedSchematic loadedSchematic, DataListEntryWidgetData constructData)
    {
        super(loadedSchematic, constructData);

        this.dateFormat = AbstractSchematicInfoWidget.createDateFormat();

        Schematic schematic = loadedSchematic.schematic;
        String timeStr = this.dateFormat.format(new Date(schematic.getMetadata().getTimeModified()));
        Icon icon = SchematicTypeIcons.INSTANCE.getIcon(loadedSchematic).orElse(DefaultIcons.EXCLAMATION_11);

        this.modificationNoticeIcon = new IconWidget(DefaultIcons.EXCLAMATION_11);
        this.modificationNoticeIcon.translateAndAddHoverString("litematica.hover.schematic_list.modified_on", timeStr);
        this.iconOffset.setXOffset(3);
        this.textOffset.setXOffset(icon.getWidth() + 6);
        this.textSettings.setTextColor(loadedSchematic.wasModifiedSinceSaved() ? 0xFFFF9010 : 0xFFFFFFFF);

        this.setIcon(icon);
        this.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, this.isOdd ? 0xA0101010 : 0xA0303030);
        this.getBackgroundRenderer().getHoverSettings().setEnabledAndColor(true, 0xA0707070);
        this.setText(StyledTextLine.parseFirstLine(schematic.getMetadata().getSchematicName()));
        this.addHoverInfo(loadedSchematic);
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        if (this.getData().wasModifiedSinceSaved())
        {
            this.addWidget(this.modificationNoticeIcon);
        }
    }

    @Override
    public void updateSubWidgetPositions()
    {
        super.updateSubWidgetPositions();

        this.modificationNoticeIcon.centerVerticallyInside(this);
        this.modificationNoticeIcon.setRight(this.getRight() - 6);
    }

    protected void addHoverInfo(LoadedSchematic loadedSchematic)
    {
        List<String> lines = new ArrayList<>();
        Optional<Path> schematicFileOpt = loadedSchematic.file;
        Schematic schematic = loadedSchematic.schematic;
        String fileName = schematicFileOpt.isPresent() ? schematicFileOpt.get().getFileName().toString() :
                                  StringUtils.translate("litematica.hover.schematic_list.in_memory_only");

        lines.add(StringUtils.translate("litematica.hover.schematic_list.schematic_name", schematic.getMetadata().getSchematicName()));
        lines.add(StringUtils.translate("litematica.hover.schematic_list.schematic_file", fileName));
        lines.add(StringUtils.translate("litematica.hover.schematic_list.schematic_type", schematic.getType().getDisplayName()));

        if (loadedSchematic.wasModifiedSinceSaved())
        {
            String timeStr = this.dateFormat.format(new Date(schematic.getMetadata().getTimeModified()));
            lines.add(StringUtils.translate("litematica.hover.schematic_list.modified_on", timeStr));
        }

        this.getHoverInfoFactory().addStrings(lines);
    }

    public static boolean schematicSearchFilter(LoadedSchematic entry, List<String> searchTerms)
    {
        String fileName = null;

        if (entry.file.isPresent())
        {
            fileName = entry.file.get().getFileName().toString().toLowerCase(Locale.ROOT);
            fileName = FileNameUtils.getFileNameWithoutExtension(fileName);
        }

        for (String searchTerm : searchTerms)
        {
            if (entry.schematic.getMetadata().getSchematicName().toLowerCase(Locale.ROOT).contains(searchTerm))
            {
                return true;
            }

            if (fileName != null && fileName.contains(searchTerm))
            {
                return true;
            }
        }

        return false;
    }
}
