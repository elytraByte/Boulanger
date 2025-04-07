package net.boulangermod.boulanger.datagen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
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
//        blockWithItem(ModBlocks.MIXER);

//        blockWithItem(ModBlocks.HARD_RED_SPRING_WHEAT_CROP);

        logBlock(((RotatedPillarBlock) ModBlocks.PINE_LOG.get()));
        logBlock(((RotatedPillarBlock) ModBlocks.STRIPPED_PINE_LOG.get()));
        blockItem(ModBlocks.PINE_LOG);
        blockWithItem(ModBlocks.KAOLINITE_CLAY);
        blockItem(ModBlocks.PINE_WOOD);
        blockItem(ModBlocks.STRIPPED_PINE_LOG);
        blockItem(ModBlocks.STRIPPED_PINE_WOOD);
        blockWithItem(ModBlocks.PINE_PLANKS);
        leavesBlock(ModBlocks.PINE_LEAVES);
        saplingBlock(ModBlocks.PINE_SAPLING);

        blockItem(ModBlocks.WOOD_GASIFIER);
        getVariantBuilder(ModBlocks.WOOD_GASIFIER.get())
                .forAllStates(state -> ConfiguredModel.builder()
                        .modelFile(new ModelFile.UncheckedModelFile("boulanger:block/wood-gasifier/wood-gasifier"))
                        .build());

        getVariantBuilder(ModBlocks.WOOD_OVEN.get())
                .forAllStates(state -> ConfiguredModel.builder()
                        .modelFile(models().orientable(
                                "wood_oven", // model name
                                modLoc("block/wood_oven_side"), // side texture
                                modLoc("block/wood_oven_front_off"),  // top texture
                                modLoc("block/wood_oven_top")
                        ))
                        .build());

        blockItem(ModBlocks.WOOD_OVEN);
        blockItem(ModBlocks.MIXING_BLOCK);

        blockWithItem(ModBlocks.BLACK_TILE);
        blockWithItem(ModBlocks.DARK_BLUE_TILE);
        blockWithItem(ModBlocks.BLUE_TILE);
        blockWithItem(ModBlocks.DARK_BLUE_WHITE_TILE);
        blockWithItem(ModBlocks.L3E_TILE);
        blockWithItem(ModBlocks.WHITE_TILE);


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
