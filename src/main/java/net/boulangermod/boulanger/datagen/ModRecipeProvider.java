package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

//    @Override
//    protected void buildRecipes(RecipeOutput pRecipeOutput) {

//        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.BLACK_OPAL_BLOCK.get())
//                .pattern("BBB")
//                .pattern("BBB")
//                .pattern("BBB")
//                .define('B', ModItems.BLACK_OPAL.get())
//                .unlockedBy("has_block_opal", has(ModItems.BLACK_OPAL.get())).save(pRecipeOutput);
//
//        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.IRON_WEDGE.get())
//                .pattern("BBB")
//                .pattern("iii")
//                .pattern("BiB")
//                .define('i', Items.IRON_INGOT)
//                .define('B', Items.AIR) // AIR slots are implicitly empty, you can omit this define if you like
//                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
//                .save(pRecipeOutput);
//
//        // 2) Shapeless recipe: Water Bucket → Water Jug
//        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.WATER_JUG.get())
//                .requires(Items.WATER_BUCKET)
//                .unlockedBy("has_water_bucket", has(Items.WATER_BUCKET))
//                .save(pRecipeOutput, Boulanger.MODID + ":water_bucket_to_jug");
//    }
//
//
}
