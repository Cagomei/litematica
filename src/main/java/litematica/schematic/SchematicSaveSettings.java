package litematica.schematic;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

import malilib.util.data.SimpleBooleanStorageWithDefault;

public class SchematicSaveSettings
{
    public final SimpleBooleanStorageWithDefault saveBlocks              = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault saveBlockEntities       = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault saveScheduledBlockTicks = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault saveEntities            = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault saveFromVanillaWorld    = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault saveFromSchematicWorld  = new SimpleBooleanStorageWithDefault(false);
    public final SimpleBooleanStorageWithDefault clientWorldFirst        = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault exposedBlocksOnly       = new SimpleBooleanStorageWithDefault(false);
    public final Set<Block> ignoreBlocks = new HashSet<>();
    public final Set<IBlockState> ignoreBlockStates = new HashSet<>();
    public SchematicType schematicType = SchematicType.LITEMATICA;
}
