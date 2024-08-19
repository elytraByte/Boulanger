package org.l3e.boulanger.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.l3e.boulanger.Boulanger;
import org.l3e.boulanger.block.ModBlocks;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Boulanger.MODID);

    public static final DeferredItem<Item> FLOUR_AP = ITEMS.registerSimpleItem("ap_flour");
    public static final DeferredItem<Item> YEAST_BREWERS= ITEMS.registerSimpleItem("yeast_brewers");
    public static final DeferredItem<Item> SALT_KOSHER = ITEMS.registerItem("salt_kosher", SaltKosher::new, new Item.Properties());
    public static final DeferredItem<Item> WRENCH = ITEMS.registerItem("wrench", WrenchToolItem::new, new Item.Properties());
    public static final DeferredItem<Item> DOUGH = ITEMS.registerSimpleItem("dough");
    public static final DeferredItem<Item> FLOUR_WW = ITEMS.registerSimpleItem("ww_flour");
    public static final DeferredItem<Item> HARD_RED_SPRING_WHEAT = ITEMS.registerSimpleItem("hard_red_spring_wheat");
    public static final DeferredItem<Item> HARD_RED_SPRING_WHEAT_SEEDS = ITEMS.register("hard_red_spring_wheat_seeds",
            () -> new ItemNameBlockItem(ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get(), new Item.Properties()));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
