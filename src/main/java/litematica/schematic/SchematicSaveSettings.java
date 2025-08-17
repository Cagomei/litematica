package litematica.schematic;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;

import malilib.config.option.OptionListConfig;
import malilib.util.data.SimpleBooleanStorageWithDefault;
import malilib.util.world.BlockState;
import litematica.util.value.SchematicSaveWorldSelection;

public class SchematicSaveSettings
{
    public final SimpleBooleanStorageWithDefault saveBlocks              = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault saveBlockEntities       = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault saveScheduledBlockTicks = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault saveEntities            = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault exposedBlocksOnly       = new SimpleBooleanStorageWithDefault(false);
    public final OptionListConfig<SchematicSaveWorldSelection> worldSelection = new OptionListConfig<>("-", SchematicSaveWorldSelection.VANILLA_ONLY, SchematicSaveWorldSelection.VALUES);

    public final Set<Block> ignoreBlocks = new HashSet<>();
    public final Set<BlockState> ignoreBlockStates = new HashSet<>();

    public SchematicType schematicType = SchematicType.LITEMATICA;

    public SchematicSaveSettings copy()
    {
        SchematicSaveSettings newSettings = new SchematicSaveSettings();

        newSettings.saveBlocks.setBooleanValue(this.saveBlocks.getBooleanValue());
        newSettings.saveBlockEntities.setBooleanValue(this.saveBlockEntities.getBooleanValue());
        newSettings.saveScheduledBlockTicks.setBooleanValue(this.saveScheduledBlockTicks.getBooleanValue());
        newSettings.saveEntities.setBooleanValue(this.saveEntities.getBooleanValue());
        newSettings.exposedBlocksOnly.setBooleanValue(this.exposedBlocksOnly.getBooleanValue());

        newSettings.worldSelection.setValue(this.worldSelection.getValue());

        newSettings.ignoreBlocks.addAll(this.ignoreBlocks);
        newSettings.ignoreBlockStates.addAll(this.ignoreBlockStates);

        newSettings.schematicType = this.schematicType;

        return newSettings;
    }
}
