package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.*;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
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

        // textures
        ResourceLocation side = modLoc("block/pine_log");
        ResourceLocation sideResin = modLoc("block/resin_pine_log");
        ResourceLocation end = modLoc("block/pine_log_top");

        // models
        ModelFile pineLog = models().cubeColumn("pine_log", side, end);
        ModelFile pineLogResin = models().cubeColumn("resin_pine_log", sideResin, end);

        // variants: resin_remaining==0 -> pineLog, >0 -> pineLogResin
        getVariantBuilder(ModBlocks.PINE_LOG.get())
                .forAllStatesExcept(state -> {
                    int remaining = state.getValue(PineResinLogBlock.RESIN_REMAINING);
                    Direction.Axis axis = state.getValue(RotatedPillarBlock.AXIS);
                    ModelFile pick = (remaining == 0) ? pineLog : pineLogResin;

                    var b = ConfiguredModel.builder().modelFile(pick);
                    if (axis == Direction.Axis.X) b.rotationX(90).rotationY(90);
                    else if (axis == Direction.Axis.Z) b.rotationX(90);
                    return b.build();
                }, PineResinLogBlock.HAS_RESIN);

        simpleBlockItem(ModBlocks.PINE_LOG.get(), pineLog);

        // Stripped pine log
        logBlock((RotatedPillarBlock) ModBlocks.STRIPPED_PINE_LOG.get());
        simpleBlockItem(ModBlocks.STRIPPED_PINE_LOG.get(),
                models().getExistingFile(modLoc("block/stripped_pine_log")));

        // Stripped pine wood (bark on all faces)
        axisBlock(
                (RotatedPillarBlock) ModBlocks.STRIPPED_PINE_WOOD.get(),
                blockTexture(ModBlocks.STRIPPED_PINE_LOG.get()),
                blockTexture(ModBlocks.STRIPPED_PINE_LOG.get())
        );

        // Matching item (cube-all) for stripped pine wood
        simpleBlockItem(
                ModBlocks.STRIPPED_PINE_WOOD.get(),
                models().cubeAll("stripped_pine_wood", blockTexture(ModBlocks.STRIPPED_PINE_LOG.get()))
        );

        // Pine wood (bark on all faces) uses the regular pine_log texture for all faces
        axisBlock(
                (RotatedPillarBlock) ModBlocks.PINE_WOOD.get(),
                blockTexture(ModBlocks.PINE_LOG.get()),
                blockTexture(ModBlocks.PINE_LOG.get())
        );
        simpleBlockItem(
                ModBlocks.PINE_WOOD.get(),
                models().getExistingFile(modLoc("block/pine_wood"))
        );

        // ─── PINE PLANKS, STAIRS, SLAB, FENCES ───────────────────────────────
        simpleBlockWithItem(ModBlocks.PINE_PLANKS.get(), cubeAll(ModBlocks.PINE_PLANKS.get()));

        stairsBlock(
                (StairBlock) ModBlocks.PINE_STAIRS.get(),
                blockTexture(ModBlocks.PINE_PLANKS.get())
        );

        slabBlock(
                (SlabBlock) ModBlocks.PINE_SLAB.get(),
                blockTexture(ModBlocks.PINE_PLANKS.get()),
                blockTexture(ModBlocks.PINE_PLANKS.get())
        );

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

        // ─── PRESSURE PLATE ───────────────────────────────────────────────────
        {
            PressurePlateBlock plate = (PressurePlateBlock) ModBlocks.PINE_PRESSURE_PLATE.get();
            String n = BuiltInRegistries.BLOCK.getKey(plate).getPath();
            ResourceLocation tex = blockTexture(ModBlocks.PINE_PLANKS.get());

            ModelFile up = models().pressurePlate(n + "_up", tex);
            ModelFile down = models().pressurePlateDown(n + "_down", tex);

            getVariantBuilder(plate).forAllStates(s ->
                    ConfiguredModel.builder()
                            .modelFile(s.getValue(PressurePlateBlock.POWERED) ? down : up)
                            .build()
            );

            itemModels().withExistingParent(n, modLoc("block/" + n + "_up"));
        }

        buttonBlock(((ButtonBlock) ModBlocks.PINE_BUTTON.get()), blockTexture(ModBlocks.PINE_PLANKS.get()));

        // ─── TRAPDOOR ─────────────────────────────────────────────────────────
        trapdoorBlockWithRenderType(
                (TrapDoorBlock) ModBlocks.PINE_TRAPDOOR.get(),
                modLoc("block/pine_trapdoor"),
                true,
                "cutout"
        );
        simpleBlockItem(
                ModBlocks.PINE_TRAPDOOR.get(),
                models().getExistingFile(modLoc("block/pine_trapdoor_bottom"))
        );

        // ─── DOOR (cutout) ───────────────────────────────────────────────────
        {
            DoorBlock door = (DoorBlock) ModBlocks.PINE_DOOR.get();
            doorBlockWithRenderType(door,
                    modLoc("block/pine_door_bottom"),
                    modLoc("block/pine_door_top"),
                    "cutout");
        }
        itemModels().basicItem(ModBlocks.PINE_DOOR.get().asItem());

        // ─── IRON WEDGE ───────────────────────────────────────────────────────
        getVariantBuilder(ModBlocks.IRON_WEDGE.get())
                .forAllStates(s -> ConfiguredModel.builder()
                        .modelFile(models().getExistingFile(modLoc("block/iron_wedge")))
                        .build()
                );
        simpleBlockItem(ModBlocks.IRON_WEDGE.get(),
                models().getExistingFile(modLoc("block/iron_wedge"))
        );

        horizontal(ModBlocks.MIXING_BLOCK.get(), "mixing_block");
        horizontal(ModBlocks.PROOFING_BOX.get(), "proofing_box");
        horizontal(ModBlocks.DOUGH_DIVIDER.get(), "dough_divider");

        // Stone Mill
        {
            ModelFile stoneMill = models().getExistingFile(modLoc("block/stone_mill_block"));
            horizontalBlock(ModBlocks.STONE_MILL_BLOCK.get(), stoneMill);
            simpleBlockItem(ModBlocks.STONE_MILL_BLOCK.get(), stoneMill);
        }

        // ─── SCALE (custom model 'scale1') ────────────────────────────────────
        {
            ModelFile scale = models().getExistingFile(modLoc("block/scale1"));
            getVariantBuilder(ModBlocks.SCALE_BLOCK.get())
                    .forAllStates(s -> {
                        Direction dir = s.getValue(BlockStateProperties.HORIZONTAL_FACING);
                        int rotY = (int) dir.toYRot(); // no +180
                        return ConfiguredModel.builder()
                                .modelFile(scale)
                                .rotationY(rotY)
                                .build();
                    });
        }

        // ─── SIMPLE CUBES ─────────────────────────────────────────────────────
        blockWithItem(ModBlocks.KAOLINITE_CLAY);
        blockWithItem(ModBlocks.BLACK_TILE);
        blockWithItem(ModBlocks.LIGHT_BLUE_TILE);
        blockWithItem(ModBlocks.BLUE_TILE);
        blockWithItem(ModBlocks.BLUE_WHITE_TILE);
        blockWithItem(ModBlocks.L3E_TILE);
        blockWithItem(ModBlocks.WHITE_TILE);
        blockWithItem(ModBlocks.MACHINE_HOUSING);

        // ─── SUGAR REFINERY (multipart: core + 3 arms; cutout) ───────────────
        ModelFile core = models()
                .withExistingParent("sugar_refinery_core", mcLoc("block/brewing_stand"))
                .texture("base", mcLoc("block/brewing_stand_base"))
                .texture("stand", modLoc("block/sugar_refinery"))
                .texture("particle", modLoc("block/sugar_refinery"))
                .renderType("cutout");

        ModelFile arm0 = models()
                .withExistingParent("sugar_refinery_empty0", mcLoc("block/brewing_stand_empty0"))
                .texture("stand", modLoc("block/sugar_refinery"))
                .renderType("cutout");
        ModelFile arm1 = models()
                .withExistingParent("sugar_refinery_empty1", mcLoc("block/brewing_stand_empty1"))
                .texture("stand", modLoc("block/sugar_refinery"))
                .renderType("cutout");
        ModelFile arm2 = models()
                .withExistingParent("sugar_refinery_empty2", mcLoc("block/brewing_stand_empty2"))
                .texture("stand", modLoc("block/sugar_refinery"))
                .renderType("cutout");

        getMultipartBuilder(ModBlocks.SUGAR_REFINERY.get())
                .part().modelFile(core).addModel().end()
                .part().modelFile(arm0).addModel().end()
                .part().modelFile(arm1).addModel().end()
                .part().modelFile(arm2).addModel().end();

        simpleBlockItem(
                ModBlocks.SUGAR_REFINERY.get(),
                models()
                        .withExistingParent("sugar_refinery_item", mcLoc("item/brewing_stand"))
                        .texture("base", mcLoc("block/brewing_stand_base"))
                        .texture("stand", modLoc("block/sugar_refinery"))
                        .texture("particle", modLoc("block/sugar_refinery"))
                        .renderType("cutout")
        );

        // ─── IRON FRAME (stages) ──────────────────────────────────────────────
        var builder = getVariantBuilder(ModBlocks.IRON_FRAME.get());
        for (int stage = 0; stage <= IronFrameBlock.MAX_STAGE; stage++) {
            ModelFile model = models().getExistingFile(modLoc("block/iron_frame_" + stage));
            builder.partialState()
                    .with(IronFrameBlock.STAGE, stage)
                    .modelForState()
                    .modelFile(model)
                    .addModel();
        }
        itemModels().withExistingParent("iron_frame", modLoc("block/iron_frame_0"));

        // ─── MOTIVATOR ────────────────────────────────────────────────────────
        String name = "motivator";
        ModelFile motivatorModel = models().cubeBottomTop(
                name,
                modLoc("block/motivator_side"),
                modLoc("block/motivator_bottom"),
                modLoc("block/motivator_top")
        );
        simpleBlock(ModBlocks.MOTIVATOR.get(), motivatorModel);
        itemModels().withExistingParent(name, modLoc("block/" + name));

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
                (CropBlock) ModBlocks.WHEAT_BUSHEL_BLOCK.get(),
                "boulanger_wheat_stage",
                "boulanger_wheat_stage"
        );
        simpleBlockItem(ModBlocks.WHEAT_BUSHEL_BLOCK.get(),
                models().getExistingFile(modLoc("block/boulanger_wheat_stage0"))
        );

        // ─── WOOD OVEN (custom ON/OFF) ────────────────────────────────────────
        {
            ModelFile ovenOff = models().getExistingFile(modLoc("block/wood_oven_off"));
            ModelFile ovenOn  = models().getExistingFile(modLoc("block/wood_oven_on"));

            getVariantBuilder(ModBlocks.WOOD_OVEN.get())
                    .forAllStates(s -> {
                        Direction dir = s.getValue(BlockStateProperties.HORIZONTAL_FACING);
                        boolean lit   = s.getValue(BlockStateProperties.LIT);
                        int rotY = (int) dir.toYRot();
                        return ConfiguredModel.builder()
                                .modelFile(lit ? ovenOn : ovenOff)
                                .rotationY(rotY)
                                .build();
                    });
        }

        // ─── BAKER'S TABLE ────────────────────────────────────────────────────
        ModelFile bakersTableModel = models().orientable(
                "bakers_table",
                modLoc("block/bakers_table_side"),
                modLoc("block/bakers_table_front"),
                modLoc("block/bakers_table_top")
        );
        horizontalBlock(ModBlocks.BAKERS_TABLE.get(), bakersTableModel);
        simpleBlockItem(ModBlocks.BAKERS_TABLE.get(), bakersTableModel);

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

        // ─── WOOD GASIFIER (formed/hidden) ────────────────────────────────────
        ModelFile base = models().cubeAll("wood_gasifier_base", modLoc("block/burnished_steel"));
        ModelFile big  = models().getExistingFile(modLoc("block/wood_gasifier"));

        getVariantBuilder(ModBlocks.WOOD_GASIFIER.get())
                .forAllStates(st -> {
                    Direction dir   = st.getValue(WoodGasifierBlock.FACING);
                    boolean formed  = st.getValue(WoodGasifierBlock.FORMED);
                    boolean hidden  = st.getValue(WoodGasifierBlock.HIDDEN);
                    ModelFile file  = (formed && !hidden) ? big : base;

                    return ConfiguredModel.builder()
                            .modelFile(file)
                            .rotationY((int) dir.toYRot())
                            .uvLock(true)
                            .build();
                });

        // Item = base cube
        itemModels().withExistingParent(
                BuiltInRegistries.BLOCK.getKey(ModBlocks.WOOD_GASIFIER.get()).getPath(),
                modLoc("block/wood_gasifier_base")
        );



        // ─── BATTERY ──────────────────────────────────────────────────────────
        {
            ResourceLocation texIn  = modLoc("block/battery_in");
            ResourceLocation texOut = modLoc("block/battery_out");

            ModelFile batteryModel = models().cube(
                    "battery",
                    texIn,   // down  (INPUT)
                    texOut,  // up    (OUTPUT)
                    texOut,  // north
                    texOut,  // south
                    texOut,  // west
                    texOut   // east
            );

            simpleBlock(ModBlocks.BATTERY.get(), batteryModel);
            simpleBlockItem(ModBlocks.BATTERY.get(), batteryModel);
        }

        // ─── NETWORK PIECES ───────────────────────────────────────────────────
        woodgasPipeStates(ModBlocks.WOODGAS_PIPE);
        energyCableStates(ModBlocks.ENERGY_CABLE);
        woodgasFlareStates();
        woodgasValveStates();
        //woodgasValveCoreOnly();




        // ─── WOODGAS ENGINE (use custom ON/OFF models; swap by LIT) ────────────────
        {
            ModelFile engineOff = models().getExistingFile(modLoc("block/woodgas_engine_off"));
            ModelFile engineOn  = models().getExistingFile(modLoc("block/woodgas_engine_on"));

            getVariantBuilder(ModBlocks.WOODGAS_ENGINE.get())
                    .forAllStates(s -> {
                        Direction dir = s.getValue(AbstractProcessingBlock.FACING);
                        boolean lit   = s.getValue(WoodGasEngineBlock.LIT);

                        // 180° flip so the custom model’s “front” matches the block’s FACING
                        int rotY = (((int) dir.toYRot()) + 180) % 360;

                        return ConfiguredModel.builder()
                                .modelFile(lit ? engineOn : engineOff)
                                .rotationY(rotY)
                                .build();
                    });
            woodGasTankStates();
        }

        // ─── FEED-THROUGH (default look = vanilla chiseled stone bricks) ──────
        {
            String id = BuiltInRegistries.BLOCK.getKey(ModBlocks.FEED_THROUGH_BLOCK.get()).getPath();
            ModelFile feedthroughModel = models().cubeAll(id, mcLoc("block/chiseled_stone_bricks"));
            simpleBlock(ModBlocks.FEED_THROUGH_BLOCK.get(), feedthroughModel);
            simpleBlockItem(ModBlocks.FEED_THROUGH_BLOCK.get(), feedthroughModel);
        }
    }

    // ───────────────────────── Helpers ─────────────────────────

    public void woodGasTankStates() {
        ModelFile full  = models().getExistingFile(modLoc("block/gas_tank"));
        ModelFile empty = models().getBuilder("gas_tank_empty")
                .parent(models().getExistingFile(mcLoc("block/block"))); // renders nothing

        getVariantBuilder(ModBlocks.GAS_TANK.get()).forAllStates(state -> {
            var facing = state.getValue(WoodGasTankBlock.FACING);
            var half   = state.getValue(WoodGasTankBlock.HALF);

            ModelFile model = (half == DoubleBlockHalf.LOWER) ? full : empty;

            int yRot = switch (facing) {
                case SOUTH -> 180;
                case WEST  -> 270;
                case EAST  -> 90;
                default    -> 0;
            };

            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationY(yRot)
                    .build();
        });
    }

    private void woodgasFlareStates() {
        ModelFile flare = models()
                .withExistingParent("woodgas_flare_rt", modLoc("block/woodgas_flare"))
                .renderType("cutout");

        getVariantBuilder(ModBlocks.WOODGAS_FLARE.get())
                .forAllStatesExcept(state -> {
                    Direction f = state.getValue(WoodGasFlareBlock.FACING);

                    int xRot = 0, yRot = 0;
                    switch (f) {
                        case UP -> {               xRot =   0; yRot =   0; }
                        case DOWN -> {             xRot = 180; yRot =   0; }
                        case NORTH -> {            xRot =  90; yRot =   0; }
                        case SOUTH -> {            xRot =  90; yRot = 180; }
                        case EAST -> {             xRot =  90; yRot =  90; }
                        case WEST -> {             xRot =  90; yRot = 270; }
                    }

                    return ConfiguredModel.builder()
                            .modelFile(flare)
                            .rotationX(xRot)
                            .rotationY(yRot)
                            .uvLock(true)
                            .build();
                }, WoodGasFlareBlock.LIT);

        simpleBlockItem(ModBlocks.WOODGAS_FLARE.get(), flare);
    }

    // WOODGAS VALVE (core = on/off by OPEN; arms always; cutout)
    private void woodgasValveStates() {
        Block valve = ModBlocks.WOODGAS_VALVE.get();

        // Core models (force cutout)
        ModelFile coreOn = models()
                .withExistingParent("woodgas_valve_on_rt", modLoc("block/woodgas_valve"))
                .renderType("cutout");
        ModelFile coreOff = models()
                .withExistingParent("woodgas_valve_off_rt", modLoc("block/woodgas_valve_closed"))
                .renderType("cutout");

        // Arm model (force cutout)
        ModelFile arm = models()
                .withExistingParent("woodgas_pipe_arm_rt", modLoc("block/woodgas_pipe_arm"))
                .renderType("cutout");

        MultiPartBlockStateBuilder b = getMultipartBuilder(valve);

        // Core: render ON or OFF per-facing + OPEN flag
        for (Direction f : Direction.values()) {
            int x = (f == Direction.UP) ? -90 : (f == Direction.DOWN) ? 90 : 0;
            int y = f.getAxis().isHorizontal() ? (int) f.toYRot() : 0;

            // OPEN = true → on model
            b.part()
                    .modelFile(coreOn)
                    .rotationX(x).rotationY(y)
                    .uvLock(true)
                    .addModel()
                    .condition(WoodGasValveBlock.FACING, f)
                    .condition(WoodGasValveBlock.OPEN, true);

            // OPEN = false → off model
            b.part()
                    .modelFile(coreOff)
                    .rotationX(x).rotationY(y)
                    .uvLock(true)
                    .addModel()
                    .condition(WoodGasValveBlock.FACING, f)
                    .condition(WoodGasValveBlock.OPEN, false);
        }

        // Arms: add per-side boolean (flipped 180° around Y as requested)
        addValveArm(b, arm, Direction.NORTH, WoodGasValveBlock.NORTH);
        addValveArm(b, arm, Direction.SOUTH, WoodGasValveBlock.SOUTH);
        addValveArm(b, arm, Direction.WEST,  WoodGasValveBlock.WEST);
        addValveArm(b, arm, Direction.EAST,  WoodGasValveBlock.EAST);
        addValveArm(b, arm, Direction.UP,    WoodGasValveBlock.UP);
        addValveArm(b, arm, Direction.DOWN,  WoodGasValveBlock.DOWN);
    }

    // Flip each arm 180° yaw so it faces the opposite way
    private void addValveArm(MultiPartBlockStateBuilder b, ModelFile arm, Direction dir, BooleanProperty prop) {
        int x = (dir == Direction.UP) ? -90 : (dir == Direction.DOWN) ? 90 : 0;
        int y = dir.getAxis().isHorizontal() ? (int) dir.toYRot() : 0;
        y = (y + 180) % 360; // flip

        b.part()
                .modelFile(arm)
                .rotationX(x).rotationY(y)
                .uvLock(true)
                .addModel()
                .condition(prop, true);
    }

    private void woodgasPipeStates(DeferredBlock<? extends Block> pipeBlock) {
        Block b = pipeBlock.get();
        var m = getMultipartBuilder(b);

        final BooleanProperty N = WoodGasPipe.NORTH;
        final BooleanProperty E = WoodGasPipe.EAST;
        final BooleanProperty S = WoodGasPipe.SOUTH;
        final BooleanProperty W = WoodGasPipe.WEST;
        final BooleanProperty U = WoodGasPipe.UP;
        final BooleanProperty D = WoodGasPipe.DOWN;

        ModelFile isolated  = models().getExistingFile(modLoc("block/woodgas_pipe_isolated"));
        ModelFile arm       = models().getExistingFile(modLoc("block/woodgas_pipe_arm"));
        ModelFile straightH = models().getExistingFile(modLoc("block/woodgas_pipe_h")); // base N–S
        ModelFile straightV = models().getExistingFile(modLoc("block/woodgas_pipe_v")); // base U–D

        // 0-connections → isolated
        m.part().modelFile(isolated).uvLock(true).addModel()
                .condition(N,false).condition(E,false).condition(S,false)
                .condition(W,false).condition(U,false).condition(D,false);

        // Enumerate all other states (1..63)
        for (int mask = 1; mask < 64; mask++) {
            boolean n = (mask & 1)  != 0;
            boolean e = (mask & 2)  != 0;
            boolean s = (mask & 4)  != 0;
            boolean w = (mask & 8)  != 0;
            boolean u = (mask & 16) != 0;
            boolean d = (mask & 32) != 0;

            boolean isNS = n && s && !e && !w && !u && !d;
            boolean isEW = e && w && !n && !s && !u && !d;
            boolean isUD = u && d && !n && !e && !s && !w;

            // Pure straights: render full model only
            if (isNS) { partExactPipe(m, straightH, n,e,s,w,u,d, 0,  0);  continue; }
            if (isEW) { partExactPipe(m, straightH, n,e,s,w,u,d, 0, 90);  continue; }
            if (isUD) { partExactPipe(m, straightV, n,e,s,w,u,d, 0,  0);  continue; }

            // All other cases: center (use isolated) + arms for each true side
            partExactPipe(m, isolated, n,e,s,w,u,d, 0, 0);
            if (n) partExactPipe(m, arm, n,e,s,w,u,d, 0,   0);
            if (e) partExactPipe(m, arm, n,e,s,w,u,d, 0,  90);
            if (s) partExactPipe(m, arm, n,e,s,w,u,d, 0, 180);
            if (w) partExactPipe(m, arm, n,e,s,w,u,d, 0, 270);
            if (u) partExactPipe(m, arm, n,e,s,w,u,d, -90, 0);
            if (d) partExactPipe(m, arm, n,e,s,w,u,d,  90, 0);
        }
    }

    // Call this from registerStatesAndModels()
    private void energyCableStates(DeferredBlock<? extends Block> cableBlock) {
        Block b = cableBlock.get();
        var m = getMultipartBuilder(b);

        final BooleanProperty N = EnergyCableBlock.NORTH;
        final BooleanProperty E = EnergyCableBlock.EAST;
        final BooleanProperty S = EnergyCableBlock.SOUTH;
        final BooleanProperty W = EnergyCableBlock.WEST;
        final BooleanProperty U = EnergyCableBlock.UP;
        final BooleanProperty D = EnergyCableBlock.DOWN;

        ModelFile core      = models().getExistingFile(modLoc("block/cable_core")); // small center cube
        ModelFile arm       = models().getExistingFile(modLoc("block/cable_arm"));
        ModelFile straightH = models().getExistingFile(modLoc("block/cable_h")); // base N–S
        ModelFile straightV = models().getExistingFile(modLoc("block/cable_v")); // base U–D

        // 0-connections → just the core
        m.part().modelFile(core).uvLock(true).addModel()
                .condition(N,false).condition(E,false).condition(S,false)
                .condition(W,false).condition(U,false).condition(D,false);

        // Enumerate all other states (1..63)
        for (int mask = 1; mask < 64; mask++) {
            boolean n = (mask & 1)  != 0;
            boolean e = (mask & 2)  != 0;
            boolean s = (mask & 4)  != 0;
            boolean w = (mask & 8)  != 0;
            boolean u = (mask & 16) != 0;
            boolean d = (mask & 32) != 0;

            boolean isNS = n && s && !e && !w && !u && !d;
            boolean isEW = e && w && !n && !s && !u && !d;
            boolean isUD = u && d && !n && !e && !s && !w;

            if (isNS) { partExactCable(m, straightH, n,e,s,w,u,d, 0,   0);  continue; }
            if (isEW) { partExactCable(m, straightH, n,e,s,w,u,d, 0,  90);  continue; }
            if (isUD) { partExactCable(m, straightV, n,e,s,w,u,d, 0,   0);  continue; }

            partExactCable(m, core, n,e,s,w,u,d, 0, 0);
            if (n) partExactCable(m, arm, n,e,s,w,u,d,   0,   0);
            if (e) partExactCable(m, arm, n,e,s,w,u,d,   0,  90);
            if (s) partExactCable(m, arm, n,e,s,w,u,d,   0, 180);
            if (w) partExactCable(m, arm, n,e,s,w,u,d,   0, 270);
            if (u) partExactCable(m, arm, n,e,s,w,u,d, -90,   0);
            if (d) partExactCable(m, arm, n,e,s,w,u,d,  90,   0);
        }

        // Item model: show a straight piece in inventory
        simpleBlockItem(cableBlock.get(), straightH);
    }

    // Pipe-family property bind
    private void partExactPipe(MultiPartBlockStateBuilder m, ModelFile model,
                               boolean n, boolean e, boolean s, boolean w, boolean u, boolean d,
                               int xRot, int yRot) {
        m.part().modelFile(model).rotationX(xRot).rotationY(yRot).uvLock(true)
                .addModel()
                .condition(WoodGasPipe.NORTH, n)
                .condition(WoodGasPipe.EAST,  e)
                .condition(WoodGasPipe.SOUTH, s)
                .condition(WoodGasPipe.WEST,  w)
                .condition(WoodGasPipe.UP,    u)
                .condition(WoodGasPipe.DOWN,  d);
    }

    // Cable-family property bind
    private void partExactCable(MultiPartBlockStateBuilder m, ModelFile model,
                                boolean n, boolean e, boolean s, boolean w, boolean u, boolean d,
                                int xRot, int yRot) {
        m.part().modelFile(model).rotationX(xRot).rotationY(yRot).uvLock(true)
                .addModel()
                .condition(EnergyCableBlock.NORTH, n)
                .condition(EnergyCableBlock.EAST,  e)
                .condition(EnergyCableBlock.SOUTH, s)
                .condition(EnergyCableBlock.WEST,  w)
                .condition(EnergyCableBlock.UP,    u)
                .condition(EnergyCableBlock.DOWN,  d);
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

    private void horizontal(Block block, String name) {
        ModelFile model = models().getExistingFile(modLoc("block/" + name));
        horizontalBlock(block, model); // y=0/90/180/270 for N/E/S/W
    }

    private void blockWithItem(DeferredBlock<? extends Block> block) {
        simpleBlockWithItem(block.get(), cubeAll(block.get()));
    }

    // inside your BlockStateProvider subclass



}
