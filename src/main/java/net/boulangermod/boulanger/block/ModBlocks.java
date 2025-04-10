package net.boulangermod.boulanger.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.crops.HardRedSpringWheatCrop;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.worldgen.tree.ModTreeGrowers;

import java.util.HashMap;
import java.util.Map;
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

    public static final DeferredBlock<Block> SCALE_BLOCK = registerBlock("scale_block",
            () -> new ScaleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

//    public static final DeferredBlock<Block> MIXING_BLOCK = registerBlock("mixing_block",
//            () -> new MixingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final DeferredBlock<Block> KAOLINITE_CLAY = registerBlock("kaolinite_clay",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CLAY)));

    public static final DeferredBlock<Block> BLACK_TILE = registerBlock("black_tile",
            () -> new DecorativePorcelainTileBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)));

    public static final DeferredBlock<Block> BLUE_TILE = registerBlock("blue_tile",
            () -> new DecorativePorcelainTileBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)));

    public static final DeferredBlock<Block> DARK_BLUE_TILE = registerBlock("dark_blue_tile",
            () -> new DecorativePorcelainTileBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)));

    public static final DeferredBlock<Block> DARK_BLUE_WHITE_TILE = registerBlock("dark_blue_white_tile",
            () -> new DecorativePorcelainTileBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)));

    public static final DeferredBlock<Block> L3E_TILE = registerBlock("l3e_tile",
            () -> new DecorativePorcelainTileBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)));

    public static final DeferredBlock<Block> WHITE_TILE = registerBlock("white_tile",
            () -> new DecorativePorcelainTileBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)));



//    public static final DeferredBlock<Block> PORCELAIN_TILE = registerBlock("porcelain_tile",)
//    public static final DeferredBlock<Block> KILN = registerBlock("kiln",)
//    public static final DeferredBlock<Block> PROOFER = registerBlock("proofer",)
//    public static final DeferredBlock<Block> WOOD_OVEN = registerBlock("wood_oven",)
//    public static final DeferredBlock<Block> FERMENTATION_JAR = registerBlock("fermentation_jar",)


    public static final DeferredBlock<Block> HARD_RED_SPRING_WHEAT_CROP = registerBlock("hard_red_spring_wheat_crop",
            () -> new HardRedSpringWheatCrop(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));

    public static final DeferredBlock<Block> WILD_WHEAT = registerBlock("wild_wheat",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final DeferredBlock<Block> PINE_LOG = registerBlock("pine_log",
            () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));

    public static final DeferredBlock<Block> PINE_WOOD = registerBlock("pine_wood",
            () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)));

    public static final DeferredBlock<Block> STRIPPED_PINE_LOG = registerBlock("stripped_pine_log",
            () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)));

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

    public static final DeferredBlock<Block> PINE_SAPLING = registerBlock("pine_sapling",
            () -> new ModSaplingBlock(ModTreeGrowers.PINE, BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_SAPLING), Blocks.TERRACOTTA));

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);

    }
}

