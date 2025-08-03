package litematica.schematic.util;

import java.nio.file.Files;
import java.nio.file.Path;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.tag.util.DataFileUtils;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.DataView;
import litematica.schematic.Schematic;

public class SchematicFileUtils
{
    public static boolean readFromFile(Schematic schematic, Path file)
    {
        if (Files.isRegularFile(file) == false || Files.isReadable(file) == false)
        {
            return false;
        }

        CompoundData data = DataFileUtils.readCompoundDataFromNbtFile(file);

        if (data == null)
        {
            MessageDispatcher.error("litematica.error.schematic_read_from_file_failed.cant_read",
                                    file.toAbsolutePath().toString());
            return false;
        }

        return schematic.read(data);
    }

    public static boolean writeToFile(Schematic schematic, Path file, boolean overwrite)
    {
        String fileName = file.getFileName().toString();
        String extension = schematic.getType().getFileNameExtension();

        if (fileName.endsWith(extension) == false)
        {
            fileName = fileName + extension;
            file = file.getParent().resolve(fileName);
        }

        if (overwrite == false && Files.exists(file))
        {
            MessageDispatcher.error("litematica.error.schematic_write_to_file_failed.exists",
                                    file.toAbsolutePath().toString());
            return false;
        }

        if (Files.isWritable(file))
        {
            MessageDispatcher.error("litematica.error.schematic_write_to_file_failed.not_writable",
                                    file.toAbsolutePath().toString());
            return false;
        }

        DataView data;

        try
        {
            data = schematic.write();
        }
        catch (Exception e)
        {
            String key = "litematica.message.error.schematic_save.serializing_schematic_data_failed";
            MessageDispatcher.error().console(e).translate(key, e.getMessage());
            return false;
        }

        return DataFileUtils.writeCompoundDataToCompressedNbtFile(file, data);
    }
}
