package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.block.PineResinLogBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Boulanger.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // ─────────────────────────────────────────────────────────────
        // Pine textures live in: assets/boulanger/textures/block/pine/*.png
        // So we always reference them as: modLoc("block/pine/<name>")
        // ─────────────────────────────────────────────────────────────

        // ─── PINE LOG (resin variants) ────────────────────────────────────────
        ResourceLocation pineLogSide      = pineTex("pine_log");
        ResourceLocation pineLogSideResin = pineTex("resin_pine_log");
        ResourceLocation pineLogEnd       = pineTex("pine_log_top");

        ModelFile pineLog      = models().cubeColumn("pine_log", pineLogSide, pineLogEnd);
        ModelFile pineLogResin = models().cubeColumn("resin_pine_log", pineLogSideResin, pineLogEnd);


        // Pine log (with resin variant chosen by resin_remaining)
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

        // ─── STRIPPED PINE WOOD (bark on all faces; has its own textures) ─────
        ResourceLocation strippedWoodSide = pineTex("stripped_pine_wood");
        ResourceLocation strippedWoodEnd  = pineTex("stripped_pine_wood_top");

        ModelFile strippedWood     = models().cubeColumn("stripped_pine_wood", strippedWoodSide, strippedWoodEnd);
        ModelFile strippedWoodHorz = models().cubeColumnHorizontal("stripped_pine_wood_horizontal", strippedWoodSide, strippedWoodEnd);

        axisBlock((RotatedPillarBlock) ModBlocks.STRIPPED_PINE_WOOD.get(), strippedWood, strippedWoodHorz);
        simpleBlockItem(ModBlocks.STRIPPED_PINE_WOOD.get(), strippedWood);

        // ─── PINE WOOD (bark on all faces; reuse pine_log for every face) ─────
        ModelFile pineWood     = models().cubeColumn("pine_wood", pineLogSide, pineLogSide);
        ModelFile pineWoodHorz = models().cubeColumnHorizontal("pine_wood_horizontal", pineLogSide, pineLogSide);

        axisBlock((RotatedPillarBlock) ModBlocks.PINE_WOOD.get(), pineWood, pineWoodHorz);
        simpleBlockItem(ModBlocks.PINE_WOOD.get(), pineWood);

        // ─── PLANKS ──────────────────────────────────────────────────────────
        ResourceLocation planksTex = pineTex("pine_planks");
        ModelFile planksModel = models().cubeAll("pine_planks", planksTex);
        simpleBlockWithItem(ModBlocks.PINE_PLANKS.get(), planksModel);

        // ─── STAIRS ──────────────────────────────────────────────────────────
        {
            StairBlock stairs = (StairBlock) ModBlocks.PINE_STAIRS.get();
            String n = name(stairs);

            ModelFile stairsModel = models().stairs(n, planksTex, planksTex, planksTex);
            ModelFile stairsInner = models().stairsInner(n + "_inner", planksTex, planksTex, planksTex);
            ModelFile stairsOuter = models().stairsOuter(n + "_outer", planksTex, planksTex, planksTex);

            // Use the overload that accepts ModelFiles so nothing needs to "already exist"
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

            // Blockstate: choose bottom/top/double (double uses full planks cube model)
            getVariantBuilder(slab).forAllStates(state -> {
                SlabType type = state.getValue(SlabBlock.TYPE);
                ModelFile pick = (type == SlabType.DOUBLE) ? planksModel : (type == SlabType.TOP) ? slabTop : slabBottom;
                return ConfiguredModel.builder().modelFile(pick).build();
            });

            // Item model = bottom slab
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

            ModelFile closed    = models().fenceGate(n, planksTex);
            ModelFile open      = models().fenceGateOpen(n + "_open", planksTex);
            ModelFile wall      = models().fenceGateWall(n + "_wall", planksTex);
            ModelFile wallOpen  = models().fenceGateWallOpen(n + "_wall_open", planksTex);

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

        // ─── SAPLING ────────────────────────────────────────────────────────────
        {
            Block sapling = ModBlocks.PINE_SAPLING.get();

            // Blockstate/model (cross, cutout)
            ModelFile saplingBlockModel = models()
                    .cross(name(sapling), pineTex("pine_sapling"))
                    .renderType("cutout");
            simpleBlock(sapling, saplingBlockModel);

            // Item model (flat icon, correct size)
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

            // Item model = bottom
            simpleBlockItem(trapdoor, models().getExistingFile(modLoc("block/pine_trapdoor_bottom")));
        }

        // ─── DOOR (cutout) ───────────────────────────────────────────────────
        {
            DoorBlock door = (DoorBlock) ModBlocks.PINE_DOOR.get();
            doorBlockWithRenderType(door, pineTex("pine_door_bottom"), pineTex("pine_door_top"), "cutout");
            // Door item uses item texture (assets/.../textures/item/pine_door.png)
            itemModels().basicItem(door.asItem());
        }

        // ─── KAOLINITE CLAY (keep) ───────────────────────────────────────────
        simpleBlockWithItem(ModBlocks.KAOLINITE_CLAY.get(), cubeAll(ModBlocks.KAOLINITE_CLAY.get()));
    }

    private ResourceLocation pineTex(String name) {
        return modLoc("block/pine/" + name);
    }

    private static String name(Block b) {
        return BuiltInRegistries.BLOCK.getKey(b).getPath();
    }
}
