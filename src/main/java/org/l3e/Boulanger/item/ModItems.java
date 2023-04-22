package org.l3e.Boulanger.item;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.property.Properties;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.l3e.Boulanger.Boulanger;
import org.l3e.Boulanger.block.HardRedSpringWheat;
import org.l3e.Boulanger.block.ModBlocks;

import java.util.function.Supplier;


public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Boulanger.MOD_ID);

    public static final RegistryObject<Item> AP_FLOUR_SMALL = ITEMS.register("ap_flour_small",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> B_FLOUR_SMALL = ITEMS.register("b_flour_small",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HG_FLOUR_SMALL = ITEMS.register("hg_flour_small",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WW_FLOUR_SMALL = ITEMS.register("ww_flour_small",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> P_FLOUR_SMALL = ITEMS.register("p_flour_small",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LR_FLOUR_SMALL = ITEMS.register("lr_flour_small",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WHOLE_WHEAT_FLOUR = ITEMS.register("whole_wheat_flour",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BREAK_FLOUR = ITEMS.register("break_flour",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BRAN = ITEMS.register("bran",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WHEAT_CHAFF = ITEMS.register("wheat_chaff",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MIDDLINGS_FLOUR = ITEMS.register("middlings_flour",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STRAIGHT_FLOUR = ITEMS.register("straight_flour",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PATENT_FLOUR = ITEMS.register("patent_flour",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FIRST_CLEAR_FLOUR = ITEMS.register("first_clear_flour",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SECOND_CLEAR_FLOUR = ITEMS.register("second_clear_flour",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SAF_RED = ITEMS.register("saf_red",
            () -> new SafRedItem(new Item.Properties()));
    public static final RegistryObject<Item> SAF_GOLD = ITEMS.register("saf_gold",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WILD_YEAST = ITEMS.register("wild_yeast",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BREWERS_YEAST = ITEMS.register("brewers_yeast",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FLEISCHEMANNS_YEAST = ITEMS.register("fleischemanns_yeast",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PAPER_BAG = ITEMS.register("paper_bag",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WHEAT_BERRIES = ITEMS.register("wheat_berries",
            () -> new WheatBerries(new Item.Properties()));
    public static final RegistryObject<Item> HARD_RED_SPRING_WHEAT_SEEDS = ITEMS.register("hard_red_spring_wheat_seeds",
            () -> new ItemNameBlockItem(ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get(), new Item.Properties()));
    public static final RegistryObject<Item> HARD_RED_SPRING_WHEAT_ITEM = ITEMS.register("hard_red_spring_wheat_item",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HARD_RED_WINTER_WHEAT_SEEDS = ITEMS.register("hard_red_winter_wheat_seeds",
            () -> new ItemNameBlockItem(ModBlocks.HARD_RED_WINTER_WHEAT_CROP.get(), new Item.Properties()));
    public static final RegistryObject<Item> HARD_RED_WINTER_WHEAT_ITEM = ITEMS.register("hard_red_winter_wheat_item",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GRINDING_STONE = ITEMS.register("grinding_stone",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GRINDING_STONE_ASSEMBLY = ITEMS.register("grinding_stone_assembly",
            () -> new Item(new Item.Properties()));



    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}
