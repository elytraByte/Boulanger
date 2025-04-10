package net.boulangermod.boulanger.datagen;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;

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
}
