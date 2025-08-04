package litematica.schematic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import malilib.util.FileNameUtils;
import malilib.util.StringUtils;
import malilib.util.data.tag.DataView;
import malilib.util.position.Vec3i;
import litematica.schematic.container.BlockContainer;
import litematica.schematic.container.BlockContainerFactory;

public class SchematicType
{
    public static final SchematicType LITEMATICA = SchematicType.builder()
        .setTranslationKey("litematica.schematic.type.litematica")
        .setBlockContainerFactory(LitematicaSchematic::createDefaultBlockContainer)
        .setSchematicFromDataFactory(LitematicaSchematic::fromData)
        .setSchematicFromRegionsFactory(LitematicaSchematic::fromRegions)
        .setMetadataFromDataFactory(LitematicaSchematic::createAndReadMetadata)
        .setDataValidator(LitematicaSchematic::isValidData)
        .setExtension(LitematicaSchematic.FILE_NAME_EXTENSION)
        .setExtensionValidator(LitematicaSchematic.FILE_NAME_EXTENSION::equalsIgnoreCase)
        .setHasName(true)
        .build();

    public static final SchematicType SCHEMATICA = SchematicType.builder()
        .setTranslationKey("litematica.schematic.type.schematica")
        .setBlockContainerFactory(SchematicaSchematic::createDefaultBlockContainer)
        .setSchematicFromDataFactory(SchematicaSchematic::fromData)
        .setSchematicFromRegionsFactory(SchematicaSchematic::fromRegions)
        .setMetadataFromDataFactory(SchematicaSchematic::createAndReadMetadata)
        .setDataValidator(SchematicaSchematic::isValidData)
        .setExtension(SchematicaSchematic.FILE_NAME_EXTENSION)
        .setExtensionValidator(SchematicaSchematic.FILE_NAME_EXTENSION::equalsIgnoreCase)
        .setHasName(true)
        .build();

    public static final SchematicType SPONGE = SchematicType.builder()
        .setTranslationKey("litematica.schematic.type.sponge")
        .setBlockContainerFactory(SpongeSchematic::createDefaultBlockContainer)
        .setSchematicFromDataFactory(SpongeSchematic::fromData)
        .setSchematicFromRegionsFactory(SpongeSchematic::fromRegions)
        .setMetadataFromDataFactory(SpongeSchematic::createAndReadMetadata)
        .setDataValidator(SpongeSchematic::isValidData)
        .setExtension(SpongeSchematic.FILE_NAME_EXTENSION)
        .setExtensionValidator((ext) -> SpongeSchematic.FILE_NAME_EXTENSION.equalsIgnoreCase(ext) || SchematicaSchematic.FILE_NAME_EXTENSION.equalsIgnoreCase(ext))
        .setHasName(true)
        .build();

    public static final SchematicType VANILLA = SchematicType.builder()
        .setTranslationKey("litematica.schematic.type.vanilla")
        .setBlockContainerFactory(VanillaSchematic::createDefaultBlockContainer)
        .setSchematicFromDataFactory(VanillaSchematic::fromData)
        .setSchematicFromRegionsFactory(VanillaSchematic::fromRegions)
        .setMetadataFromDataFactory(VanillaSchematic::createAndReadMetadata)
        .setDataValidator(VanillaSchematic::isValidData)
        .setExtension(VanillaSchematic.FILE_NAME_EXTENSION)
        .setExtensionValidator(VanillaSchematic.FILE_NAME_EXTENSION::equalsIgnoreCase)
        .setHasName(true)
        .build();

    public static ImmutableList<SchematicType> KNOWN_TYPES = ImmutableList.of(LITEMATICA, SCHEMATICA, SPONGE, VANILLA);

    public static final Predicate<Path> SCHEMATIC_FILE_FILTER = p -> Files.isRegularFile(p) &&
                                                                     Files.isReadable(p) &&
                                                                     getPossibleTypesFromFileName(p).isEmpty() == false;

    private final String extension;
    private final BlockContainerFactory containerFactory;
    private final Function<DataView, Optional<SchematicMetadata>> metadataFromDataFactory;
    private final Function<DataView, Optional<Schematic>> schematicFromDataFactory;
    private final Function<ImmutableMap<String, SchematicRegion>, Optional<Schematic>> schematicFromRegionsFactory;
    private final Function<String, Boolean> extensionValidator;
    private final Function<DataView, Boolean> dataValidator;
    private final String translationKey;
    private final boolean hasName;

    private SchematicType(String translationKey,
                          BlockContainerFactory containerFactory,
                          Function<DataView, Optional<Schematic>> schematicFromDataFactory,
                          Function<ImmutableMap<String, SchematicRegion>, Optional<Schematic>> schematicFromRegionsFactory,
                          Function<DataView, Optional<SchematicMetadata>> metadataFromDataFactory,
                          Function<DataView, Boolean> dataValidator,
                          String extension,
                          Function<String, Boolean> extensionValidator,
                          boolean hasName)
    {
        this.translationKey = translationKey;
        this.extension = extension;
        this.containerFactory = containerFactory;
        this.schematicFromDataFactory = schematicFromDataFactory;
        this.schematicFromRegionsFactory = schematicFromRegionsFactory;
        this.metadataFromDataFactory = metadataFromDataFactory;
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
        return StringUtils.translate(this.translationKey);
    }

