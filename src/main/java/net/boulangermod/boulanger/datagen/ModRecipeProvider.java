package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput pRecipeOutput) {
        // Porcelain Mix → data/boulanger/recipes/decorations/porcelain_mix.json
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PORCELAIN_MIX.get(), 6)
                .pattern("KKB")
                .pattern("KKB")
                .pattern("GGB")
                .define('K', ModItems.KAOLINITE_CLAY_BALL.get())
                .define('B', ModItems.BONE_ASH.get())
                .define('G', ModItems.GLASS_DUST.get())
                .unlockedBy("has_kaolinite_clay", has(ModItems.KAOLINITE_CLAY_BALL.get()))
                .save(pRecipeOutput);  // ← no second argument

        // Sledgehammer → data/boulanger/recipes/tools/sledgehammer.json
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SLEDGEHAMMER.get())
                .pattern("  B")
                .pattern(" S ")
                .pattern("S  ")
                .define('B', Items.IRON_BLOCK)
                .define('S', Items.STICK)
                .unlockedBy("has_iron_block", has(Items.IRON_BLOCK))
                .save(pRecipeOutput);  // ← no second argument

        // Bone → Bone Ash (blast furnace) → data/boulanger/recipes/misc/bone_to_bone_ash_blasting.json
        SimpleCookingRecipeBuilder.blasting(
                        Ingredient.of(Items.BONE),
                        RecipeCategory.MISC,
                        ModItems.BONE_ASH.get(),
                        0.35f,
                        100
                )
                .unlockedBy("has_bone", has(Items.BONE))
                .save(pRecipeOutput);  // ← no second argument

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.GLASS_DUST.get(), 2)
                .requires(Items.GLASS)
                .unlockedBy("has_glass", has(Items.GLASS))
                .save(pRecipeOutput);
    }


}
