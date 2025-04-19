package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.MixingBlock;
import net.boulangermod.boulanger.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, String modid, ExistingFileHelper exFileHelper) {
        super(output, Boulanger.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Pine logs
        logBlock((RotatedPillarBlock) ModBlocks.PINE_LOG.get());
        logBlock((RotatedPillarBlock) ModBlocks.STRIPPED_PINE_LOG.get());
        logBlock((RotatedPillarBlock) ModBlocks.STRIPPED_PINE_WOOD.get());
        blockItem(ModBlocks.PINE_LOG);
        blockItem(ModBlocks.STRIPPED_PINE_LOG);
        blockItem(ModBlocks.STRIPPED_PINE_WOOD);
        blockWithItem(ModBlocks.PINE_PLANKS);
        leavesBlock(ModBlocks.PINE_LEAVES);
        saplingBlock(ModBlocks.PINE_SAPLING);

        // Iron Wedge
        getVariantBuilder(ModBlocks.IRON_WEDGE.get())
                .forAllStates(state -> ConfiguredModel.builder()
                        .modelFile(models().getExistingFile(modLoc("block/iron_wedge")))
                        .build()
                );
        simpleBlockItem(ModBlocks.IRON_WEDGE.get(), models().getExistingFile(modLoc("block/iron_wedge")));

        // Stone Milling, Tiles, Kaolinite
        blockWithItem(ModBlocks.STONE_MILL_BLOCK);
        blockWithItem(ModBlocks.KAOLINITE_CLAY);
        blockWithItem(ModBlocks.BLACK_TILE);
        blockWithItem(ModBlocks.BLUE_TILE);
        blockWithItem(ModBlocks.DARK_BLUE_TILE);
        blockWithItem(ModBlocks.DARK_BLUE_WHITE_TILE);
        blockWithItem(ModBlocks.L3E_TILE);
        blockWithItem(ModBlocks.WHITE_TILE);

        // Wild Wheat
        simpleBlock(ModBlocks.WILD_WHEAT.get(), models()
                .cross(ModBlocks.WILD_WHEAT.getId().getPath(), modLoc("block/wild_wheat"))
                .renderType("cutout")
        );

        // Crop (Hard Red Spring Wheat)
        makeCrop((CropBlock) ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get(), "boulanger_wheat_stage", "boulanger_wheat_stage");

        // Wood Oven (directional + lit)
        getVariantBuilder(ModBlocks.WOOD_OVEN.get())
                .forAllStates(state -> {
                    boolean lit = state.getValue(BlockStateProperties.LIT);
                    Direction dir = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    String suffix = lit ? "_on" : "_off";
                    ModelFile file = models().orientable(
                            "wood_oven" + suffix,
                            modLoc("block/wood_oven_side"),
                            modLoc("block/wood_oven_front" + suffix),
                            modLoc("block/wood_oven_top")
                    );
                    return ConfiguredModel.builder()
                            .modelFile(file)
                            .rotationY((int) dir.toYRot())
                            .build();
                });
        simpleBlockItem(ModBlocks.WOOD_OVEN.get(), models().getExistingFile(modLoc("block/wood_oven_off")));

        // Scale Block (custom sides)
        scaleBlockWithCustomSides(ModBlocks.SCALE_BLOCK.get());

        // Mixer Block (directional front)
        String mixerName = ModBlocks.MIXING_BLOCK.getId().getPath();
        ModelFile mixerModel = models().orientable(
                mixerName,
                modLoc("block/mixer_side"),
                modLoc("block/mixer_front"),
                modLoc("block/mixer_top")
        );
        // Item model
        simpleBlockItem(ModBlocks.MIXING_BLOCK.get(), mixerModel);
        // Blockstate variants
        getVariantBuilder(ModBlocks.MIXING_BLOCK.get())
                .forAllStates(state -> {
                    Direction dir = state.getValue(MixingBlock.FACING).getOpposite();
                    int yRot = (int) dir.toYRot();
                    return ConfiguredModel.builder()
                            .modelFile(mixerModel)
                            .rotationY(yRot)
                            .build();
                });
    }

    private void leavesBlock(DeferredBlock<Block> block) {
        simpleBlockWithItem(block.get(), models().singleTexture(
                BuiltInRegistries.BLOCK.getKey(block.get()).getPath(),
                ResourceLocation.fromNamespaceAndPath("minecraft", "block/leaves"),
                "all", blockTexture(block.get())
        ).renderType("cutout"));
    }

    private void saplingBlock(DeferredBlock<Block> block) {
        simpleBlock(block.get(), models().cross(
                BuiltInRegistries.BLOCK.getKey(block.get()).getPath(),
                blockTexture(block.get())
        ).renderType("cutout"));
    }

    private void blockWithItem(DeferredBlock<Block> block) {
        simpleBlockWithItem(block.get(), cubeAll(block.get()));
    }

    private void blockItem(DeferredBlock<Block> block) {
        simpleBlockItem(block.get(), new ModelFile.UncheckedModelFile(
                Boulanger.MODID + ":block/" + block.getId().getPath()
        ));
    }

    private void makeCrop(CropBlock block, String modelBase, String texBase) {
        int max = block.getMaxAge();
        Map<Integer, ModelFile> map = new LinkedHashMap<>();
        for (int age = 0; age <= max; age++) {
            String name = modelBase + age;
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(
                    Boulanger.MODID,
                    "block/wheat/" + texBase + age
            );
            map.put(age, models().crop(name, tex).renderType("cutout"));
        }
        getVariantBuilder(block).forAllStates(state -> {
            int age = block.getAge(state);
            ModelFile file = map.get(age);
            return ConfiguredModel.builder()
                    .modelFile(file)
                    .build();
        });
    }

    /**
     * Custom scale block with side/top/bottom textures.
     */
    public void scaleBlockWithCustomSides(Block block) {
        String name = block.builtInRegistryHolder().key().location().getPath();
        ModelFile model = models().cubeBottomTop(
                name,
                modLoc("block/scale_side"),
                modLoc("block/pine_planks"),
                modLoc("block/scale_top")
        ).texture("particle", modLoc("block/scale_side"));
        simpleBlock(block, model);
    }
}
