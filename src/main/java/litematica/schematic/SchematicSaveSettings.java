package litematica.schematic;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;

import malilib.config.option.OptionListConfig;
import malilib.util.data.SimpleBooleanStorageWithDefault;
import malilib.util.data.json.JsonUtils;
import malilib.util.game.BlockUtils;
import malilib.util.game.wrap.RegistryUtils;
import malilib.util.world.BlockState;
import litematica.gui.SaveSchematicFromAreaScreen.SaveSide;
import litematica.util.value.SchematicSaveWorldSelection;

public class SchematicSaveSettings
{
    public final SimpleBooleanStorageWithDefault saveBlocks              = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault saveBlockEntities       = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault saveScheduledBlockTicks = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault saveEntities            = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault exposedBlocksOnly       = new SimpleBooleanStorageWithDefault(false);
    public final OptionListConfig<SchematicSaveWorldSelection> worldSelection = new OptionListConfig<>("-", SchematicSaveWorldSelection.VANILLA_ONLY, SchematicSaveWorldSelection.VALUES);
    public final OptionListConfig<SaveSide> saveSide = new OptionListConfig<>("-", SaveSide.AUTO, SaveSide.VALUES);

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
        newSettings.saveSide.setValue(this.saveSide.getValue());

        newSettings.ignoreBlocks.addAll(this.ignoreBlocks);
        newSettings.ignoreBlockStates.addAll(this.ignoreBlockStates);

        newSettings.schematicType = this.schematicType;

        return newSettings;
    }

    public JsonElement toJson()
    {
        JsonObject obj = new JsonObject();

        obj.addProperty("save_blocks", this.saveBlocks.getBooleanValue());
        obj.addProperty("save_block_entities", this.saveBlockEntities.getBooleanValue());
        obj.addProperty("save_scheduled_block_ticks", this.saveScheduledBlockTicks.getBooleanValue());
        obj.addProperty("save_entities", this.saveEntities.getBooleanValue());
        obj.addProperty("exposed_blocks_only", this.exposedBlocksOnly.getBooleanValue());
        obj.addProperty("world_selection", this.worldSelection.getValue().getName());
        obj.addProperty("save_side", this.saveSide.getValue().getName());

        obj.add("ignore_blocks", this.getIgnoredBlocksAsJson());
        obj.add("ignore_block_states", this.getIgnoredBlockStatesAsJson());

        return obj;
    }

    protected JsonElement getIgnoredBlocksAsJson()
    {
        JsonArray arr = new JsonArray();

        for (Block block : this.ignoreBlocks)
        {
            arr.add(RegistryUtils.getBlockIdStr(block));
        }

        return arr;
    }

    protected JsonElement getIgnoredBlockStatesAsJson()
    {
        JsonArray arr = new JsonArray();

        for (BlockState state : this.ignoreBlockStates)
        {
            arr.add(state.getFullStateString());
        }

        return arr;
    }

    public void tryLoad(String serializedValue)
    {
        JsonElement el = JsonUtils.parseJsonFromString(serializedValue);

        if ((el instanceof JsonObject) == false)
        {
            return;
        }

        JsonObject obj = el.getAsJsonObject();

        this.saveBlocks.setBooleanValue(JsonUtils.getBooleanOrDefault(obj, "save_blocks", true));
        this.saveBlockEntities.setBooleanValue(JsonUtils.getBooleanOrDefault(obj, "save_block_entities", true));
        this.saveScheduledBlockTicks.setBooleanValue(JsonUtils.getBooleanOrDefault(obj, "save_scheduled_block_ticks", true));
        this.saveEntities.setBooleanValue(JsonUtils.getBooleanOrDefault(obj, "save_entities", true));
        this.exposedBlocksOnly.setBooleanValue(JsonUtils.getBooleanOrDefault(obj, "exposed_blocks_only", false));

        this.worldSelection.setValue(SchematicSaveWorldSelection.findValueByName(JsonUtils.getStringOrDefault(obj, "world_selection", ""), SchematicSaveWorldSelection.VALUES));
        this.saveSide.setValue(SaveSide.findValueByName(JsonUtils.getStringOrDefault(obj, "save_side", ""), SaveSide.VALUES));

        this.ignoreBlocks.clear();
        JsonUtils.getArrayElementsIfExists(obj, "ignore_blocks", this::parseBlock);

        this.ignoreBlockStates.clear();
        JsonUtils.getArrayElementsIfExists(obj, "ignore_block_states", this::parseBlockState);
    }

    protected void parseBlock(JsonElement el)
    {
        Block block = RegistryUtils.getBlockByIdStr(el.getAsString());
        this.ignoreBlocks.add(block);
    }

    protected void parseBlockState(JsonElement el)
    {
        Optional<BlockState> stateOpt = BlockUtils.getBlockStateFromString(el.getAsString());

        if (stateOpt.isPresent())
        {
            this.ignoreBlockStates.add(stateOpt.get());
        }
    }
}
