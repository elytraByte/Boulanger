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
    public static final RegistryObject<Item> SAF_RED = ITEMS.register("saf_red",
            () -> new SafRedItem(new Item.Properties()));
    public static final RegistryObject<Item> HARD_RED_SPRING_WHEAT_SEEDS = ITEMS.register("hard_red_spring_wheat_seeds",
            () -> new ItemNameBlockItem(ModBlocks.HARD_RED_SPRING_WHEAT.get(), new Item.Properties()));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}
