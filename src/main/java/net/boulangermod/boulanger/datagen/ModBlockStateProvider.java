package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.*;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, String modid, ExistingFileHelper exFileHelper) {
        super(output, Boulanger.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // ─── PINE LOGS & WOOD ────────────────────────────────────────────────
        logBlock((RotatedPillarBlock) ModBlocks.PINE_LOG.get());
        simpleBlockItem(ModBlocks.PINE_LOG.get(),
                models().getExistingFile(modLoc("block/pine_log")));

        logBlock((RotatedPillarBlock) ModBlocks.STRIPPED_PINE_LOG.get());
        simpleBlockItem(ModBlocks.STRIPPED_PINE_LOG.get(),
                models().getExistingFile(modLoc("block/stripped_pine_log")));

        logBlock((RotatedPillarBlock) ModBlocks.STRIPPED_PINE_WOOD.get());
        simpleBlockItem(ModBlocks.STRIPPED_PINE_WOOD.get(),
                models().getExistingFile(modLoc("block/stripped_pine_wood")));

        axisBlock(
                (RotatedPillarBlock) ModBlocks.PINE_WOOD.get(),
                blockTexture(ModBlocks.PINE_LOG.get()),
                blockTexture(ModBlocks.PINE_LOG.get())
        );
        simpleBlockItem(ModBlocks.PINE_WOOD.get(),
                models().getExistingFile(modLoc("block/pine_wood")));

        // ─── PINE PLANKS, STAIRS, SLAB, FENCES ───────────────────────────────
        // Planks as a simple cube + item
        simpleBlockWithItem(ModBlocks.PINE_PLANKS.get(), cubeAll(ModBlocks.PINE_PLANKS.get()));

        // Stairs (auto‐generates all facing/shape/states)
        stairsBlock(
                (StairBlock) ModBlocks.PINE_STAIRS.get(),
                blockTexture(ModBlocks.PINE_PLANKS.get())
        );

        // Slab (single + double)
        slabBlock(
                (SlabBlock) ModBlocks.PINE_SLAB.get(),
                blockTexture(ModBlocks.PINE_PLANKS.get()),
                blockTexture(ModBlocks.PINE_PLANKS.get())
        );

        // Fence + Gate
        fenceBlock(
                (FenceBlock) ModBlocks.PINE_FENCE.get(),
                blockTexture(ModBlocks.PINE_PLANKS.get())
        );
        fenceGateBlock(
                (FenceGateBlock) ModBlocks.PINE_FENCE_GATE.get(),
                blockTexture(ModBlocks.PINE_PLANKS.get())
        );

        // ─── LEAVES & SAPLING ─────────────────────────────────────────────────
        leavesBlock(ModBlocks.PINE_LEAVES);
        saplingBlock(ModBlocks.PINE_SAPLING);

        // ─── IRON WEDGE ───────────────────────────────────────────────────────
        getVariantBuilder(ModBlocks.IRON_WEDGE.get())
                .forAllStates(s -> ConfiguredModel.builder()
                        .modelFile(models().getExistingFile(modLoc("block/iron_wedge")))
                        .build()
                );
        simpleBlockItem(ModBlocks.IRON_WEDGE.get(),
                models().getExistingFile(modLoc("block/iron_wedge"))
        );


        blockWithItem(ModBlocks.PROOFING_BOX);
        blockWithItem(ModBlocks.BAKERS_TABLE);
        blockWithItem(ModBlocks.DOUGH_DIVIDER);



        // ─── STONE MILL & TILES ───────────────────────────────────────────────
        blockWithItem(ModBlocks.STONE_MILL_BLOCK);
        blockWithItem(ModBlocks.KAOLINITE_CLAY);
        blockWithItem(ModBlocks.BLACK_TILE);
        blockWithItem(ModBlocks.BLUE_TILE);
        blockWithItem(ModBlocks.DARK_BLUE_TILE);
        blockWithItem(ModBlocks.DARK_BLUE_WHITE_TILE);
        blockWithItem(ModBlocks.L3E_TILE);
        blockWithItem(ModBlocks.WHITE_TILE);
        scaleBlockWithCustomSides(ModBlocks.SCALE_BLOCK.get());

        // ─── WILD WHEAT & CROPS ───────────────────────────────────────────────
        simpleBlock(
                ModBlocks.WILD_WHEAT.get(),
                models().cross(
                        ModBlocks.WILD_WHEAT.getId().getPath(),
                        modLoc("block/wild_wheat")
                ).renderType("cutout")
        );
        simpleBlockItem(ModBlocks.WILD_WHEAT.get(),
                models().getExistingFile(modLoc("block/wild_wheat"))
        );

        makeCrop(
                (CropBlock) ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get(),
                "boulanger_wheat_stage",
                "boulanger_wheat_stage"
        );
        simpleBlockItem(ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get(),
                models().getExistingFile(modLoc("block/boulanger_wheat_stage0"))
        );

        // ─── WOOD OVEN ─────────────────────────────────────────────────────────
        getVariantBuilder(ModBlocks.WOOD_OVEN.get())
                .forAllStates(s -> {
                    boolean lit = s.getValue(BlockStateProperties.LIT);
                    Direction dir = s.getValue(BlockStateProperties.HORIZONTAL_FACING);
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
        simpleBlockItem(ModBlocks.WOOD_OVEN.get(),
                models().getExistingFile(modLoc("block/wood_oven_off"))
        );

        // ─── MIXING BLOCK ──────────────────────────────────────────────────────
        String mixerName = ModBlocks.MIXING_BLOCK.getId().getPath();
        ModelFile mixerModel = models().orientable(
                mixerName,
                modLoc("block/diorite_side"),
                modLoc("block/diorite_copper_back"),
                modLoc("block/mixer_top")
        );
        simpleBlockItem(ModBlocks.MIXING_BLOCK.get(), mixerModel);
        getVariantBuilder(ModBlocks.MIXING_BLOCK.get())
                .forAllStates(s -> {
                    Direction dir = s.getValue(MixingBlock.FACING).getOpposite();
                    return ConfiguredModel.builder()
                            .modelFile(mixerModel)
                            .rotationY((int) dir.toYRot())
                            .build();
                });

        // ─── TREE TAP ─────────────────────────────────────────────────────────
        ModelFile tapModel = models().getExistingFile(modLoc("block/tree_tap"));
        simpleBlockItem(ModBlocks.TREE_TAP.get(), tapModel);
        getVariantBuilder(ModBlocks.TREE_TAP.get())
                .forAllStates(s -> {
                    Direction dir = s.getValue(TreeTapBlock.FACING);
                    return ConfiguredModel.builder()
                            .modelFile(tapModel)
                            .rotationY((int) dir.toYRot())
                            .build();
                });

        // ─── WOOD GASIFIER ─────────────────────────────────────────────────────
        ModelFile gasifierModel = models().getExistingFile(modLoc("block/wood_gasifier"));
        simpleBlockItem(ModBlocks.WOOD_GASIFIER.get(), gasifierModel);
        getVariantBuilder(ModBlocks.WOOD_GASIFIER.get())
                .forAllStates(s -> {
                    Direction dir = s.getValue(WoodGasifierBlock.FACING);
                    return ConfiguredModel.builder()
                            .modelFile(gasifierModel)
                            .rotationY((int) dir.toYRot())
                            .build();
                });

        ResourceLocation bedrockTex = ResourceLocation.fromNamespaceAndPath("minecraft", "block/bedrock");

// 2) Generate one shared cube-all model named “energy_bedrock”
        ModelFile energyModel = models().cubeAll(
                "energy_bedrock",
                bedrockTex
        );

// 3) ENERGY CABLE: full-cube Bedrock for both blockstate & item
        simpleBlockWithItem(
                ModBlocks.ENERGY_CABLE.get(),
                energyModel
        );

// 4) ENERGY STORAGE (“Battery”):
//    – item uses the same Bedrock cube
        simpleBlockItem(
                ModBlocks.BATTERY.get(),
                energyModel
        );
//    – blockstate maps ALL (lit/unlit) variants to the same model
        getVariantBuilder(ModBlocks.BATTERY.get())
                .forAllStates(s -> ConfiguredModel.builder()
                        .modelFile(energyModel)
                        .build()
                );
        // ————————————————————————————————————————————————————————————————————————

        ResourceLocation coalTex = ResourceLocation.fromNamespaceAndPath("minecraft", "block/coal_block");

// Models
        ModelFile endModel      = models().getExistingFile(modLoc("block/woodgas_pipe_end"));
        ModelFile straightModel = models().getExistingFile(modLoc("block/woodgas_pipe_straight"));
        ModelFile cornerModel   = models().getExistingFile(modLoc("block/woodgas_pipe_north_bend"));
        ModelFile tModel        = models().getExistingFile(modLoc("block/woodgas_pipe_t"));
        ModelFile crossModel    = models().getExistingFile(modLoc("block/woodgas_pipe_cross"));

        var pipe = getMultipartBuilder(ModBlocks.WOODGAS_PIPE.get());

        // 4-way “+”
        pipe.part()
                .modelFile(crossModel)
                .addModel()
                .condition(WoodGasPipe.NORTH, true)
                .condition(WoodGasPipe.SOUTH, true)
                .condition(WoodGasPipe.EAST,  true)
                .condition(WoodGasPipe.WEST,  true)
                .end();

        // 3-way Ts
        int[] tsY = {0, 90, 180, 270};
        BooleanProperty[][] tsConds = {
                {WoodGasPipe.NORTH, WoodGasPipe.SOUTH, WoodGasPipe.EAST},
                {WoodGasPipe.SOUTH, WoodGasPipe.EAST,  WoodGasPipe.WEST},
                {WoodGasPipe.EAST,  WoodGasPipe.WEST,  WoodGasPipe.NORTH},
                {WoodGasPipe.WEST,  WoodGasPipe.NORTH, WoodGasPipe.SOUTH}
        };
        for (int i = 0; i < 4; i++) {
            pipe.part()
                    .modelFile(tModel).rotationY(tsY[i])
                    .addModel()
                    .condition(tsConds[i][0], true)
                    .condition(tsConds[i][1], true)
                    .condition(tsConds[i][2], true)
                    .end();
        }

        // 2-way straights
        pipe.part()
                .modelFile(straightModel)
                .addModel()
                .condition(WoodGasPipe.NORTH, true)
                .condition(WoodGasPipe.SOUTH, true)
                .end();
        pipe.part()
                .modelFile(straightModel).rotationY(90)
                .addModel()
                .condition(WoodGasPipe.EAST, true)
                .condition(WoodGasPipe.WEST, true)
                .end();

        // 2-way corners
        int[] crY = {0, 90, 180, 270};
        BooleanProperty[][] crConds = {
                {WoodGasPipe.NORTH, WoodGasPipe.EAST},
                {WoodGasPipe.EAST,  WoodGasPipe.SOUTH},
                {WoodGasPipe.SOUTH, WoodGasPipe.WEST},
                {WoodGasPipe.WEST,  WoodGasPipe.NORTH}
        };
        for (int i = 0; i < 4; i++) {
            pipe.part()
                    .modelFile(cornerModel).rotationY(crY[i])
                    .addModel()
                    .condition(crConds[i][0], true)
                    .condition(crConds[i][1], true)
                    .end();
        }

        // 1-way ends
        Direction[] ends = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN};
        int[] rotY =    {0, 90,  180,    270,    0,   0};
        int[] rotX =    {0, 0,    0,      0,     90, 270};
        for (int i = 0; i < ends.length; i++) {
            pipe.part()
                    .modelFile(endModel)
                    .rotationY(rotY[i])
                    .rotationX(rotX[i])
                    .addModel()
                    .condition(getProp(ends[i]), true)
                    .end();
        }

        // Default item model
        simpleBlockItem(ModBlocks.WOODGAS_PIPE.get(),
                models().getExistingFile(modLoc("block/woodgas_pipe_straight")));

        // Internal Combustion Engine
        ModelFile engineModel = models().cubeAll("internal_combustion_engine", coalTex);
        simpleBlockWithItem(
                ModBlocks.INTERAL_COMUSTION_ENGINE.get(),
                engineModel
        );
        }


    private static BooleanProperty getProp(Direction dir) {
        return switch (dir) {
            case NORTH -> WoodGasPipe.NORTH;
            case EAST  -> WoodGasPipe.EAST;
            case SOUTH -> WoodGasPipe.SOUTH;
            case WEST  -> WoodGasPipe.WEST;
            case UP    -> WoodGasPipe.UP;
            case DOWN  -> WoodGasPipe.DOWN;
        };

    }
    private void leavesBlock(DeferredBlock<Block> block) {
        simpleBlockWithItem(block.get(),
                models().singleTexture(
                        BuiltInRegistries.BLOCK.getKey(block.get()).getPath(),
                        ResourceLocation.fromNamespaceAndPath("minecraft","block/leaves"),
                        "all",
                        blockTexture(block.get())
                ).renderType("cutout")
        );
    }

    private void saplingBlock(DeferredBlock<Block> block) {
        simpleBlock(block.get(),
                models().cross(
                        BuiltInRegistries.BLOCK.getKey(block.get()).getPath(),
                        blockTexture(block.get())
                ).renderType("cutout")
        );
    }

    private void makeCrop(CropBlock block, String modelBase, String texBase) {
        Map<Integer, ModelFile> ageModels = new LinkedHashMap<>();
        for (int age = 0; age <= block.getMaxAge(); age++) {
            String name = modelBase + age;
            ResourceLocation tex = modLoc("block/wheat/" + texBase + age);
            ageModels.put(age,
                    models().crop(name, tex)
                            .renderType("cutout")
            );
        }

        getVariantBuilder(block).forAllStates(s -> {
            int age = block.getAge(s);
            return ConfiguredModel.builder()
                    .modelFile(ageModels.get(age))
                    .build();
        });
    }

    public void scaleBlockWithCustomSides(Block block) {
        String name = block.builtInRegistryHolder().key().location().getPath();
        ModelFile model = models().cubeBottomTop(
                name,
                modLoc("block/diorite_side"),
                modLoc("block/pine_planks"),
                modLoc("block/scale_top")
        ).texture("particle", modLoc("block/diorite_side"));
        simpleBlock(block, model);
    }



    private void blockWithItem(DeferredBlock<? extends Block> block) {
        simpleBlockWithItem(block.get(), cubeAll(block.get()));
    }
}
