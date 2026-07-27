package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.block.PineResinLogBlock;
import net.boulangermod.boulanger.block.crop.BoulangerWheatCrop;
import net.boulangermod.boulanger.block.pneumatic.DuctSide;
//pneumatic ducting deprecated for the time being
//import net.boulangermod.boulanger.block.pneumatic.OneWayValveDuctBlock;
//import net.boulangermod.boulanger.block.pneumatic.PneumaticDuctBlock;
//import net.boulangermod.boulanger.block.pneumatic.ValveDuctBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Boulanger.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // ─────────────────────────────────────────────────────────────
        // Pine textures live in: assets/boulanger/textures/block/pine/*.png
        // ─────────────────────────────────────────────────────────────

        // ─── PINE LOG (resin variants) ────────────────────────────────────────
        ResourceLocation pineLogSide      = pineTex("pine_log");
        ResourceLocation pineLogSideResin = pineTex("resin_pine_log");
        ResourceLocation pineLogEnd       = pineTex("pine_log_top");

        ModelFile pineLog      = models().cubeColumn("pine_log", pineLogSide, pineLogEnd);
        ModelFile pineLogResin = models().cubeColumn("resin_pine_log", pineLogSideResin, pineLogEnd);

        {
            RotatedPillarBlock log = (RotatedPillarBlock) ModBlocks.PINE_LOG.get();

            getVariantBuilder(log)
                    .forAllStatesExcept(state -> {
                        int remaining = state.getValue(PineResinLogBlock.RESIN_REMAINING);
                        Direction.Axis axis = state.getValue(RotatedPillarBlock.AXIS);
                        ModelFile pick = (remaining == 0) ? pineLog : pineLogResin;

                        var b = ConfiguredModel.builder().modelFile(pick);
                        if (axis == Direction.Axis.X) b.rotationX(90).rotationY(90);
                        else if (axis == Direction.Axis.Z) b.rotationX(90);
                        return b.build();
                    }, PineResinLogBlock.HAS_RESIN);

            {
                String n = name(log);
                itemModels().withExistingParent(n, pineLog.getLocation())
                        .override()
                        .predicate(mcLoc("custom_model_data"), 1)
                        .model(pineLogResin)
                        .end();
            }
        }

        // ─── STRIPPED PINE LOG ────────────────────────────────────────────────
        ResourceLocation strippedLogSide = pineTex("stripped_pine_log");
        ResourceLocation strippedLogEnd  = pineTex("stripped_pine_log_top");

        ModelFile strippedLog     = models().cubeColumn("stripped_pine_log", strippedLogSide, strippedLogEnd);
        ModelFile strippedLogHorz = models().cubeColumnHorizontal("stripped_pine_log_horizontal", strippedLogSide, strippedLogEnd);

        axisBlock((RotatedPillarBlock) ModBlocks.STRIPPED_PINE_LOG.get(), strippedLog, strippedLogHorz);
        simpleBlockItem(ModBlocks.STRIPPED_PINE_LOG.get(), strippedLog);

        // ─── STRIPPED PINE WOOD ────────────────────────────────────────────────
        ResourceLocation strippedWoodSide = pineTex("stripped_pine_wood");
        ResourceLocation strippedWoodEnd  = pineTex("stripped_pine_wood_top");

        ModelFile strippedWood     = models().cubeColumn("stripped_pine_wood", strippedWoodSide, strippedWoodEnd);
        ModelFile strippedWoodHorz = models().cubeColumnHorizontal("stripped_pine_wood_horizontal", strippedWoodSide, strippedWoodEnd);

        axisBlock((RotatedPillarBlock) ModBlocks.STRIPPED_PINE_WOOD.get(), strippedWood, strippedWoodHorz);
        simpleBlockItem(ModBlocks.STRIPPED_PINE_WOOD.get(), strippedWood);

        // ─── PINE WOOD ────────────────────────────────────────────────────────
        ModelFile pineWood     = models().cubeColumn("pine_wood", pineLogSide, pineLogSide);
        ModelFile pineWoodHorz = models().cubeColumnHorizontal("pine_wood_horizontal", pineLogSide, pineLogSide);

        axisBlock((RotatedPillarBlock) ModBlocks.PINE_WOOD.get(), pineWood, pineWoodHorz);
        simpleBlockItem(ModBlocks.PINE_WOOD.get(), pineWood);

        // ─── PLANKS ──────────────────────────────────────────────────────────
        Block planks = ModBlocks.PINE_PLANKS.get();
        ResourceLocation planksTex = pineTex("pine_planks");
        ModelFile planksModel = models().cubeAll(name(planks), planksTex);
        simpleBlockWithItem(planks, planksModel);

        // ─── STAIRS ──────────────────────────────────────────────────────────
        {
            StairBlock stairs = (StairBlock) ModBlocks.PINE_STAIRS.get();
            String n = name(stairs);

            ModelFile stairsModel = models().stairs(n, planksTex, planksTex, planksTex);
            ModelFile stairsInner = models().stairsInner(n + "_inner", planksTex, planksTex, planksTex);
            ModelFile stairsOuter = models().stairsOuter(n + "_outer", planksTex, planksTex, planksTex);

            stairsBlock(stairs, stairsModel, stairsInner, stairsOuter);
            simpleBlockItem(stairs, stairsModel);
        }

        // ─── SLAB ────────────────────────────────────────────────────────────
        {
            SlabBlock slab = (SlabBlock) ModBlocks.PINE_SLAB.get();
            String n = name(slab);

            ModelFile slabBottom = models()
                    .withExistingParent(n, mcLoc("block/slab"))
                    .texture("bottom", planksTex)
                    .texture("top", planksTex)
                    .texture("side", planksTex);

            ModelFile slabTop = models()
                    .withExistingParent(n + "_top", mcLoc("block/slab_top"))
                    .texture("bottom", planksTex)
                    .texture("top", planksTex)
                    .texture("side", planksTex);

            getVariantBuilder(slab).forAllStates(state -> {
                SlabType type = state.getValue(SlabBlock.TYPE);
                ModelFile pick = (type == SlabType.DOUBLE) ? planksModel : (type == SlabType.TOP) ? slabTop : slabBottom;
                return ConfiguredModel.builder().modelFile(pick).build();
            });

            simpleBlockItem(slab, slabBottom);
        }

        // ─── FENCE ───────────────────────────────────────────────────────────
        {
            FenceBlock fence = (FenceBlock) ModBlocks.PINE_FENCE.get();
            String n = name(fence);

            fenceBlock(fence, n, planksTex);
            ModelFile inv = models().fenceInventory(n + "_inventory", planksTex);
            simpleBlockItem(fence, inv);
        }

        // ─── FENCE GATE ──────────────────────────────────────────────────────
        {
            FenceGateBlock gate = (FenceGateBlock) ModBlocks.PINE_FENCE_GATE.get();
            String n = name(gate);

            ModelFile closed   = models().fenceGate(n, planksTex);
            ModelFile open     = models().fenceGateOpen(n + "_open", planksTex);
            ModelFile wall     = models().fenceGateWall(n + "_wall", planksTex);
            ModelFile wallOpen = models().fenceGateWallOpen(n + "_wall_open", planksTex);

            fenceGateBlock(gate, closed, open, wall, wallOpen);
            simpleBlockItem(gate, closed);
        }

        // ─── LEAVES (cutout) ─────────────────────────────────────────────────
        {
            Block leaves = ModBlocks.PINE_LEAVES.get();
            ModelFile leavesModel = models().singleTexture(
                    name(leaves),
                    ResourceLocation.fromNamespaceAndPath("minecraft", "block/leaves"),
                    "all",
                    pineTex("pine_leaves")
            ).renderType("cutout");

            simpleBlockWithItem(leaves, leavesModel);
        }

        // ─── SAPLING ─────────────────────────────────────────────────────────
        {
            Block sapling = ModBlocks.PINE_SAPLING.get();

            ModelFile saplingBlockModel = models()
                    .cross(name(sapling), pineTex("pine_sapling"))
                    .renderType("cutout");
            simpleBlock(sapling, saplingBlockModel);

            itemModels()
                    .withExistingParent(name(sapling), mcLoc("item/generated"))
                    .texture("layer0", pineTex("pine_sapling"));
        }

        // ─── PRESSURE PLATE ──────────────────────────────────────────────────
        {
            PressurePlateBlock plate = (PressurePlateBlock) ModBlocks.PINE_PRESSURE_PLATE.get();
            String n = name(plate);

            ModelFile up = models().pressurePlate(n + "_up", planksTex);
            ModelFile down = models().pressurePlateDown(n + "_down", planksTex);

            getVariantBuilder(plate).forAllStates(s ->
                    ConfiguredModel.builder()
                            .modelFile(s.getValue(PressurePlateBlock.POWERED) ? down : up)
                            .build()
            );

            simpleBlockItem(plate, up);
        }

        // ─── BUTTON ──────────────────────────────────────────────────────────
        {
            ButtonBlock button = (ButtonBlock) ModBlocks.PINE_BUTTON.get();
            String n = name(button);

            buttonBlock(button, planksTex);
            models().buttonInventory(n + "_inventory", planksTex);
            itemModels().withExistingParent(n, modLoc("block/" + n + "_inventory"));
        }

        // ─── TRAPDOOR (cutout) ───────────────────────────────────────────────
        {
            TrapDoorBlock trapdoor = (TrapDoorBlock) ModBlocks.PINE_TRAPDOOR.get();
            trapdoorBlockWithRenderType(trapdoor, pineTex("pine_trapdoor"), true, "cutout");
            simpleBlockItem(trapdoor, models().getExistingFile(modLoc("block/pine_trapdoor_bottom")));
        }

        // ─── DOOR (cutout) ───────────────────────────────────────────────────
        {
            DoorBlock door = (DoorBlock) ModBlocks.PINE_DOOR.get();
            doorBlockWithRenderType(door, pineTex("pine_door_bottom"), pineTex("pine_door_top"), "cutout");
            itemModels().basicItem(door.asItem());
        }

        // ─── KAOLINITE CLAY ──────────────────────────────────────────────────
        simpleBlockWithItem(ModBlocks.KAOLINITE_CLAY.get(), cubeAll(ModBlocks.KAOLINITE_CLAY.get()));

        //scale
        ModelFile scalePlaceholder = models().cubeAll(
                "scale",
                mcLoc("block/iron_block")
        );

        simpleBlockWithItem(
                ModBlocks.SCALE.get(),
                scalePlaceholder
        );

