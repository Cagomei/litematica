package litematica.schematic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;

import malilib.util.FileNameUtils;
import malilib.util.nbt.CompoundData;
import malilib.util.nbt.NbtUtils;
import litematica.data.LoadedSchematic;
import litematica.schematic.old.LitematicaSchematic;
import litematica.schematic.old.SchematicaSchematic;
import litematica.schematic.old.SpongeSchematic;
import litematica.schematic.old.VanillaStructure;

public class SchematicType
{
    public static final SchematicType LITEMATICA = SchematicType.builder()
         .setDisplayName("Litematica")
         .setFactory(LitematicaSchematic::new)
         .setDataValidator(LitematicaSchematic::isValidSchematic)
         .setExtension(LitematicaSchematic.FILE_NAME_EXTENSION)
         .setExtensionValidator(LitematicaSchematic.FILE_NAME_EXTENSION::equals)
         .setHasName(true)
         .build();

    public static final SchematicType SCHEMATICA = SchematicType.builder()
         .setDisplayName("Schematica/MCEdit")
         .setFactory(SchematicaSchematic::new)
         .setDataValidator(SchematicaSchematic::isValidSchematic)
         .setExtension(SchematicaSchematic.FILE_NAME_EXTENSION)
         .setExtensionValidator(SchematicaSchematic.FILE_NAME_EXTENSION::equals)
         .setHasName(true)
         .build();

    public static final SchematicType SPONGE = SchematicType.builder()
         .setDisplayName("Sponge")
         .setFactory(SpongeSchematic::new)
         .setDataValidator(SpongeSchematic::isValidSchematic)
         .setExtension(SpongeSchematic.FILE_NAME_EXTENSION)
         .setExtensionValidator((ext) -> SpongeSchematic.FILE_NAME_EXTENSION.equals(ext) || SchematicaSchematic.FILE_NAME_EXTENSION.equals(ext))
         .setHasName(true)
         .build();

    public static final SchematicType VANILLA = SchematicType.builder()
        .setDisplayName("Vanilla Structure")
        .setFactory(VanillaStructure::new)
        .setDataValidator(VanillaStructure::isValidSchematic)
        .setExtension(VanillaStructure.FILE_NAME_EXTENSION)
        .setExtensionValidator(VanillaStructure.FILE_NAME_EXTENSION::equals)
        .setHasName(true)
        .build();

    public static ImmutableList<SchematicType> KNOWN_TYPES = ImmutableList.of(LITEMATICA, SCHEMATICA, SPONGE, VANILLA);

    public static final Predicate<Path> SCHEMATIC_FILE_FILTER = p -> Files.isRegularFile(p) &&
                                                                     Files.isReadable(p) &&
                                                                     getPossibleTypesFromFileName(p).isEmpty() == false;

    private final String extension;
    private final Supplier<Schematic> factory;
    private final Function<String, Boolean> extensionValidator;
    private final Function<CompoundData, Boolean> dataValidator;
    private final String displayName;
    private final boolean hasName;

