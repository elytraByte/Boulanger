package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.datagen.builder.DoughProcessRecipeBuilder;
import net.boulangermod.boulanger.datagen.builder.RatioRecipeBuilder;
import net.boulangermod.boulanger.item.BreadType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.recipe.StepType;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static net.boulangermod.boulanger.Boulanger.MODID;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput pRecipeOutput) {
        // Existing shaped/shapeless/block recipes
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PORCELAIN_MIX.get(), 6)
                .pattern("KKB")
                .pattern("KKB")
                .pattern("GGB")
                .define('K', ModItems.KAOLINITE_CLAY_BALL.get())
                .define('B', ModItems.BONE_ASH.get())
                .define('G', ModItems.GLASS_DUST.get())
                .unlockedBy("has_kaolinite_clay", has(ModItems.KAOLINITE_CLAY_BALL.get()))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.UNFIRED_BLUE_PORCELAIN_BRICK, 8)
                .pattern("PPP")
                .pattern("PBP")
                .pattern("PPP")
                .define('P', ModItems.UNFIRED_PORCELAIN_BRICK.get())
                .define('B', Items.BLUE_DYE)
                .unlockedBy("has_kaolinite_clay", has(ModItems.KAOLINITE_CLAY_BALL.get()))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.UNFIRED_LIGHT_BLUE_PORCELAIN_BRICK, 8)
                .pattern("PPP")
                .pattern("PBP")
                .pattern("PPP")
                .define('P', ModItems.UNFIRED_PORCELAIN_BRICK.get())
                .define('B', Items.LIGHT_BLUE_DYE)
                .unlockedBy("has_kaolinite_clay", has(ModItems.KAOLINITE_CLAY_BALL.get()))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.UNFIRED_BLACK_PORCELAIN_BRICK, 8)
                .pattern("PPP")
                .pattern("PBP")
                .pattern("PPP")
                .define('P', ModItems.UNFIRED_PORCELAIN_BRICK.get())
                .define('B', Items.BLACK_DYE)
                .unlockedBy("has_kaolinite_clay", has(ModItems.KAOLINITE_CLAY_BALL.get()))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.BLUE_WHITE_TILE, 9)
                .pattern("BWB")
                .pattern("WBW")
                .pattern("BWB")
                .define('W', ModBlocks.WHITE_TILE)
                .define('B', ModBlocks.BLUE_TILE)
                .unlockedBy("has_kaolinite_clay", has(ModItems.KAOLINITE_CLAY_BALL.get()))
                .save(pRecipeOutput);

        // 4 × BLUE_PORCELAIN_BRICK -> 1 × BLUE_TILE
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.BLUE_TILE.get(), 1)
                .pattern("PP")
                .pattern("PP")
                .define('P', ModItems.BLUE_PORCELAIN_BRICK.get())
                .unlockedBy("has_blue_porcelain_brick", has(ModItems.BLUE_PORCELAIN_BRICK.get()))
                .save(pRecipeOutput, ResourceLocation.fromNamespaceAndPath(MODID, "blue_tile_from_bricks"));

        // 4 × LIGHT_BLUE_PORCELAIN_BRICK -> 1 × LIGHT_BLUE_TILE
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.LIGHT_BLUE_TILE.get(), 1)
                .pattern("PP")
                .pattern("PP")
                .define('P', ModItems.LIGHT_BLUE_PORCELAIN_BRICK.get())
                .unlockedBy("has_light_blue_porcelain_brick", has(ModItems.LIGHT_BLUE_PORCELAIN_BRICK.get()))
                .save(pRecipeOutput, ResourceLocation.fromNamespaceAndPath(MODID, "light_blue_tile_from_bricks"));

        // 4 × BLACK_PORCELAIN_BRICK -> 1 × BLACK_TILE
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.BLACK_TILE.get(), 1)
                .pattern("PP")
                .pattern("PP")
                .define('P', ModItems.BLACK_PORCELAIN_BRICK.get())
                .unlockedBy("has_black_porcelain_brick", has(ModItems.BLACK_PORCELAIN_BRICK.get()))
                .save(pRecipeOutput, ResourceLocation.fromNamespaceAndPath(MODID, "black_tile_from_bricks"));
        // 4 × PORCELAIN_BRICK -> 1 × WHITE_TILE
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.WHITE_TILE.get(), 1)
                .pattern("PP")
                .pattern("PP")
                .define('P', ModItems.PORCELAIN_BRICK.get())
                .unlockedBy("has_porcelain_brick", has(ModItems.PORCELAIN_BRICK.get()))
                .save(pRecipeOutput, ResourceLocation.fromNamespaceAndPath(MODID, "white_tile_from_bricks"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SLEDGEHAMMER.get())
                .pattern("  B")
                .pattern(" S ")
                .pattern("S  ")
                .define('B', Items.IRON_BLOCK)
                .define('S', Items.STICK)
                .unlockedBy("has_iron_block", has(Items.IRON_BLOCK))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SUGAR_REFINERY.get())
                .pattern(" B ")
                .pattern(" F ")
                .pattern("SSS")
                .define('F', ModBlocks.WOOD_OVEN)
                .define('B', Blocks.BREWING_STAND)
                .define('S', ModItems.DIORITE_PLATE)
                .unlockedBy("has_wood_oven", has(ModBlocks.WOOD_OVEN))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MOTIVATOR.get())
                .pattern("PPP")
                .pattern("DCD")
                .pattern("XGX")
                .define('P', ModBlocks.PINE_PLANKS)
                .define('D', ModItems.DIORITE_PLATE)
                .define('C', ModBlocks.MACHINE_HOUSING)
                .define('X', Blocks.PISTON)
                .define('G', ModItems.GILDED_PCB)
                .unlockedBy("has_machine_housing", has(ModBlocks.MACHINE_HOUSING))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.BAKERS_TABLE.get())
                .pattern("PPP")
                .pattern(" C ")
                .pattern(" O ")
                .define('P', Blocks.BLUE_CARPET)
                .define('C', Blocks.CRAFTING_TABLE)
                .define('O', ModItems.PAN)
                .unlockedBy("has_pan", has(ModItems.PAN))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DOUGH_DIVIDER)
                .pattern("PPP")
                .pattern("DCD")
                .pattern("DMD")
                .define('P', ModBlocks.PINE_PLANKS)
                .define('C', Items.IRON_SWORD)
                .define('D', ModItems.DIORITE_PLATE)
                .define('M', ModBlocks.MACHINE_HOUSING)
                .unlockedBy("has_proofing_box", has(ModBlocks.PROOFING_BOX))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MILLIGRAM_SCALE)
                .pattern("LIL")
                .pattern("ISI")
                .pattern("LIL")
                .define('L', Items.LAPIS_LAZULI)
                .define('I', Items.IRON_INGOT)
                .define('S', ModBlocks.SCALE_BLOCK)
                .unlockedBy("has_scale", has(ModBlocks.SCALE_BLOCK))
                .save(pRecipeOutput);

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(Items.BONE), RecipeCategory.MISC, ModItems.BONE_ASH.get(), 0.35f, 100)
                .unlockedBy("has_bone", has(Items.BONE))
                .save(pRecipeOutput);

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

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.UNFIRED_PORCELAIN_BRICK), RecipeCategory.MISC, ModItems.PORCELAIN_BRICK.get(), 0.35f, 100)
                .unlockedBy("has_kaolinite_clay", has(ModItems.KAOLINITE_CLAY_BALL))
                .save(pRecipeOutput);

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.UNFIRED_BLUE_PORCELAIN_BRICK), RecipeCategory.MISC, ModItems.BLUE_PORCELAIN_BRICK.get(), 0.35f, 100)
                .unlockedBy("has_kaolinite_clay", has(ModItems.KAOLINITE_CLAY_BALL))
                .save(pRecipeOutput);

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.UNFIRED_LIGHT_BLUE_PORCELAIN_BRICK), RecipeCategory.MISC, ModItems.LIGHT_BLUE_PORCELAIN_BRICK.get(), 0.35f, 100)
                .unlockedBy("has_kaolinite_clay", has(ModItems.KAOLINITE_CLAY_BALL))
                .save(pRecipeOutput);

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.UNFIRED_BLACK_PORCELAIN_BRICK), RecipeCategory.MISC, ModItems.BLACK_PORCELAIN_BRICK.get(), 0.35f, 100)
                .unlockedBy("has_kaolinite_clay", has(ModItems.KAOLINITE_CLAY_BALL))
                .save(pRecipeOutput);

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(Blocks.DIORITE), RecipeCategory.MISC, ModItems.DIORITE_BRICK.get(), 0.35f, 100)
                .unlockedBy("has_diorite", has(Blocks.DIORITE))
                .save(pRecipeOutput);

    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DIORITE_PLATE.get(), 1)
                .pattern("DDD")
                .pattern("DDD")
                .pattern("DDD")
                .define('D', ModItems.DIORITE_BRICK)
                .unlockedBy("has_diorite_brick", has(ModItems.DIORITE_BRICK))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.REINFORCED_DIORITE_PLATE.get(), 2)
                .pattern("IDI")
                .pattern("DID")
                .pattern("IDI")
                .define('D', ModItems.DIORITE_PLATE)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_diorite_brick", has(ModItems.DIORITE_PLATE))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MIXING_BLOCK.get(), 1)
                .pattern("TCW")
                .pattern("DPD")
                .pattern("ERE")
                .define('T', Items.REDSTONE_TORCH)
                .define('C', Blocks.CAULDRON)
                .define('W', Items.CLOCK)
                .define('D', ModItems.DIORITE_PLATE)
                .define('P', Blocks.PISTON)
                .define('E', ModItems.GILDED_PCB)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_pcb", has(ModItems.PCB))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MACHINE_HOUSING.get(), 1)
                .pattern("PDP")
                .pattern("DXD")
                .pattern("PDP")
                .define('P', Items.IRON_INGOT)
                .define('X', ModItems.PCB)
                .define('D', ModItems.DIORITE_PLATE)
                .unlockedBy("has_pcb", has(ModItems.PCB))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SCALE_BLOCK.get(), 1)
                .pattern("WWW")
                .pattern("DCD")
                .pattern("XBX")
                .define('W', Blocks.OAK_PRESSURE_PLATE)
                .define('D', ModItems.DIORITE_PLATE)
                .define('C', ModBlocks.MACHINE_HOUSING)
                .define('X', ModBlocks.PINE_PLANKS)
                .define('B', ModItems.GILDED_PCB)
                .unlockedBy("has_machine_housing", has(ModBlocks.MACHINE_HOUSING))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.PROOFING_BOX.get(), 1)
                .pattern("WXW")
                .pattern("DCD")
                .pattern("WGW")
                .define('W', ModItems.DIORITE_PLATE)
                .define('G', Blocks.CAMPFIRE)
                .define('C', ModBlocks.MACHINE_HOUSING)
                .define('D', ModBlocks.PINE_PLANKS)
                .define('X', ModItems.PCB)
                .unlockedBy("has_machine_housing", has(ModBlocks.MACHINE_HOUSING))
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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COPPER_CHANNEL.get(), 1)
                .pattern("   ")
                .pattern("C C")
                .pattern("CCC")
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.TREE_TAP.get(), 1)
                .pattern("C  ")
                .pattern("CU ")
                .pattern("CB ")
                .define('C', Blocks.COBBLESTONE)
                .define('U', ModItems.COPPER_CHANNEL)
                .define('B', Items.BUCKET)
                .unlockedBy("has_pine_logs", has(ModBlocks.PINE_LOG))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.WOOD_GASIFIER.get(), 1)
                .pattern("ICI")
                .pattern("IFI")
                .pattern("CBC")
                .define('C', Blocks.CAULDRON)
                .define('I', Items.IRON_INGOT)
                .define('F', Blocks.CAMPFIRE)
                .define('B', ModItems.PCB)
                .unlockedBy("has_pine_logs", has(ModBlocks.PINE_LOG))
                .save(pRecipeOutput);

        // ——— Baguette ———
        new RatioRecipeBuilder(
                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "baguette"),
                new ItemStack(ModItems.DOUGH.get()),
                0.05D // tolerance %
        )
                // 100% bread flour
                .addComponent(
                        IngredientCategory.FLOUR,
                        95.0,
                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "bread_flour"))
                )
                // 5% vital wheat gluten
                .addComponent(
                        IngredientCategory.FLOUR,
                        5.0,
                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "vital_wheat_gluten"))
                )
                // 70% water (vanilla water_bucket)
                .addComponent(
                        IngredientCategory.WATER,
                        70.0,
                        List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "water_bucket"))
                )
                // 3% salt
                .addComponent(
                        IngredientCategory.SALT,
                        3.0,
                        List.of()
                )
                // 4% yeast
                .addComponent(
                        IngredientCategory.YEAST,
                        4.0,
                        List.of()
                ).servingWeight(454)
                .save(pRecipeOutput);