//pneumatic ducting deprecated for the time being
//        // ─── PNEUMATICS ──────────────────────────────────────────────────────
//        pneumaticDuct(ModBlocks.PNEUMATIC_DUCT.get());
//        simpleBlockWithItem(ModBlocks.AIR_COMPRESSOR.get(), cubeAll(ModBlocks.AIR_COMPRESSOR.get()));
//        simpleBlockWithItem(ModBlocks.AIR_TANK.get(), cubeAll(ModBlocks.AIR_TANK.get()));
//
//        // INLINE valves only (no multipart / no flanges / no up/down)
//        valveDuctInline(ModBlocks.VALVE_DUCT.get());
//        oneWayValveDuctInline(ModBlocks.ONE_WAY_VALVE_DUCT.get());

        // ─── WHEAT ───────────────────────────────────────────────────────────
        wheatBlocks();
    }

    // ─────────────────────────────────────────────────────────────
    // Wheat crop + wild wheat
    // ─────────────────────────────────────────────────────────────
    private void wheatBlocks() {
        Block wild = ModBlocks.WILD_WHEAT.get();
        ModelFile wildModel = models()
                .cross(name(wild), modLoc("block/wheat/wild_wheat"))
                .renderType("cutout");
        simpleBlockWithItem(wild, wildModel);

        Block crop = ModBlocks.WHEAT_BUSHEL_BLOCK.get();
        String cropName = name(crop);

        ModelFile[] stages = new ModelFile[8];
        for (int age = 0; age <= 7; age++) {
            stages[age] = models()
                    .crop(cropName + "_stage" + age, modLoc("block/wheat/boulanger_wheat_stage" + age))
                    .renderType("cutout");
        }

        getVariantBuilder(crop).forAllStates(state -> {
            int age = state.getValue(BoulangerWheatCrop.AGE);
            if (age < 0) age = 0;
            if (age > 7) age = 7;
            return ConfiguredModel.builder().modelFile(stages[age]).build();
        });

        itemModels().withExistingParent(cropName, stages[7].getLocation());
    }

