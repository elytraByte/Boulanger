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

// models (name the resin model EXACTLY like your blockstate expects)
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


// Stripped pine log: pillar-style mapping (unchanged)
        logBlock((RotatedPillarBlock) ModBlocks.STRIPPED_PINE_LOG.get());
        simpleBlockItem(ModBlocks.STRIPPED_PINE_LOG.get(),
                models().getExistingFile(modLoc("block/stripped_pine_log")));

// Stripped pine wood uses ONE texture for all faces
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

// ─── PRESSURE PLATE (explicit models + state mapping) ───────────────────────
        {
            PressurePlateBlock plate = (PressurePlateBlock) ModBlocks.PINE_PRESSURE_PLATE.get();
            String n = BuiltInRegistries.BLOCK.getKey(plate).getPath();
            ResourceLocation tex = blockTexture(ModBlocks.PINE_PLANKS.get());

            // create block models so …_up / …_down ALWAYS exist
            ModelFile up = models().pressurePlate(n + "_up", tex);
            ModelFile down = models().pressurePlateDown(n + "_down", tex);

            // POWERED=false -> up ; POWERED=true -> down
            getVariantBuilder(plate).forAllStates(s ->
                    ConfiguredModel.builder()
                            .modelFile(s.getValue(PressurePlateBlock.POWERED) ? down : up)
                            .build()
            );

            // item → parent the UP model
            itemModels().withExistingParent(n, modLoc("block/" + n + "_up"));
        }

        {
            ButtonBlock btn = (ButtonBlock) ModBlocks.PINE_BUTTON.get();
            ResourceLocation tex = blockTexture(ModBlocks.PINE_PLANKS.get());

            buttonBlock(btn, tex);  // generates all FLOOR/WALL/CEILING + POWERED variants

            // Item model
            simpleBlockItem(
                    ModBlocks.PINE_BUTTON.get(),
                    models().buttonInventory(ModBlocks.PINE_BUTTON.getId().getPath(), tex)
            );
        }


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

// ─── DOOR (bottom/top textures + cutout render type) ────────────────────────
        {
            DoorBlock door = (DoorBlock) ModBlocks.PINE_DOOR.get();

            // If you have separate textures:
            ResourceLocation bottom = modLoc("block/pine_door_bottom");
            ResourceLocation top = modLoc("block/pine_door_top");

            // If you only have one texture image, you can point both to the same file:
            // ResourceLocation bottom = modLoc("block/pine_door");
            // ResourceLocation top    = bottom;

            doorBlockWithRenderType(door, bottom, top, "cutout");
        }

// Item is already correct:
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
        horizontal(ModBlocks.SCALE_BLOCK.get(), "scale_block");
        horizontal(ModBlocks.PROOFING_BOX.get(), "proofing_box");
        horizontal(ModBlocks.DOUGH_DIVIDER.get(), "dough_divider");

        horizontalBlock(ModBlocks.STONE_MILL_BLOCK.get(),
                models().getExistingFile(modLoc("block/stone_mill_block")));


        // ─── STONE MILL & TILES ───────────────────────────────────────────────
        blockWithItem(ModBlocks.KAOLINITE_CLAY);
        blockWithItem(ModBlocks.BLACK_TILE);
        blockWithItem(ModBlocks.LIGHT_BLUE_TILE);
        blockWithItem(ModBlocks.BLUE_TILE);
        blockWithItem(ModBlocks.BLUE_WHITE_TILE);
        blockWithItem(ModBlocks.L3E_TILE);
        blockWithItem(ModBlocks.WHITE_TILE);
//        scaleBlockWithCustomSides(ModBlocks.SCALE_BLOCK.get());
        blockWithItem(ModBlocks.MACHINE_HOUSING);

// Core (rod + base): vanilla geo, your stand texture
        ModelFile core = models()
                .withExistingParent("sugar_refinery_core", mcLoc("block/brewing_stand"))
                .texture("base", mcLoc("block/brewing_stand_base")) // vanilla base
                .texture("stand", modLoc("block/sugar_refinery"))    // your main texture
                .texture("particle", modLoc("block/sugar_refinery"))
                .renderType("cutout");                                   // <-- important