// ——— Whole Wheat Bread ———
        new RatioRecipeBuilder(
                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "whole_wheat_bread"),
                new ItemStack(ModItems.DOUGH.get()),
                0.05D // 5% tolerance
        )
                // 100% total flour is implied by these three lines summing to 100%
                .addComponent(
                        IngredientCategory.FLOUR, 70.0,
                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "whole_wheat_flour"))
                )
                .addComponent(
                        IngredientCategory.FLOUR, 25.0,
                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "high_gluten_flour"))
                )
                .addComponent(
                        IngredientCategory.FLOUR, 5.0,
                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "wheat_bran"))
                )
                // then your other ingredients by baker's %
                .addComponent(
                        IngredientCategory.SUGAR, 10.0,
                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "brown_sugar"))
                )
                .addComponent(IngredientCategory.SALT, 6.0, List.of())
                .addComponent(IngredientCategory.YEAST, 8.0, List.of())
                .addComponent(
                        IngredientCategory.EGGS, 10.0,
                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "fancy_egg"))
                )
                .addComponent(
                        IngredientCategory.WATER, 72.0,
                        List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "water_bucket"))
                )
                .servingWeight(680.0)
                .save(pRecipeOutput);


//        // ——— Debug Hydrated Dough ———
//        new RatioRecipeBuilder(
//                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "debug_hydrated_dough"),
//                new ItemStack(ModItems.DOUGH.get()),
//                2.0D // tolerance %
//        )
//                .addComponent(
//                        IngredientCategory.FLOUR,
//                        100.0,
//                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "high_gluten_flour"))
//                )
//                .addComponent(
//                        IngredientCategory.WATER,
//                        100.0,
//                        List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "water_bucket"))
//                )
//                .servingWeight(200.0)
//                .save(pRecipeOutput);

        // ——— Banh Mi ———
        new RatioRecipeBuilder(
                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "banh_mi"),
                new ItemStack(ModItems.DOUGH.get()),
                0.05D // 5% tolerance
        )
                // Flour blend: 57% bread flour, 43% high-gluten flour
                .addComponent(
                        IngredientCategory.FLOUR, 57.0,
                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "bread_flour"))
                )
                .addComponent(
                        IngredientCategory.FLOUR, 43.0,
                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "high_gluten_flour"))
                )
                // Hydration: 31% water, 31% whole milk
                .addComponent(
                        IngredientCategory.WATER, 31.0,
                        List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "water_bucket"))
                )
                .addComponent(
                        IngredientCategory.DAIRY, 31.0,
                        List.of(ResourceLocation.fromNamespaceAndPath("boulanger", "whole_milk"))
                )
                // Enrichments & add-ins
                .addComponent(
                        IngredientCategory.EGGS, 12.0,
                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "fancy_egg"))
                )
                .addComponent(
                        IngredientCategory.FAT, 4.0,
                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "unsalted_butter"))
                )
                .addComponent(
                        IngredientCategory.SUGAR, 3.0,
                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "brown_sugar"))
                )
                .addComponent(
                        IngredientCategory.SALT, 2.0,
                        List.of()  // any salt
                )
                // Functional additives (all at baker’s %)
                .addComponent(
                        IngredientCategory.ADDITIVE, 0.09,
                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "ascorbic_acid"))
                )
                .addComponent(
                        IngredientCategory.ADDITIVE, 0.03,
                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "calcium_propionate"))
                )
                .addComponent(
                        IngredientCategory.ADDITIVE, 0.005,
                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "diastatic_malt_powder"))
                )
                .addComponent(
                        IngredientCategory.ADDITIVE, 0.005,
                        List.of(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "l_cysteine"))
                )
                .servingWeight(180.0)  // 180 g loaf
                .save(pRecipeOutput);


        new DoughProcessRecipeBuilder(
                ResourceLocation.fromNamespaceAndPath("boulanger", "dough_process/baguette"),
                ResourceLocation.fromNamespaceAndPath("boulanger", "baguette")
        )
                .addStep(StepType.PROOF, 1600)       // First proof (e.g., 80 seconds @ 20 tps)
                .addStep(StepType.PUNCHDOWN)         // Degas
                .addStep(StepType.PROOF, 1600)       // Second proof
                .addStep(StepType.PUNCHDOWN)         // Optional degas before divide
                .addStep(StepType.DIVIDE)            // → Used by DoughDividerBlockEntity
                .setServingWeight(454.0)             // ← Defines portion size post-divide
                .addStep(StepType.SHAPE)             // Shape dough into pan or free-form
                .setPanType(ResourceLocation.fromNamespaceAndPath(MODID, PanType.BAGUETTE.getId()))
                .save(pRecipeOutput);

        new DoughProcessRecipeBuilder(
                ResourceLocation.fromNamespaceAndPath("boulanger", "dough_process/whole_wheat_bread"),
                ResourceLocation.fromNamespaceAndPath("boulanger", "whole_wheat_bread")
        )
                .addStep(StepType.PROOF, 1600)       // 1st proof
                .addStep(StepType.PUNCHDOWN)         // degas
                .addStep(StepType.PROOF, 1600)       // 2nd proof
                .addStep(StepType.PUNCHDOWN)         // degas before divide
                .addStep(StepType.DIVIDE)            // split into portions
                .addStep(StepType.SHAPE)             // shape into pan or free-form
                .addStep(StepType.PROOF, 1600)       // final proof after shaping
                .setServingWeight(680.0)             // each portion is 680 g
                .setPanType(ResourceLocation.fromNamespaceAndPath(MODID, PanType.LOAF.getId()))
                .save(pRecipeOutput);

        // ——— Banh Mi dough‐process pipeline ———
        new DoughProcessRecipeBuilder(
                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "dough_process/banh_mi"),
                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "banh_mi")
        )
                .addStep(StepType.PROOF, 1600)     // 1st bulk proof
                .addStep(StepType.PUNCHDOWN)       // degas
                .addStep(StepType.PROOF, 1600)     // 2nd proof
                .addStep(StepType.PUNCHDOWN)       // degas before dividing
                .addStep(StepType.DIVIDE)          // split into 1×180 g portions
                .setServingWeight(180.0)
                .addStep(StepType.SHAPE)           // shape into banh mi loaf form
                .setPanType(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, PanType.BAGUETTE.getId()))
                .addStep(StepType.PROOF, 1600)     // final proof in pan
                .save(pRecipeOutput);


    }
}
