package org.l3e.Boulanger.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.l3e.Boulanger.Boulanger;
import org.l3e.Boulanger.item.ModItems;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Boulanger.MOD_ID);

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    public static final RegistryObject<Block> HARD_RED_SPRING_WHEAT_CROP = BLOCKS.register("hard_red_spring_wheat_crop",
            () -> new HardRedSpringWheat(BlockBehaviour.Properties.copy(Blocks.WHEAT)));
    public static final RegistryObject<Block> HARD_RED_WINTER_WHEAT_CROP = BLOCKS.register("hard_red_winter_wheat_crop",
            () -> new HardRedWinterWheat(BlockBehaviour.Properties.copy(Blocks.WHEAT)));
    public static final RegistryObject<Block> STONE_MILL = registerBlock("stone_mill",
            () -> new StoneMillBlock(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(1.5f).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> THRESHING_MACHINE = registerBlock("threshing_machine",
            () -> new ThreshingMachineBlock(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(1.5f).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> SEPARATOR = registerBlock("separator",
            () -> new SeparatorBlock(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(1.5f).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> ASPIRATOR = registerBlock("aspirator",
            () -> new AspiratorBlock(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(1.5f).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> DESTONER = registerBlock("destoner",
            () -> new DestonerBlock(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(1.5f).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> CHECKERED_BAKERY_TILE = registerBlock("checkered_bakery_tile",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(1.5f).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> BLUE_BAKERY_TILE = registerBlock("blue_bakery_tile",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(1.5f).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> WHITE_BAKERY_TILE = registerBlock("white_bakery_tile",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(1.5f).requiresCorrectToolForDrops().noOcclusion()));



    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

        public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}