//    // ─────────────────────────────────────────────────────────────
//    // Pneumatic duct multipart state
//    // ─────────────────────────────────────────────────────────────
//    private static final DuctSide[] PIPE_SIDES = new DuctSide[]{DuctSide.OPEN, DuctSide.CONNECTED, DuctSide.FLANGED};
//
//    private void pneumaticDuct(Block duct) {
//        // Authored E/W outlets
//        ModelFile straightEW = models().getExistingFile(modLoc("block/pneumatic_duct_horizontal"));
//        ModelFile straightUD = models().getExistingFile(modLoc("block/pneumatic_duct_verticle")); // your asset spelling
//
//        ModelFile core = models().getExistingFile(modLoc("block/pneumatic_duct_core"));
//        ModelFile armNorth = models().getExistingFile(modLoc("block/pneumatic_duct_multipart"));
//
//        // flange authored facing NORTH
//        ModelFile flange = models().getExistingFile(modLoc("block/flange"));
//
//        MultiPartBlockStateBuilder b = getMultipartBuilder(duct);
//
//        // East/West straight (matches authored orientation)
//        b.part().modelFile(straightEW).addModel()
//                .condition(PneumaticDuctBlock.EAST, PIPE_SIDES)
//                .condition(PneumaticDuctBlock.WEST, PIPE_SIDES)
//                .condition(PneumaticDuctBlock.NORTH, DuctSide.CLOSED)
//                .condition(PneumaticDuctBlock.SOUTH, DuctSide.CLOSED)
//                .condition(PneumaticDuctBlock.UP, DuctSide.CLOSED)
//                .condition(PneumaticDuctBlock.DOWN, DuctSide.CLOSED)
//                .end();
//
//        // North/South straight (rotate E/W model 90)
//        b.part().modelFile(straightEW).rotationY(90).addModel()
//                .condition(PneumaticDuctBlock.NORTH, PIPE_SIDES)
//                .condition(PneumaticDuctBlock.SOUTH, PIPE_SIDES)
//                .condition(PneumaticDuctBlock.EAST, DuctSide.CLOSED)
//                .condition(PneumaticDuctBlock.WEST, DuctSide.CLOSED)
//                .condition(PneumaticDuctBlock.UP, DuctSide.CLOSED)
//                .condition(PneumaticDuctBlock.DOWN, DuctSide.CLOSED)
//                .end();
//
//        // Up/Down straight
//        b.part().modelFile(straightUD).addModel()
//                .condition(PneumaticDuctBlock.UP, PIPE_SIDES)
//                .condition(PneumaticDuctBlock.DOWN, PIPE_SIDES)
//                .condition(PneumaticDuctBlock.NORTH, DuctSide.CLOSED)
//                .condition(PneumaticDuctBlock.SOUTH, DuctSide.CLOSED)
//                .condition(PneumaticDuctBlock.EAST, DuctSide.CLOSED)
//                .condition(PneumaticDuctBlock.WEST, DuctSide.CLOSED)
//                .end();
//
//        // Core ONLY when not a perfect straight
//        {
//            MultiPartBlockStateBuilder.PartBuilder pb = b.part().modelFile(core).addModel();
//            pb.useOr();
//            addNotStraightWhenGroups(pb);
//            pb.end();
//        }
//
//        // Arms + flange per face
//        for (Direction dir : Direction.values()) {
//            EnumProperty<DuctSide> face = PneumaticDuctBlock.propFor(dir);
//
//            {
//                int rx = rotXFromNorth(dir);
//                int ry = rotYFromNorth(dir);
//
//                MultiPartBlockStateBuilder.PartBuilder pb = b.part()
//                        .modelFile(armNorth)
//                        .rotationX(rx)
//                        .rotationY(ry)
//                        .addModel();
//
//                pb.useOr();
//                addArmVisibilityGroups(pb, dir);
//                pb.end();
//            }
//
//            b.part()
//                    .modelFile(flange)
//                    .rotationX(rotXFromNorth(dir))
//                    .rotationY(rotYFromNorth(dir))
//                    .addModel()
//                    .condition(face, DuctSide.FLANGED)
//                    .end();
//        }
//
//        simpleBlockItem(duct, straightEW);
//    }
//
//    private void valveDuctInline(Block duct) {
//        ModelFile open = models().getExistingFile(modLoc("block/valve"));
//        ModelFile closed = models().getExistingFile(modLoc("block/valve_closed"));
//
//        var vb = getVariantBuilder(duct);
//
//        for (Direction facing : Direction.Plane.HORIZONTAL) {
//            vb.partialState()
//                    .with(ValveDuctBlock.FACING, facing)
//                    .with(ValveDuctBlock.OPEN, true)
//                    .modelForState()
//                    .modelFile(open)
//                    .rotationY(rotYFromNorth(facing))
//                    .addModel();
//
//            vb.partialState()
//                    .with(ValveDuctBlock.FACING, facing)
//                    .with(ValveDuctBlock.OPEN, false)
//                    .modelForState()
//                    .modelFile(closed)
//                    .rotationY(rotYFromNorth(facing))
//                    .addModel();
//        }
//
//        simpleBlockItem(duct, open);
//    }
//
//    private void oneWayValveDuctInline(Block duct) {
//        ModelFile open = models().getExistingFile(modLoc("block/one_way_valve"));
//        ModelFile closed = models().getExistingFile(modLoc("block/one_way_valve_closed"));
//
//        var vb = getVariantBuilder(duct);
//
//        for (Direction facing : Direction.Plane.HORIZONTAL) {
//            vb.partialState()
//                    .with(OneWayValveDuctBlock.FACING, facing)
//                    .with(OneWayValveDuctBlock.OPEN, true)
//                    .modelForState()
//                    .modelFile(open)
//                    .rotationY(rotYFromNorth(facing))
//                    .addModel();
//
//            vb.partialState()
//                    .with(OneWayValveDuctBlock.FACING, facing)
//                    .with(OneWayValveDuctBlock.OPEN, false)
//                    .modelForState()
//                    .modelFile(closed)
//                    .rotationY(rotYFromNorth(facing))
//                    .addModel();
//        }
//
//        simpleBlockItem(duct, open);
//    }
//
//    // ─────────────────────────────────────────────────────────────
//    // Multipart condition helpers
//    // ─────────────────────────────────────────────────────────────
//    private static void addNotStraightWhenGroups(MultiPartBlockStateBuilder.PartBuilder pb) {
//        pb.nestedGroup().condition(PneumaticDuctBlock.NORTH, PIPE_SIDES).condition(PneumaticDuctBlock.EAST,  PIPE_SIDES).end();
//        pb.nestedGroup().condition(PneumaticDuctBlock.NORTH, PIPE_SIDES).condition(PneumaticDuctBlock.WEST,  PIPE_SIDES).end();
//        pb.nestedGroup().condition(PneumaticDuctBlock.NORTH, PIPE_SIDES).condition(PneumaticDuctBlock.UP,    PIPE_SIDES).end();
//        pb.nestedGroup().condition(PneumaticDuctBlock.NORTH, PIPE_SIDES).condition(PneumaticDuctBlock.DOWN,  PIPE_SIDES).end();
//
//        pb.nestedGroup().condition(PneumaticDuctBlock.SOUTH, PIPE_SIDES).condition(PneumaticDuctBlock.EAST,  PIPE_SIDES).end();
//        pb.nestedGroup().condition(PneumaticDuctBlock.SOUTH, PIPE_SIDES).condition(PneumaticDuctBlock.WEST,  PIPE_SIDES).end();
//        pb.nestedGroup().condition(PneumaticDuctBlock.SOUTH, PIPE_SIDES).condition(PneumaticDuctBlock.UP,    PIPE_SIDES).end();
//        pb.nestedGroup().condition(PneumaticDuctBlock.SOUTH, PIPE_SIDES).condition(PneumaticDuctBlock.DOWN,  PIPE_SIDES).end();
//
//        pb.nestedGroup().condition(PneumaticDuctBlock.EAST,  PIPE_SIDES).condition(PneumaticDuctBlock.UP,    PIPE_SIDES).end();
//        pb.nestedGroup().condition(PneumaticDuctBlock.EAST,  PIPE_SIDES).condition(PneumaticDuctBlock.DOWN,  PIPE_SIDES).end();
//        pb.nestedGroup().condition(PneumaticDuctBlock.WEST,  PIPE_SIDES).condition(PneumaticDuctBlock.UP,    PIPE_SIDES).end();
//        pb.nestedGroup().condition(PneumaticDuctBlock.WEST,  PIPE_SIDES).condition(PneumaticDuctBlock.DOWN,  PIPE_SIDES).end();
//    }
//
//    private static void addArmVisibilityGroups(MultiPartBlockStateBuilder.PartBuilder pb, Direction dir) {
//        EnumProperty<DuctSide> face = PneumaticDuctBlock.propFor(dir);
//
//        for (Direction adj : Direction.values()) {
//            if (adj == dir || adj == dir.getOpposite()) continue;
//
//            pb.nestedGroup()
//                    .condition(face, PIPE_SIDES)
//                    .condition(PneumaticDuctBlock.propFor(adj), PIPE_SIDES)
//                    .end();
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────
//    // Rotation helpers
//    // Arms/flanges authored facing NORTH
//    // Valve inline models authored along North/South axis
//    // ─────────────────────────────────────────────────────────────
//    private static int rotXFromNorth(Direction dir) {
//        return switch (dir) {
//            case UP -> 270;
//            case DOWN -> 90;
//            default -> 0;
//        };
//    }
//
//    private static int rotYFromNorth(Direction dir) {
//        return switch (dir) {
//            case NORTH -> 0;
//            case EAST -> 90;
//            case SOUTH -> 180;
//            case WEST -> 270;
//            default -> 0;
//        };
//    }

    private ResourceLocation pineTex(String name) {
        return modLoc("block/pine/" + name);
    }

    private static String name(Block b) {
        return BuiltInRegistries.BLOCK.getKey(b).getPath();
    }
}
