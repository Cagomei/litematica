package litematica.schematic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;

import net.minecraft.nbt.NBTTagCompound;

import malilib.gui.icon.Icon;
import malilib.util.FileNameUtils;
import malilib.util.nbt.NbtUtils;
import litematica.gui.util.LitematicaIcons;

public class SchematicType<S extends ISchematic>
{
    public static final SchematicType<LitematicaSchematic> LITEMATICA = SchematicType.<LitematicaSchematic>builder()
            .setDisplayName("Litematica")
            .setFactory(LitematicaSchematic::new)
            .setDataValidator(LitematicaSchematic::isValidSchematic)
            .setExtension(LitematicaSchematic.FILE_NAME_EXTENSION)
            .setExtensionValidator(LitematicaSchematic.FILE_NAME_EXTENSION::equals)
            .setDefaultIcon(LitematicaIcons.SCHEMATIC_LITEMATIC)
            .setInMemoryIcon(LitematicaIcons.SCHEMATIC_IN_MEMORY_LITEMATIC)
            .setHasName(true)
            .build();

    public static final SchematicType<SchematicaSchematic> SCHEMATICA = SchematicType.<SchematicaSchematic>builder()
            .setDisplayName("Schematica/MCEdit")
            .setFactory(SchematicaSchematic::new)
            .setDataValidator(SchematicaSchematic::isValidSchematic)
            .setExtension(SchematicaSchematic.FILE_NAME_EXTENSION)
            .setExtensionValidator(SchematicaSchematic.FILE_NAME_EXTENSION::equals)
            .setDefaultIcon(LitematicaIcons.SCHEMATIC_SCHEMATICA)
            .setInMemoryIcon(LitematicaIcons.SCHEMATIC_IN_MEMORY_SCHEMATICA)
            .setHasName(true)
            .build();

    public static final SchematicType<SpongeSchematic> SPONGE = SchematicType.<SpongeSchematic>builder()
            .setDisplayName("Sponge")
            .setFactory(SpongeSchematic::new)
            .setDataValidator(SpongeSchematic::isValidSchematic)
            .setExtension(SpongeSchematic.FILE_NAME_EXTENSION)
            .setExtensionValidator((ext) -> SpongeSchematic.FILE_NAME_EXTENSION.equals(ext) || SchematicaSchematic.FILE_NAME_EXTENSION.equals(ext))
            .setDefaultIcon(LitematicaIcons.SCHEMATIC_SPONGE)
            .setInMemoryIcon(LitematicaIcons.SCHEMATIC_IN_MEMORY_SPONGE)
            .setHasName(true)
            .build();

    public static final SchematicType<VanillaStructure> VANILLA = SchematicType.<VanillaStructure>builder()
            .setDisplayName("Vanilla Structure")
            .setFactory(VanillaStructure::new)
            .setDataValidator(VanillaStructure::isValidSchematic)
            .setExtension(VanillaStructure.FILE_NAME_EXTENSION)
            .setExtensionValidator(VanillaStructure.FILE_NAME_EXTENSION::equals)
            .setDefaultIcon(LitematicaIcons.SCHEMATIC_VANILLA)
            .setInMemoryIcon(LitematicaIcons.SCHEMATIC_IN_MEMORY_VANILLA)
            .setHasName(true)
            .build();

    public static final ImmutableList<SchematicType<?>> KNOWN_TYPES = ImmutableList.of(LITEMATICA, SCHEMATICA, SPONGE, VANILLA);

    public static final Predicate<Path> SCHEMATIC_FILE_FILTER = p -> Files.isRegularFile(p) && Files.isReadable(p) && getPossibleTypesFromFileName(p).isEmpty() == false;

    private final String extension;
    private final Icon defaultIcon;
    private final Icon inMemoryIcon;
    private final Function<Path, S> factory;
    private final Function<String, Boolean> extensionValidator;
    private final Function<NBTTagCompound, Boolean> dataValidator;
    private final String displayName;
    private final boolean hasName;

    private SchematicType(String displayName, Function<Path, S> factory, Function<NBTTagCompound, Boolean> dataValidator,
                          String extension, Function<String, Boolean> extensionValidator,
                          Icon defaultIcon, Icon inMemoryIcon, boolean hasName)
    {
        this.displayName = displayName;
        this.extension = extension;
        this.factory = factory;
        this.extensionValidator = extensionValidator;
        this.dataValidator = dataValidator;
        this.defaultIcon = defaultIcon;
        this.inMemoryIcon = inMemoryIcon;
        this.hasName = hasName;
    }

    public String getFileNameExtension()
    {
        return this.extension;
    }

    public String getDisplayName()
    {
        return this.displayName;
    }

    public Icon getIcon()
    {
        return this.defaultIcon;
    }

    public Icon getInMemoryIcon()
    {
        return this.inMemoryIcon;
    }