// Arms (three radial plates). If you want your texture on them too:
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

// Multipart blockstate = core + all three arms
        getMultipartBuilder(ModBlocks.SUGAR_REFINERY.get())
                .part().modelFile(core).addModel().end()
                .part().modelFile(arm0).addModel().end()
                .part().modelFile(arm1).addModel().end()
                .part().modelFile(arm2).addModel().end();

// Item model (uses vanilla brewing-stand item geo, with your stand texture)
        simpleBlockItem(
                ModBlocks.SUGAR_REFINERY.get(),
                models()
                        .withExistingParent("sugar_refinery_item", mcLoc("item/brewing_stand"))
                        .texture("base", mcLoc("block/brewing_stand_base"))
                        .texture("stand", modLoc("block/sugar_refinery"))
                        .texture("particle", modLoc("block/sugar_refinery"))
                        .renderType("cutout")
        );

        // iron_frame: stage 0..7 -> iron_frame_0..7
        var builder = getVariantBuilder(ModBlocks.IRON_FRAME.get());
        for (int stage = 0; stage <= IronFrameBlock.MAX_STAGE; stage++) {
            ModelFile model = models().getExistingFile(modLoc("block/iron_frame_" + stage));
            builder.partialState()
                    .with(IronFrameBlock.STAGE, stage)
                    .modelForState()
                    .modelFile(model)
                    .addModel();
        }

        // Block item uses stage 0 in inventory
        itemModels().withExistingParent("iron_frame", modLoc("block/iron_frame_0"));

        // MOTIVATOR: bottom/top/side
        String name = "motivator"; // your block id: assets/boulanger/models/block/motivator.json, etc.

        ModelFile motivatorModel = models().cubeBottomTop(
                name,
                modLoc("block/motivator_side"),   // side
                modLoc("block/motivator_bottom"), // bottom
                modLoc("block/motivator_top")     // top
        );

        simpleBlock(ModBlocks.MOTIVATOR.get(), motivatorModel);

        // Item model points to the block model
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
                            modLoc("block/diorite_side"),
                            modLoc("block/wood_oven_front" + suffix),
                            modLoc("block/diorite_side")
                    );
                    return ConfiguredModel.builder()
                            .modelFile(file)
                            .rotationY((int) dir.toYRot())
                            .build();
                });
        simpleBlockItem(ModBlocks.WOOD_OVEN.get(),
                models().getExistingFile(modLoc("block/wood_oven_off"))
        );

        // ─── BAKER'S TABLE ─────────────────────────────────────────────────────
        ModelFile bakersTableModel = models().orientable(
                "bakers_table",
                modLoc("block/bakers_table_side"),   // all four side faces
                modLoc("block/bakers_table_front"),  // “front” face
                modLoc("block/bakers_table_top")     // top face
        );

// use the same orientable model for the item form
        simpleBlockItem(ModBlocks.BAKERS_TABLE.get(), bakersTableModel);

// rotate the block in-world based on its facing property
        getVariantBuilder(ModBlocks.BAKERS_TABLE.get())
                .forAllStates(s -> {
                    Direction dir = s.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    return ConfiguredModel.builder()
                            .modelFile(bakersTableModel)
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
                            .uvLock(true)                 // ← prevents face UVs from warping
                            .build();
                });

// Item = base cube
        itemModels().withExistingParent(
                BuiltInRegistries.BLOCK.getKey(ModBlocks.WOOD_GASIFIER.get()).getPath(),
                modLoc("block/wood_gasifier_base")
        );



