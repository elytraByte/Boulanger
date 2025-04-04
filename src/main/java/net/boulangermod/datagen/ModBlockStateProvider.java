package net.boulangermod.datagen;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.boulangermod.Boulanger;
import net.boulangermod.block.ModBlocks;

public class ModBlockStateProvider implements DataProvider {
    private final PackOutput packOutput;

    public ModBlockStateProvider(PackOutput packOutput) {
        this.packOutput = packOutput;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return generateBlockStatesAndModels(cache);
    }

    private CompletableFuture<?> generateBlockStatesAndModels(CachedOutput cache) {
        CompletableFuture<?>[] futures = new CompletableFuture[]{
                generateLogBlock(cache, ModBlocks.PINE_LOG.getId()),
                generateLogBlock(cache, ModBlocks.STRIPPED_PINE_LOG.getId()),
                generateSimpleBlock(cache, ModBlocks.PINE_PLANKS.getId()),
                generateLeavesBlock(cache, ModBlocks.PINE_LEAVES.getId()),
                generateSaplingBlock(cache, ModBlocks.PINE_SAPLING.getId()),
//                generateWoodGasifierBlock(cache)
        };
        return CompletableFuture.allOf(futures);
    }

    // --------------------------
    // Block Model/State Generators
    // --------------------------

    private CompletableFuture<?> generateLogBlock(CachedOutput cache, ResourceLocation blockId) {
        // Block model
        JsonObject modelJson = new JsonObject();
        modelJson.addProperty("parent", "minecraft:block/cube_column");
        JsonObject textures = new JsonObject();
        textures.addProperty("end", blockId.getNamespace() + ":block/" + blockId.getPath() + "_top");
        textures.addProperty("side", blockId.getNamespace() + ":block/" + blockId.getPath());
        modelJson.add("textures", textures);

        return saveModel(cache, modelJson, blockId)
                .thenCompose(v -> generateBlockItemModel(cache, blockId));
    }

    private CompletableFuture<?> generateSimpleBlock(CachedOutput cache, ResourceLocation blockId) {
        JsonObject modelJson = new JsonObject();
        modelJson.addProperty("parent", "minecraft:block/cube_all");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", blockId.getNamespace() + ":block/" + blockId.getPath());
        modelJson.add("textures", textures);

        return saveModel(cache, modelJson, blockId)
                .thenCompose(v -> generateBlockItemModel(cache, blockId));
    }

    private CompletableFuture<?> generateLeavesBlock(CachedOutput cache, ResourceLocation blockId) {
        JsonObject modelJson = new JsonObject();
        modelJson.addProperty("parent", "minecraft:block/leaves");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", blockId.getNamespace() + ":block/" + blockId.getPath());
        modelJson.add("textures", textures);

        return saveModel(cache, modelJson, blockId)
                .thenCompose(v -> generateBlockItemModel(cache, blockId));
    }

    private CompletableFuture<?> generateSaplingBlock(CachedOutput cache, ResourceLocation blockId) {
        JsonObject modelJson = new JsonObject();
        modelJson.addProperty("parent", "minecraft:block/cross");
        JsonObject textures = new JsonObject();
        textures.addProperty("cross", blockId.getNamespace() + ":block/" + blockId.getPath());
        modelJson.add("textures", textures);

        return saveModel(cache, modelJson, blockId)
                .thenCompose(v -> generateBlockItemModel(cache, blockId));
    }

//    private CompletableFuture<?> generateWoodGasifierBlock(CachedOutput cache) {
//        ResourceLocation blockId = ModBlocks.WOOD_GASIFIER.getId();
//
//        // Block model
//        JsonObject modelJson = new JsonObject();
//        modelJson.addProperty("parent", "boulanger:block/wood-gasifier/wood-gasifier");
//
//        // Blockstate
//        JsonObject blockstateJson = new JsonObject();
//        JsonObject variants = new JsonObject();
//        JsonObject model = new JsonObject();
//        model.addProperty("model", "boulanger:block/wood-gasifier/wood-gasifier");
//        variants.add("", model);
//        blockstateJson.add("variants", variants);
//
//        return saveModel(cache, modelJson, blockId)
//                .thenCompose(v -> saveBlockstate(cache, blockstateJson, blockId))
//                .thenCompose(v -> generateBlockItemModel(cache, blockId));
//    }

    // --------------------------
    // Helper Methods
    // --------------------------

    private CompletableFuture<?> generateBlockItemModel(CachedOutput cache, ResourceLocation blockId) {
        JsonObject itemModelJson = new JsonObject();
        itemModelJson.addProperty("parent", blockId.getNamespace() + ":block/" + blockId.getPath());
        return saveItemModel(cache, itemModelJson, blockId);
    }

    private CompletableFuture<?> saveModel(CachedOutput cache, JsonObject json, ResourceLocation blockId) {
        Path path = packOutput.getOutputFolder()
                .resolve("assets/" + blockId.getNamespace() + "/models/block/" + blockId.getPath() + ".json");
        return DataProvider.saveStable(cache, json, path);
    }

    private CompletableFuture<?> saveBlockstate(CachedOutput cache, JsonObject json, ResourceLocation blockId) {
        Path path = packOutput.getOutputFolder()
                .resolve("assets/" + blockId.getNamespace() + "/blockstates/" + blockId.getPath() + ".json");
        return DataProvider.saveStable(cache, json, path);
    }

    private CompletableFuture<?> saveItemModel(CachedOutput cache, JsonObject json, ResourceLocation blockId) {
        Path path = packOutput.getOutputFolder()
                .resolve("assets/" + blockId.getNamespace() + "/models/item/" + blockId.getPath() + ".json");
        return DataProvider.saveStable(cache, json, path);
    }

    @Override
    public String getName() {
        return "Block States/Models: " + Boulanger.MODID;
    }
}