    public boolean getHasName()
    {
        return this.hasName;
    }

    public boolean isValidExtension(String extension)
    {
        return this.extensionValidator.apply(extension).booleanValue();
    }

    public boolean isValidData(DataView dataIn)
    {
        return this.dataValidator.apply(dataIn).booleanValue();
    }

    public BlockContainer createContainer(Vec3i containerSize)
    {
        return this.containerFactory.create(containerSize);
    }

    public Optional<Schematic> createSchematicFromRegions(ImmutableMap<String, SchematicRegion> regions)
    {
        return this.schematicFromRegionsFactory.apply(regions);
    }

    public Optional<Schematic> createSchematicFromData(DataView dataIn)
    {
        return this.schematicFromDataFactory.apply(dataIn);
    }

    public Optional<SchematicMetadata> createMetadataFromData(DataView data)
    {
        return this.metadataFromDataFactory.apply(data);
    }

    public static List<SchematicType> getPossibleTypesFromFileName(Path file)
    {
        return getPossibleTypesFromFileName(file.getFileName().toString());
    }

    public static List<SchematicType> getPossibleTypesFromFileName(String fileName)
    {
        String extension = FileNameUtils.getFileNameExtension(fileName.toLowerCase(Locale.ROOT));
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

    public static List<SchematicType> getAllTypesSortedByProbability(Path file)
    {
        List<SchematicType> possibleTypes = SchematicType.getPossibleTypesFromFileName(file);

        for (SchematicType type : SchematicType.KNOWN_TYPES)
        {
            if (possibleTypes.contains(type))
            {
                continue;
            }

            possibleTypes.add(type);
        }

        return possibleTypes;
    }

    public static Optional<SchematicType> getTypeFromData(Path file, DataView dataIn)
    {
        List<SchematicType> possibleTypes = SchematicType.getAllTypesSortedByProbability(file);
        return getTypeFromData(possibleTypes, dataIn);
    }

    public static Optional<SchematicType> getTypeFromData(List<SchematicType> possibleTypes, DataView dataIn)
    {
        for (SchematicType type : possibleTypes)
        {
            if (type.isValidData(dataIn))
            {
                return Optional.of(type);
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
        private String extension;
        private BlockContainerFactory containerFactory;
        private Function<DataView, Optional<Schematic>> schematicFromDataFactory;
        private Function<ImmutableMap<String, SchematicRegion>, Optional<Schematic>> schematicFromRegionsFactory;
        private Function<DataView, Optional<SchematicMetadata>> metadataFromDataFactory;
        private Function<String, Boolean> extensionValidator;
        private Function<DataView, Boolean> dataValidator;
        private String displayName = "?";
        private boolean hasName = false;

        public SchematicType.Builder setDataValidator(Function<DataView, Boolean> dataValidator)
        {
            this.dataValidator = dataValidator;
            return this;
        }

        public SchematicType.Builder setTranslationKey(String displayName)
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

        public SchematicType.Builder setBlockContainerFactory(BlockContainerFactory containerFactory)
        {
            this.containerFactory = containerFactory;
            return this;
        }

        public SchematicType.Builder setSchematicFromDataFactory(Function<DataView, Optional<Schematic>> schematicFromDataFactory)
        {
            this.schematicFromDataFactory = schematicFromDataFactory;
            return this;
        }

        public SchematicType.Builder setSchematicFromRegionsFactory(Function<ImmutableMap<String, SchematicRegion>, Optional<Schematic>> schematicFromRegionsFactory)
        {
            this.schematicFromRegionsFactory = schematicFromRegionsFactory;
            return this;
        }

        public SchematicType.Builder setMetadataFromDataFactory(Function<DataView, Optional<SchematicMetadata>> metadataFromDataFactory)
        {
            this.metadataFromDataFactory = metadataFromDataFactory;
            return this;
        }

        public SchematicType.Builder setHasName(boolean hasName)
        {
            this.hasName = hasName;
            return this;
        }

        public SchematicType build()
        {
            if (this.containerFactory == null ||
                this.schematicFromDataFactory == null ||
                this.schematicFromRegionsFactory == null ||
                this.metadataFromDataFactory == null ||
                this.dataValidator == null ||
                this.extension == null ||
                this.extensionValidator == null ||
                this.displayName == null)
            {
                throw new IllegalArgumentException("SchematicType.Builder#build(): Some of the values were null!");
            }

            return new SchematicType(this.displayName,
                                     this.containerFactory,
                                     this.schematicFromDataFactory,
                                     this.schematicFromRegionsFactory,
                                     this.metadataFromDataFactory,
                                     this.dataValidator,
                                     this.extension,
                                     this.extensionValidator,
                                     this.hasName);
        }
    }
}