// Item = base cube
        itemModels().withExistingParent(
                BuiltInRegistries.BLOCK.getKey(ModBlocks.WOOD_GASIFIER.get()).getPath(),
                modLoc("block/wood_gasifier_base")
        );


        ResourceLocation bedrockTex = ResourceLocation.fromNamespaceAndPath("minecraft", "block/bedrock");


        ModelFile energyModel = models().cubeAll(
                "energy_bedrock",
                bedrockTex
        );

        simpleBlockWithItem(
                ModBlocks.ENERGY_CABLE.get(),
                energyModel
        );

        simpleBlockItem(
                ModBlocks.BATTERY.get(),
                energyModel
        );

        getVariantBuilder(ModBlocks.BATTERY.get())
                .forAllStates(s -> ConfiguredModel.builder()
                        .modelFile(energyModel)
                        .build()
                );
        // ————————————————————————————————————————————————————————————————————————

        ResourceLocation coalTex = ResourceLocation.fromNamespaceAndPath("minecraft", "block/coal_block");


        // ─── WOODGAS ENGINE (vanilla orientable: top/side + front on/off) ───────────
        {
            ResourceLocation texSide = modLoc("block/woodgas_engine_side");
            ResourceLocation texTop = modLoc("block/woodgas_engine_top"); // also used as bottom by 'orientable'
            ResourceLocation texFrontOff = modLoc("block/woodgas_engine_off");
            ResourceLocation texFrontOn = modLoc("block/woodgas_engine_on");

            // Models using vanilla parent block/orientable (top, side, front)
            ModelFile engineOff = models()
                    .withExistingParent("woodgas_engine_off", mcLoc("block/orientable"))
                    .texture("top", texTop)
                    .texture("side", texSide)
                    .texture("front", texFrontOff);

            ModelFile engineOn = models()
                    .withExistingParent("woodgas_engine_on", mcLoc("block/orientable"))
                    .texture("top", texTop)
                    .texture("side", texSide)
                    .texture("front", texFrontOn);

            // Blockstate: rotate to HORIZONTAL_FACING; choose model by LIT
            getVariantBuilder(ModBlocks.WOODGAS_ENGINE.get())
                    .forAllStates(s -> {
                        Direction dir = s.getValue(AbstractProcessingBlock.FACING);
                        boolean lit = s.getValue(WoodGasEngineBlock.LIT);

                        int rotY = (((int) dir.toYRot()) + 180) % 360; // make the “front” face you

                        return ConfiguredModel.builder()
                                .modelFile(lit ? engineOn : engineOff)
                                .rotationY(rotY)
                                .build();
                    });


            // Item: show OFF variant in inventory
            simpleBlockItem(ModBlocks.WOODGAS_ENGINE.get(), engineOff);
        }

        woodgasPipeStates(ModBlocks.WOODGAS_PIPE);
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
            if (isNS) { partExact(m, straightH, n,e,s,w,u,d, 0,  0);  continue; }
            if (isEW) { partExact(m, straightH, n,e,s,w,u,d, 0, 90);  continue; }
            if (isUD) { partExact(m, straightV, n,e,s,w,u,d, 0,  0);  continue; }

            // All other cases: center (use isolated) + arms for each true side
            partExact(m, isolated, n,e,s,w,u,d, 0, 0);
            if (n) partExact(m, arm, n,e,s,w,u,d, 0,   0);
            if (e) partExact(m, arm, n,e,s,w,u,d, 0,  90);
            if (s) partExact(m, arm, n,e,s,w,u,d, 0, 180);
            if (w) partExact(m, arm, n,e,s,w,u,d, 0, 270);
            if (u) partExact(m, arm, n,e,s,w,u,d, -90, 0);
            if (d) partExact(m, arm, n,e,s,w,u,d,  90, 0);
        }
    }

    // Helper: add a part tied to the exact connection mask (prevents arms/isolated in pure straights)
    private void partExact(MultiPartBlockStateBuilder m, ModelFile model,
                           boolean n, boolean e, boolean s, boolean w, boolean u, boolean d,
                           int xRot, int yRot) {
        m.part().modelFile(model).rotationX(xRot).rotationY(yRot).uvLock(true)
                .addModel() // now on PartBuilder
                .condition(WoodGasPipe.NORTH, n)
                .condition(WoodGasPipe.EAST,  e)
                .condition(WoodGasPipe.SOUTH, s)
                .condition(WoodGasPipe.WEST,  w)
                .condition(WoodGasPipe.UP,    u)
                .condition(WoodGasPipe.DOWN,  d);
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


}
