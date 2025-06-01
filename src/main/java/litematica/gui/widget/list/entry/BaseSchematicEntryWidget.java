package litematica.gui.widget.list.entry;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import malilib.gui.icon.DefaultIcons;
import malilib.gui.icon.Icon;
import malilib.gui.widget.IconWidget;
import malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import malilib.gui.widget.list.entry.DataListEntryWidgetData;
import malilib.render.text.StyledTextLine;
import malilib.util.FileNameUtils;
import malilib.util.StringUtils;
import litematica.gui.widget.AbstractSchematicInfoWidget;
import litematica.schematic.ISchematic;

public class BaseSchematicEntryWidget extends BaseDataListEntryWidget<ISchematic>
{
    protected final IconWidget modificationNoticeIcon;
    protected SimpleDateFormat dateFormat;

    public BaseSchematicEntryWidget(ISchematic schematic, DataListEntryWidgetData constructData)
    {
        super(schematic, constructData);

        this.dateFormat = AbstractSchematicInfoWidget.createDateFormat();

        String timeStr = this.dateFormat.format(new Date(schematic.getMetadata().getTimeModified()));
        Icon icon = schematic.getType().getIcon(schematic.getFile() == null);
        boolean modified = schematic.getMetadata().wasModifiedSinceSaved();

        this.modificationNoticeIcon = new IconWidget(DefaultIcons.EXCLAMATION_11);
        this.modificationNoticeIcon.translateAndAddHoverString("litematica.hover.schematic_list.modified_on", timeStr);
        this.iconOffset.setXOffset(3);
        this.textOffset.setXOffset(icon.getWidth() + 6);
        this.textSettings.setTextColor(modified ? 0xFFFF9010 : 0xFFFFFFFF);

        this.setIcon(icon);
        this.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, this.isOdd ? 0xA0101010 : 0xA0303030);
        this.getBackgroundRenderer().getHoverSettings().setEnabledAndColor(true, 0xA0707070);
        this.setText(StyledTextLine.parseFirstLine(schematic.getMetadata().getName()));
        this.addHoverInfo(schematic);
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        if (this.getData().getMetadata().wasModifiedSinceSaved())
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

    protected void addHoverInfo(ISchematic schematic)
    {
        List<String> lines = new ArrayList<>();
        Path schematicFile = schematic.getFile();
        String fileName = schematicFile != null ? schematicFile.getFileName().toString() :
                                  StringUtils.translate("litematica.hover.schematic_list.in_memory_only");

        lines.add(StringUtils.translate("litematica.hover.schematic_list.schematic_name", schematic.getMetadata().getName()));
        lines.add(StringUtils.translate("litematica.hover.schematic_list.schematic_file", fileName));
        lines.add(StringUtils.translate("litematica.hover.schematic_list.schematic_type", schematic.getType().getDisplayName()));

        if (schematic.getMetadata().wasModifiedSinceSaved())
        {
            String timeStr = this.dateFormat.format(new Date(schematic.getMetadata().getTimeModified()));
            lines.add(StringUtils.translate("litematica.hover.schematic_list.modified_on", timeStr));
        }

        this.getHoverInfoFactory().addStrings(lines);
    }

    public static boolean schematicSearchFilter(ISchematic entry, List<String> searchTerms)
    {
        String fileName = null;

        if (entry.getFile() != null)
        {
            fileName = entry.getFile().getFileName().toString().toLowerCase(Locale.ROOT);
            fileName = FileNameUtils.getFileNameWithoutExtension(fileName);
        }

        for (String searchTerm : searchTerms)
        {
            if (entry.getMetadata().getName().toLowerCase(Locale.ROOT).contains(searchTerm))
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
