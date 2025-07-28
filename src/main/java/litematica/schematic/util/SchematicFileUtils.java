package litematica.schematic.util;

import java.nio.file.Files;
import java.nio.file.Path;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.tag.DataFileUtils;
import malilib.util.data.tag.CompoundData;
import litematica.schematic.Schematic;

public class SchematicFileUtils
{
    public static boolean readFromFile(Schematic schematic, Path fileIn)
    {
        if (Files.isRegularFile(fileIn) == false || Files.isReadable(fileIn) == false)
        {
            return false;
        }

        CompoundData data = DataFileUtils.readCompoundDataNbtFromFile(fileIn);

        if (data == null)
        {
            return false;
        }

        return schematic.read(data);
    }

    public static boolean writeToFile(Schematic schematic, Path fileOut, boolean overwrite)
    {
        if ((overwrite == false && Files.exists(fileOut)) || Files.isWritable(fileOut))
        {
            return false;
        }

        try
        {
            return DataFileUtils.writeCompressedCompoundData(fileOut, schematic.write());
        }
        catch (Exception e)
        {
            MessageDispatcher.error("litematica.message.error.schematic_save.failed_with_exception", e.getMessage());
        }

        return false;
    }
}
