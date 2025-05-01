package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.util.IngredientTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput packOutput,
                              CompletableFuture<HolderLookup.Provider> registries,
                              CompletableFuture<TagLookup<Block>> blockTags,
                              @Nullable ExistingFileHelper helper) {
        super(packOutput, registries, blockTags, Boulanger.MODID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // --- Vanilla log & plank tags ---
        tag(ItemTags.LOGS_THAT_BURN)
                .add(
                        ModBlocks.PINE_LOG.asItem(),
                        ModBlocks.PINE_WOOD.asItem(),
                        ModBlocks.STRIPPED_PINE_LOG.asItem(),
                        ModBlocks.STRIPPED_PINE_WOOD.asItem()
                );

        tag(ItemTags.PLANKS)
                .add(ModBlocks.PINE_PLANKS.asItem());

        // --- Water / liquid measurements ---
        tag(IngredientTags.WATER)
                .add(Items.WATER_BUCKET);

        // --- Dairy products ---
        tag(IngredientTags.DAIRY)
                .add(
                        ModItems.WHOLE_MILK.get()
//                        ModItems.HEAVY_CREAM.get(),
//                        ModItems.BUTTERMILK.get()
                );

        // --- Eggs ---
        tag(IngredientTags.EGGS)
                .add(
                        Items.EGG,
                        ModItems.FANCY_EGG.get(),
                        ModItems.EGG_YOLK.get(),
                        ModItems.EGG_WHITE.get()
                );

        // --- Salts ---
        tag(IngredientTags.SALTS)
                .add(ModItems.SALT_KOSHER.get());

        // --- Yeasts ---
        tag(IngredientTags.YEASTS)
                .add(
                        ModItems.SAF_RED.get(),
                        ModItems.SAF_GOLD.get(),
                        ModItems.FRESH_YEAST.get(),
                        ModItems.BREWERS_YEAST.get(),
                        ModItems.FLEISCHMANN.get()
                );

        // --- Fats / oils ---
        tag(IngredientTags.FATS)
                .add(
                        ModItems.BUTTER.get(),
                        ModItems.EURO_BUTTER.get(),
                        ModItems.EURO_BUTTER_BLEND.get()
                );

        // --- Sugars ---
        tag(IngredientTags.SUGARS)
                .add(
                        Items.SUGAR,
                        ModItems.BROWN_SUGAR.get(),
                        ModItems.POWDERED_SUGAR.get(),
                        ModItems.MOLASSES.get()
                );

        // --- General additives (e.g. colorants, enzymes) ---
//        tag(IngredientTags.ADDITIVES)
//                .add(
//                        ModItems.CARAMEL_COLOR.get(),
//                        ModItems.DIASTATIC_MALT.get(),
//                        ModItems.ASCORBIC_ACID.get()
//                );

        // --- Enrichments (e.g. milk powder) ---
//        tag(IngredientTags.ENRICHMENTS)
//                .add(ModItems.MILK_POWDER.get());

    }

    @Override
    public String getName() {
        return "Boulanger Item Tags";
    }
}