    private SchematicType(String displayName, Supplier<Schematic> factory, Function<CompoundData, Boolean> dataValidator,
                          String extension, Function<String, Boolean> extensionValidator, boolean hasName)
    {
        this.displayName = displayName;
        this.extension = extension;
        this.factory = factory;
        this.extensionValidator = extensionValidator;
        this.dataValidator = dataValidator;
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

    public boolean getHasName()
    {
        return this.hasName;
    }

    public boolean isValidExtension(String extension)
    {
        return this.extensionValidator.apply(extension).booleanValue();
    }

    public boolean isValidData(CompoundData dataIn)
    {
        return this.dataValidator.apply(dataIn).booleanValue();
    }

    /**
     * Creates a new schematic instance of the given type.
     * @return the created new schematic instance
     */
    public Schematic createSchematic()
    {
        return this.factory.get();
    }

    // TODO remove this?
    public Optional<Schematic> createSchematicAndReadFromTag(CompoundData dataIn)
    {
        Schematic schematic = this.factory.get();

        if (schematic.read(dataIn))
        {
            return Optional.of(schematic);
        }

        return Optional.empty();
    }

    public static List<SchematicType> getPossibleTypesFromFileName(Path file)
    {
        return getPossibleTypesFromFileName(file.getFileName().toString());
    }

    public static List<SchematicType> getPossibleTypesFromFileName(String fileName)
    {
        String extension = "." + FileNameUtils.getFileNameExtension(fileName.toLowerCase(Locale.ROOT));
        List<SchematicType> list = new ArrayList<>();

        for (SchematicType type : KNOWN_TYPES)
        {
            if (type.isValidExtension(extension))
            {
                list.add(type);
            }
        }

        return list;
    }

    public static Optional<SchematicType> getType(Path file, CompoundData dataIn)
    {
        List<SchematicType> possibleTypes = getPossibleTypesFromFileName(file);

        if (possibleTypes.isEmpty() == false)
        {
            for (SchematicType type : possibleTypes)
            {
                if (type.isValidData(dataIn))
                {
                    return Optional.of(type);
                }
            }
        }

        return Optional.empty();
    }

    public static Optional<LoadedSchematic> tryLoadSchematic(Path schematicFile)
    {
        List<SchematicType> possibleTypes = getPossibleTypesFromFileName(schematicFile);

        if (possibleTypes.isEmpty() == false)
        {
            @Nullable
            CompoundData data = NbtUtils.readNbtFromFile(schematicFile);

            if (data != null)
            {
                Optional<SchematicType> typeOpt = getType(schematicFile, data);

                if (typeOpt.isPresent())
                {
                    Optional<Schematic> schematicOpt = typeOpt.get().createSchematicAndReadFromTag(data);

                    if (schematicOpt.isPresent())
                    {
                        return Optional.of(new LoadedSchematic(schematicOpt.get(), Optional.of(schematicFile)));
                    }
                }
            }
        }

        return Optional.empty();
    }

    public static void registerType(SchematicType type)
    {
        if (KNOWN_TYPES.contains(type))
        {
            return;
        }

        ArrayList<SchematicType> types = new ArrayList<>(KNOWN_TYPES);
        types.add(type);
        KNOWN_TYPES = ImmutableList.copyOf(types);
    }

    public static SchematicType.Builder builder()
    {
        return new SchematicType.Builder();
    }

    public static class Builder
    {
        private String extension = null;
        private Supplier<Schematic> factory = null;
        private Function<String, Boolean> extensionValidator = null;
        private Function<CompoundData, Boolean> dataValidator = null;
        private String displayName = "?";
        private boolean hasName = false;

        public SchematicType.Builder setDataValidator(Function<CompoundData, Boolean> dataValidator)
        {
            this.dataValidator = dataValidator;
            return this;
        }

        public SchematicType.Builder setDisplayName(String displayName)
        {
            this.displayName = displayName;
            return this;
        }

        public SchematicType.Builder setExtension(String extension)
        {
            this.extension = extension;
            return this;
        }

        public SchematicType.Builder setExtensionValidator(Function<String, Boolean> extensionValidator)
        {
            this.extensionValidator = extensionValidator;
            return this;
        }

        public SchematicType.Builder setFactory(Supplier<Schematic> factory)
        {
            this.factory = factory;
            return this;
        }

        public SchematicType.Builder setHasName(boolean hasName)
        {
            this.hasName = hasName;
            return this;
        }

        public SchematicType build()
        {
            if (this.factory == null ||
                this.dataValidator == null ||
                this.extension == null ||
                this.extensionValidator == null ||
                this.displayName == null)
            {
                throw new IllegalArgumentException("SchematicType.Builder#build(): Some of the values were null!");
            }

            return new SchematicType(this.displayName, this.factory, this.dataValidator,
                                     this.extension, this.extensionValidator, this.hasName);
        }
    }
}
