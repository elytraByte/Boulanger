package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.block.crops.HardRedSpringWheatCrop;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, String modid, ExistingFileHelper exFileHelper) {
        super(output, Boulanger.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {

        logBlock(((RotatedPillarBlock) ModBlocks.PINE_LOG.get()));
        logBlock(((RotatedPillarBlock) ModBlocks.STRIPPED_PINE_LOG.get()));
        logBlock(((RotatedPillarBlock) ModBlocks.STRIPPED_PINE_WOOD.get()));
        blockItem(ModBlocks.PINE_LOG);
        blockItem(ModBlocks.STRIPPED_PINE_LOG);
        blockItem(ModBlocks.STRIPPED_PINE_WOOD);
        blockWithItem(ModBlocks.PINE_PLANKS);
        leavesBlock(ModBlocks.PINE_LEAVES);
        saplingBlock(ModBlocks.PINE_SAPLING);
        blockItem(ModBlocks.WOOD_OVEN);
        blockWithItem(ModBlocks.MIXING_BLOCK);
        blockWithItem(ModBlocks.SCALE_BLOCK);
        blockWithItem(ModBlocks.KAOLINITE_CLAY);
        blockWithItem(ModBlocks.BLACK_TILE);
        blockWithItem(ModBlocks.DARK_BLUE_TILE);
        blockWithItem(ModBlocks.BLUE_TILE);
        blockWithItem(ModBlocks.DARK_BLUE_WHITE_TILE);
        blockWithItem(ModBlocks.L3E_TILE);
        blockWithItem(ModBlocks.WHITE_TILE);
        simpleBlock(ModBlocks.WILD_WHEAT.get(),
                models().cross(
                        ModBlocks.WILD_WHEAT.getId().getPath(),
                        modLoc("block/wild_wheat")
                ).renderType("cutout")
        );

        makeCrop((CropBlock) ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get(), "boulanger_wheat_stage", "boulanger_wheat_stage");


//        blockItem(ModBlocks.WOOD_GASIFIER);
//        getVariantBuilder(ModBlocks.WOOD_GASIFIER.get())
//                .forAllStates(state -> ConfiguredModel.builder()
//                        .modelFile(new ModelFile.UncheckedModelFile("boulanger:block/wood-gasifier/wood-gasifier"))
//                        .build());

        getVariantBuilder(ModBlocks.WOOD_OVEN.get())
                .forAllStates(state -> {
                    boolean lit = state.getValue(BlockStateProperties.LIT);
                    Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    String suffix = lit ? "_on" : "_off";

                    models().orientable("wood_oven_off",
                            modLoc("block/wood_oven_side"),
                            modLoc("block/wood_oven_front_off"),
                            modLoc("block/wood_oven_top"));
                    models().orientable("wood_oven_on",
                            modLoc("block/wood_oven_side"),
                            modLoc("block/wood_oven_front_on"),
                            modLoc("block/wood_oven_top"));
                    return ConfiguredModel.builder()
                            .modelFile(models().orientable(
                                    "wood_oven" + suffix,
                                    modLoc("block/wood_oven_side"),
                                    modLoc("block/wood_oven_front" + suffix),
                                    modLoc("block/wood_oven_top")
                            ))
                            .rotationY((int) facing.toYRot())
                            .build();
                });


    }


    private void leavesBlock(DeferredBlock<Block> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(),
                models().singleTexture(BuiltInRegistries.BLOCK.getKey(deferredBlock.get()).getPath(), ResourceLocation.parse("minecraft:block/leaves"),
                        "all", blockTexture(deferredBlock.get())).renderType("cutout"));
    }

    private void saplingBlock(DeferredBlock<Block> deferredBlock) {
        simpleBlock(deferredBlock.get(), models().cross(BuiltInRegistries.BLOCK.getKey(deferredBlock.get()).getPath(), blockTexture(deferredBlock.get())).renderType("cutout"));
    }


    private void blockWithItem(DeferredBlock<Block> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    private void blockItem(DeferredBlock<Block> deferredBlock) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile("boulanger:block/" + deferredBlock.getId().getPath()));
    }

//    public void makeCrop(CropBlock block, String modelName, String textureName) {
//        Function<BlockState, ConfiguredModel[]> function = state -> states(state, block, modelName, textureName);
//
//        getVariantBuilder(block).forAllStates(function);
//    }

//    private ConfiguredModel[] states(BlockState state, CropBlock block, String modelName, String textureName) {
//        // Use block.getAge(state) instead of the property approach
//        int age = block.getAge(state);
//
//        // Build a unique model name for each age
//        String finalModelName = modelName + age;
//
//        // Create a ResourceLocation for the texture
//        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
//                Boulanger.MODID,
//                "block/wheat/" + textureName
//        );
//
//        // Create the model. The 'renderType("cutout")' part typically goes elsewhere,
//        // or is handled by your model JSON / client setup code.
//        return new ConfiguredModel[]{
//                new ConfiguredModel(models().crop(finalModelName, texture))
//        };
//    }

//    private ConfiguredModel[] states(BlockState state, CropBlock block, String modelName, String textureNameBase) {
//        // Retrieve the crop's age with the public getAge() method.
//        int age = block.getAge(state);
//
//        // Build a unique model name for this age stage (for example: "wheat_stage0", "wheat_stage1", etc.)
//        String finalModelName = modelName + age;
//
//        // Append the age to the texture base name as well.
//        String finalTextureName = textureNameBase + age;
//
//        // Create the ResourceLocation. It will now refer to:
//        // assets/boulanger/textures/block/wheat/{finalTextureName}.png
//        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "block/wheat/" + finalTextureName);
//
//        // Build and return the model for this state.
//        return new ConfiguredModel[] {
//                new ConfiguredModel(models().crop(finalModelName, texture))
//        };
//    }

    public void makeCrop(CropBlock block, String modelNameBase, String textureNameBase) {
        int maxAge = block.getMaxAge();

        // Store each generated ModelFile in a map keyed by the crop’s age
        Map<Integer, ModelFile> ageToModelMap = new HashMap<>();

        // 1) Generate and register a model file for each possible crop age
        for (int age = 0; age <= maxAge; age++) {
            // Build a unique model name, e.g. "wheat_stage0", "wheat_stage1", ...
            String modelName = modelNameBase + age;
            // Build a unique texture name, e.g. "boulanger_wheat_stage0", "boulanger_wheat_stage1", ...
            String textureName = textureNameBase + age;

            // Construct the ResourceLocation for your texture
            ResourceLocation texture =ResourceLocation.fromNamespaceAndPath(
                    Boulanger.MODID,
                    "block/wheat/" + textureName
            );

            // Generate and register the actual crop model.
            // 'renderType("cutout")' sets it up to use cutout transparency.
            ModelFile modelFile = models()
                    .crop(modelName, texture)
                    .renderType("cutout");

            // Store this ModelFile so we can reference it when setting block states
            ageToModelMap.put(age, modelFile);
        }

        // 2) Register the block state variants so each age points to the matching model
        getVariantBuilder(block).forAllStates(state -> {
            // Retrieve the current crop age
            int age = block.getAge(state);

            // Find the corresponding ModelFile
            ModelFile modelFile = ageToModelMap.get(age);

            // Build the ConfiguredModel using the ModelFile we generated earlier
            return new ConfiguredModel[] {
                    new ConfiguredModel(modelFile)
            };
        });
    }



}

