package litematica.data;

import java.nio.file.Path;
import java.util.Optional;

import litematica.schematic.Schematic;

public class LoadedSchematic
{
    public final Schematic schematic;
    public final Optional<Path> file;
    protected boolean modifiedSinceSaved;

    public LoadedSchematic(Schematic schematic, Optional<Path> file)
    {
        this.schematic = schematic;
        this.file = file;
    }

    public boolean wasModifiedSinceSaved()
    {
        return this.modifiedSinceSaved;
    }

    public void setModifiedSinceSaved()
    {
        this.modifiedSinceSaved = true;
    }

    public void clearModifiedSinceSaved()
    {
        this.modifiedSinceSaved = false;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {return true;}
        if (o == null || this.getClass() != o.getClass()) {return false;}

        LoadedSchematic that = (LoadedSchematic) o;

        if (this.schematic.equals(that.schematic) == false)
        {
            return false;
        }

        if (this.file.isPresent() != that.file.isPresent())
        {
            return false;
        }

        return this.file.isPresent() == false || this.file.get().equals(that.file.get());
    }

    @Override
    public int hashCode()
    {
        int result = this.schematic.hashCode();
        result = 31 * result + this.file.hashCode();
        return result;
    }
}
