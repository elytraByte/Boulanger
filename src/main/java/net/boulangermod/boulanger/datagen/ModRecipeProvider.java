package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PCB.get(), 1)
                .pattern("CPC")
                .pattern("GRG")
                .pattern("III")
                .define('I', Items.COPPER_INGOT)
                .define('G', ModItems.GLASS_DUST.get())
                .define('R', Items.REDSTONE)
                .define('C', Items.COMPARATOR)
                .define('P', ModItems.PINE_RESIN)
                .unlockedBy("has_pine_resin", has(ModItems.PINE_RESIN))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GILDED_PCB.get(), 1)
                .pattern("CPC")
                .pattern("GRG")
                .pattern("III")
                .define('I', Items.GOLD_INGOT)
                .define('G', ModItems.GLASS_DUST.get())
                .define('R', Items.REDSTONE)
                .define('C', ModItems.PCB)
                .define('P', ModItems.PINE_RESIN)
                .unlockedBy("has_pine_resin", has(ModItems.PINE_RESIN))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.IRON_WEDGE.get(), 1)
                .pattern("   ")
                .pattern("NIN")
                .pattern(" N ")
                .define('I', Blocks.IRON_BLOCK)
                .define('N', Items.IRON_INGOT)
                .unlockedBy("has_sledge_hammer", has(ModItems.SLEDGEHAMMER))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BRICK_MOLD.get(), 1)
                .pattern("   ")
                .pattern("PBP")
                .pattern("PPP")
                .define('B', Items.BRICK)
                .define('P', ModBlocks.PINE_PLANKS)
                .unlockedBy("has_kaolinite_clay", has(ModItems.KAOLINITE_CLAY_BALL))
                .save(pRecipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.UNFIRED_PORCELAIN_BRICK.get(), 1)
                .requires(ModItems.BRICK_MOLD)
                .requires(ModItems.PORCELAIN_MIX)
                .unlockedBy("has_porcelain", has(ModItems.PORCELAIN_MIX))
                .unlockedBy("has_kaolinite_clay", has(ModItems.KAOLINITE_CLAY_BALL))
                .save(pRecipeOutput);

        SimpleCookingRecipeBuilder.blasting(
                        Ingredient.of(ModItems.UNFIRED_PORCELAIN_BRICK),
                        RecipeCategory.MISC,
                        ModItems.PORCELAIN_BRICK.get(),
                        0.35f,
                        100
                )
                .unlockedBy("has_kaolinite_clay", has(ModItems.KAOLINITE_CLAY_BALL))
                .save(pRecipeOutput);  // ← no second argument

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MIXING_BLOCK.get(), 1)
                .pattern("TCW")
                .pattern("DPD")
                .pattern("ERE")
                .define('T', Items.REDSTONE_TORCH)
                .define('C', Blocks.CAULDRON)
                .define('W', Items.CLOCK)
                .define('D', ModItems.DIORITE_PLATE)
                .define('P', Blocks.PISTON)
                .define('E', ModItems.PCB)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_pcb", has(ModItems.PCB))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SCALE_BLOCK.get(), 1)
                .pattern("PPP")
                .pattern("WUW")
                .pattern("WEW")
                .define('P', Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE)
                .define('W', ModBlocks.PINE_PLANKS)
                .define('U', Blocks.PISTON)
                .define('E', ModItems.PCB)
                .unlockedBy("has_pcb", has(ModItems.PCB))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.WOOD_OVEN.get(), 1)
                .pattern("DDD")
                .pattern("WUW")
                .pattern("DDD")
                .define('D', Blocks.DIORITE)
                .define('W', ModItems.REINFORCED_DIORITE_PLATE)
                .define('U', Blocks.BLAST_FURNACE)
                .unlockedBy("has_blast_frunace", has(Blocks.BLAST_FURNACE))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.STONE_MILL_BLOCK.get(), 1)
                .pattern("DDD")
                .pattern(" P ")
                .pattern("DDD")
                .define('D', Blocks.SMOOTH_STONE_SLAB)
                .define('P', Blocks.PISTON)
                .unlockedBy("has_wheat_seeds", has(ModItems.WHEAT_SEED))
                .save(pRecipeOutput);


    }


}