    public Icon getIcon(boolean inMemoryOnly)
    {
        return inMemoryOnly ? this.inMemoryIcon : this.defaultIcon;
    }

    public boolean getHasName()
    {
        return this.hasName;
    }

    public boolean isValidExtension(String extension)
    {
        return this.extensionValidator.apply(extension).booleanValue();
    }

    public boolean isValidData(NBTTagCompound tag)
    {
        return this.dataValidator.apply(tag).booleanValue();
    }

    /**
     * Creates a new schematic, with the provided file passed to the constructor of the schematic.
     * This does not read anything from the file.
     */
    public S createSchematic(@Nullable Path file)
    {
        return this.factory.apply(file);
    }

    @Nullable
    public S createSchematicAndReadFromTag(@Nullable Path file, NBTTagCompound tag)
    {
        S schematic = this.factory.apply(file);

        if (schematic.fromTag(tag))
        {
            return schematic;
        }

        return null;
    }

    public static List<SchematicType<?>> getPossibleTypesFromFileName(Path file)
    {
        return getPossibleTypesFromFileName(file.getFileName().toString());
    }

    public static List<SchematicType<?>> getPossibleTypesFromFileName(String fileName)
    {
        String extension = "." + FileNameUtils.getFileNameExtension(fileName.toLowerCase(Locale.ROOT));
        List<SchematicType<?>> list = new ArrayList<>();

        for (SchematicType<?> type : KNOWN_TYPES)
        {
            if (type.isValidExtension(extension))
            {
                list.add(type);
            }
        }

        return list;
    }

    @Nullable
    public static SchematicType<?> getType(Path file, NBTTagCompound tag)
    {
        List<SchematicType<?>> possibleTypes = getPossibleTypesFromFileName(file);

        if (possibleTypes.isEmpty() == false)
        {
            for (SchematicType<?> type : possibleTypes)
            {
                if (type.isValidData(tag))
                {
                    return type;
                }
            }
        }

        return null;
    }

    @Nullable
    public static ISchematic tryCreateSchematicFrom(Path file)
    {
        List<SchematicType<?>> possibleTypes = getPossibleTypesFromFileName(file);

        if (possibleTypes.isEmpty() == false)
        {
            NBTTagCompound tag = NbtUtils.readNbtFromFile(file);

            if (tag != null)
            {
                SchematicType<?> type = getType(file, tag);

                if (type != null)
                {
                    return type.createSchematicAndReadFromTag(file, tag);
                }
            }
        }

        return null;
    }

    @Nullable
    public static ISchematic tryCreateSchematicFrom(Path file, NBTTagCompound tag)
    {
        SchematicType<?> type = getType(file, tag);
        return type != null ? type.createSchematicAndReadFromTag(file, tag) : null;
    }

    public static <S extends ISchematic> Builder<S> builder()
    {
        return new Builder<>();
    }

    public static class Builder<S extends ISchematic>
    {
        private String extension = null;
        private Icon defaultIcon = null;
        private Icon inMemoryIcon = null;
        private Function<Path, S> factory = null;
        private Function<String, Boolean> extensionValidator = null;
        private Function<NBTTagCompound, Boolean> dataValidator = null;
        private String displayName = "?";
        private boolean hasName = false;

        public Builder<S> setDataValidator(Function<NBTTagCompound, Boolean> dataValidator)
        {
            this.dataValidator = dataValidator;
            return this;
        }

        public Builder<S> setDisplayName(String displayName)
        {
            this.displayName = displayName;
            return this;
        }

        public Builder<S> setExtension(String extension)
        {
            this.extension = extension;
            return this;
        }

        public Builder<S> setExtensionValidator(Function<String, Boolean> extensionValidator)
        {
            this.extensionValidator = extensionValidator;
            return this;
        }

        public Builder<S> setFactory(Function<Path, S> factory)
        {
            this.factory = factory;
            return this;
        }

        public Builder<S> setHasName(boolean hasName)
        {
            this.hasName = hasName;
            return this;
        }

        public Builder<S> setDefaultIcon(Icon defaultIcon)
        {
            this.defaultIcon = defaultIcon;
            return this;
        }

        public Builder<S> setInMemoryIcon(Icon inMemoryIcon)
        {
            this.inMemoryIcon = inMemoryIcon;
            return this;
        }

        public SchematicType<S> build()
        {
            if (this.factory == null ||
                this.dataValidator == null ||
                this.extension == null ||
                this.extensionValidator == null ||
                this.defaultIcon == null ||
                this.displayName == null ||
                this.inMemoryIcon == null)
            {
                throw new IllegalArgumentException("SchematicType.Builder#build(): Some of the values were null!");
            }

            return new SchematicType<>(this.displayName, this.factory, this.dataValidator,
                                       this.extension, this.extensionValidator,
                                       this.defaultIcon, this.inMemoryIcon, this.hasName);
        }
    }
}
