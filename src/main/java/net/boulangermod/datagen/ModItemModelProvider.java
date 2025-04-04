package net.boulangermod.datagen;

import com.google.gson.JsonObject;
import net.boulangermod.block.ModBlocks;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.boulangermod.Boulanger;
import net.boulangermod.item.ModItems;
import net.minecraft.world.level.block.Block;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ModItemModelProvider implements DataProvider {
    private final PackOutput packOutput;

    public ModItemModelProvider(PackOutput packOutput) {
        this.packOutput = packOutput;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        generateItemModels(cache);
        return CompletableFuture.completedFuture(null);
    }

    private void generateItemModels(CachedOutput cache) {
        // Basic items
        generateSimpleItem(cache, ModItems.WHEAT_BERRIES.getId(), "item");
        generateSimpleItem(cache, ModItems.BRAN.getId(), "item");
        generateSimpleItem(cache, ModItems.BREAK_FLOUR.getId(), "item");
        generateSimpleItem(cache, ModItems.MIDDLINGS_FLOUR.getId(), "item");
        generateSimpleItem(cache, ModItems.PATENT_FLOUR.getId(), "item");
        generateSimpleItem(cache, ModItems.RYE_FLOUR.getId(), "item");
        generateSimpleItem(cache, ModItems.SEMOLINA_FLOUR.getId(), "item");
        generateSimpleItem(cache, ModItems.WHOLE_WHEAT_FLOUR.getId(), "item");
//        generateSimpleItem(cache, ModItems.ALL_PURPOSE_FLOUR.getId(), "item");
        generateSimpleItem(cache, ModItems.BREAD_FLOUR.getId(), "item");
        generateSimpleItem(cache, ModItems.HIGH_GLUTEN_FLOUR.getId(), "item");
//        generateSimpleItem(cache, ModItems.VITAL_WHEAT_GLUTEN.getId(), "item");
        generateSimpleItem(cache, ModItems.FIFTY_POUND_FLOUR.getId(), "item");
        generateSimpleItem(cache, ModItems.BUTTER.getId(), "item");
        generateSimpleItem(cache, ModItems.EURO_BUTTER.getId(), "item");
        generateSimpleItem(cache, ModItems.EURO_BUTTER_BLEND.getId(), "item");
        generateSimpleItem(cache, ModItems.SAF_RED.getId(), "item");
        generateSimpleItem(cache, ModItems.SAF_GOLD.getId(), "item");
        generateSimpleItem(cache, ModItems.FLEISCHMANN.getId(), "item");
        generateSimpleItem(cache, ModItems.FRESH_YEAST.getId(), "item");

        // Block items
        generateSimpleItem(cache, ModBlocks.PINE_SAPLING.getId(), "block");
    }

    private void generateSimpleItem(CachedOutput cache, ResourceLocation itemId, String textureType) {
        JsonObject modelJson = new JsonObject();
        modelJson.addProperty("parent", "minecraft:item/generated");

        JsonObject textures = new JsonObject();
        textures.addProperty("layer0",
                itemId.getNamespace() + ":" + textureType + "/" + itemId.getPath()
        );
        modelJson.add("textures", textures);

        // Save to file
        Path outputPath = packOutput.getOutputFolder()
                .resolve("assets/" + itemId.getNamespace() + "/models/item/" + itemId.getPath() + ".json");

        DataProvider.saveStable(cache, modelJson, outputPath);
    }

    @Override
    public String getName() {
        return "Item Models: " + Boulanger.MODID;
    }
}