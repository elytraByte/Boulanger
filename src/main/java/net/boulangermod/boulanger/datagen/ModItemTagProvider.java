package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.util.IngredientTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider,
                              CompletableFuture<TagLookup<Block>> pBlockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(packOutput, pLookupProvider, pBlockTags, Boulanger.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        tag(ItemTags.LOGS_THAT_BURN).add(ModBlocks.PINE_LOG.asItem()).add(ModBlocks.PINE_WOOD.asItem()).add(ModBlocks.STRIPPED_PINE_LOG.asItem()).add(ModBlocks.STRIPPED_PINE_WOOD.asItem());

        tag(ItemTags.PLANKS)
                .add(ModBlocks.PINE_PLANKS.asItem());

//        tag(IngredientTags.LIQUIDS)
//                .add(ModItems.MILK.get(), ModItems.WATER.get(), ModItems.EGGSHELLS.get());

        tag(IngredientTags.SALTS)
                .add(ModItems.SALT_KOSHER.get());

        tag(IngredientTags.YEASTS)
                .add(ModItems.SAF_RED.get(), ModItems.SAF_GOLD.get(), ModItems.FRESH_YEAST.get());

        tag(IngredientTags.FATS)
                .add(ModItems.BUTTER.get(), ModItems.EURO_BUTTER.get());

//        tag(IngredientTags.SUGARS)
//                .add(ModItems.MOLASSES.get());

//        tag(IngredientTags.ADDITIVES)
//                .add(ModItems.DIASTATIC_MALT.get(), ModItems.ASCORBIC_ACID.get());
//
//        tag(IngredientTags.ENRICHMENTS)
//                .add(ModItems.EGGS.get(), ModItems.MILK_POWDER.get());

    }
    @Override
    public String getName() {
        return "Boulanger Item Tags";
    }
}
