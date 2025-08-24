package net.boulangermod.boulanger.block;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.crops.HardRedSpringWheatCrop;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.worldgen.tree.ModTreeGrowers;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Boulanger.MODID);


    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    public static final DeferredBlock<Block> WOOD_GASIFIER = registerBlock("wood_gasifier",
            () -> new WoodGasifierBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final DeferredBlock<Block> WOOD_OVEN = registerBlock("wood_oven",
            () -> new WoodOvenBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final DeferredBlock<Block> MIXING_BLOCK = registerBlock("mixing_block",
            () -> new MixingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final DeferredBlock<Block> GAS_TANK = registerBlock("gas_tank",
            () -> new WoodGasTankBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final DeferredBlock<Block> SCALE_BLOCK =
            registerBlock("scale_block",
                    () -> new ScaleBlock(BlockBehaviour.Properties.of()
                            .strength(1.5F)
                            .noOcclusion()));  // ← important

    public static final DeferredBlock<StoneMillBlock> STONE_MILL_BLOCK = registerBlock("stone_mill",
            () -> new StoneMillBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F)
                    .noOcclusion()
            ));

    public static final DeferredBlock<SugarRefineryBlock> SUGAR_REFINERY = registerBlock(
            "sugar_refinery",
            () -> new SugarRefineryBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .noOcclusion()                 // <-- important: don’t cull neighbors
            )
    );

    public static final DeferredBlock<WoodGasFlareBlock> WOODGAS_FLARE =
            registerBlock("woodgas_flare",
                    () -> new WoodGasFlareBlock(
                            BlockBehaviour.Properties.of()
                                    .noOcclusion()
                                    .strength(0.3F)
                                    .sound(SoundType.LANTERN)
                                    .lightLevel(s -> s.getValue(WoodGasFlareBlock.LIT) ? 14 : 0)
                    )
            );

    public static final DeferredBlock<Block> WOODGAS_VALVE =
            registerBlock("woodgas_valve",
                    () -> new WoodGasValveBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));


    public static final DeferredBlock<Block> FEED_THROUGH_BLOCK =
            registerBlock("feed_through_block",
                    () -> new WoodGasFeedThroughBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));

    public static final DeferredBlock<Block> MOTIVATOR =
            registerBlock("motivator",
                    () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));

    public static final DeferredBlock<Block> IRON_FRAME =
            registerBlock("iron_frame", IronFrameBlock::new);

    public static final DeferredBlock<Block> KAOLINITE_CLAY = registerBlock("kaolinite_clay",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CLAY)));

    public static final DeferredBlock<Block> BLACK_TILE = registerBlock("black_tile",
            () -> new DecorativePorcelainTileBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)));

    public static final DeferredBlock<Block> BLUE_TILE = registerBlock("blue_tile",
            () -> new DecorativePorcelainTileBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)));

    public static final DeferredBlock<Block> LIGHT_BLUE_TILE = registerBlock("light_blue_tile",
            () -> new DecorativePorcelainTileBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)));

    public static final DeferredBlock<Block> BLUE_WHITE_TILE = registerBlock("blue_white_tile",
            () -> new DecorativePorcelainTileBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)));

    public static final DeferredBlock<Block> L3E_TILE = registerBlock("l3e_tile",
            () -> new DecorativePorcelainTileBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)));

    public static final DeferredBlock<Block> WHITE_TILE = registerBlock("white_tile",
            () -> new DecorativePorcelainTileBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)));

    public static final DeferredBlock<Block> IRON_WEDGE = BLOCKS.register("iron_wedge",
            () -> new IronWedgeBlock(BlockBehaviour.Properties.of().strength(2f).noOcclusion()));

    public static final DeferredBlock<Block> PROOFING_BOX = registerBlock("proofing_box",
            () -> new ProofingBoxBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final DeferredBlock<Block> BAKERS_TABLE = registerBlock("bakers_table",
            () -> new BakersTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final DeferredBlock<Block> DOUGH_DIVIDER = registerBlock("dough_divider",
            () -> new DoughDividerBlock(
                    Block.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                            .noOcclusion()                 // ← important
                            .strength(3.5F)
                            .requiresCorrectToolForDrops()
            )
    );

    public static final DeferredBlock<Block> MACHINE_HOUSING = registerBlock("machine_housing",
            () -> new Block(
                    BlockBehaviour.Properties
                            .of()
                            .strength(4f, 6f)                 // hardness & blast-resistance
                            .requiresCorrectToolForDrops()    // needs pickaxe
            )
    );

    public static final DeferredBlock<Block> HARD_RED_SPRING_WHEAT_CROP = registerBlock("hard_red_spring_wheat_crop",
            () -> new HardRedSpringWheatCrop(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));

    public static final DeferredBlock<Block> WILD_WHEAT = registerBlock("wild_wheat",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).offsetType(BlockBehaviour.OffsetType.XZ)));


    public static final DeferredBlock<Block> PINE_WOOD = registerBlock("pine_wood",
            () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)));


    public static final DeferredBlock<Block> PINE_LOG = registerBlock("pine_log",
            () -> new PineResinLogBlock(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.OAK_LOG)
                    .strength(2.0f))
    );
    public static final DeferredBlock<Block> STRIPPED_PINE_LOG = registerBlock("stripped_pine_log",
            () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)));


    public static final DeferredBlock<DoorBlock> PINE_DOOR = registerDoor("pine_door",
            () -> new DoorBlock(BlockSetType.OAK,
                    BlockBehaviour.Properties.of().strength(3.0F).noOcclusion()));

    public static final DeferredBlock<TrapDoorBlock> PINE_TRAPDOOR = registerBlock("pine_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.OAK,
                    BlockBehaviour.Properties.of().strength(3.0F).noOcclusion()));

    public static final DeferredBlock<ButtonBlock> PINE_BUTTON = registerBlock("pine_button",
            // Wooden button behavior: longer press time; no collision
            () -> new ButtonBlock(BlockSetType.OAK, 30,
                    BlockBehaviour.Properties.of().noCollission().strength(0.5F).ignitedByLava()));

    public static final DeferredBlock<Block> PINE_PRESSURE_PLATE = registerBlock(
            "pine_pressure_plate",
            () -> new PressurePlateBlock(
                    BlockSetType.OAK,                              // ← gives EVERYTHING sensitivity
                    BlockBehaviour.Properties.of().strength(0.5F)
            )
    );

    private static DeferredBlock<DoorBlock> registerDoor(String name, Supplier<DoorBlock> block) {
        DeferredBlock<DoorBlock> ref = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new DoubleHighBlockItem(ref.get(), new Item.Properties()));
        return ref;
    }

    public static final DeferredBlock<EnergyStorageBlock> BATTERY =
            registerBlock("battery",
                    () -> new EnergyStorageBlock(Block.Properties.of().strength(3f))
            );

    // CABLE
    public static final DeferredBlock<EnergyCableBlock> ENERGY_CABLE =
            registerBlock("energy_cable",
                    () -> new EnergyCableBlock(Block.Properties.of().strength(1f).noOcclusion())
            );

    public static final DeferredBlock<WoodGasPipe> WOODGAS_PIPE =
            registerBlock("woodgas_pipe",
                    () -> new WoodGasPipe(Block.Properties.of().strength(1f).noOcclusion())
            );

    public static final DeferredBlock<WoodGasEngineBlock> WOODGAS_ENGINE =
            registerBlock("woodgas_engine",
                    () -> new WoodGasEngineBlock(Block.Properties.of().strength(1f).noOcclusion())
            );

    public static final DeferredBlock<Block> PINE_SAPLING = registerBlock("pine_sapling",
            () -> new ModSaplingBlock(ModTreeGrowers.PINE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING), Blocks.GRASS_BLOCK));


    public static final DeferredBlock<Block> STRIPPED_PINE_WOOD = registerBlock("stripped_pine_wood",
            () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD)));


    public static final DeferredBlock<Block> PINE_PLANKS = registerBlock("pine_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)) {
                @Override
                public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return true;
                }

                @Override
                public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 20;
                }

                @Override
                public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 5;
                }
            });

    public static final DeferredBlock<Block> TREE_TAP =
            registerBlock("tree_tap",
                    () -> new TreeTapBlock(Block.Properties.of().strength(1f).noOcclusion()));

    public static final DeferredBlock<Block> PINE_STAIRS = registerBlock("pine_stairs",
        () -> new StairBlock(ModBlocks.PINE_PLANKS.get().defaultBlockState(),
        BlockBehaviour.Properties.of().strength(2f).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> PINE_SLAB = registerBlock("pine_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of().strength(2f).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> PINE_FENCE = registerBlock("pine_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.of().strength(2f).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> PINE_FENCE_GATE = registerBlock("pine_fence_gate",
            () -> new FenceGateBlock(WoodType.OAK, BlockBehaviour.Properties.ofFullCopy(ModBlocks.PINE_PLANKS.get())));

    public static final DeferredBlock<Block> PINE_LEAVES = registerBlock("pine_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LEAVES)) {
                @Override
                public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return true;
                }

                @Override
                public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 60;
                }

                @Override
                public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 30;
                }
            });

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }


    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);

    }
}

