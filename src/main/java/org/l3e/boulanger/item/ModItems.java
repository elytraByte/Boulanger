package org.l3e.boulanger.item;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.l3e.boulanger.Boulanger;
import org.l3e.boulanger.block.ModBlocks;
import org.l3e.boulanger.component.ModDataCompnentTypes;
import org.l3e.boulanger.component.WheatType;

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
            () -> new ItemNameBlockItem(ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get(), new Item.Properties().component(ModDataCompnentTypes.WHEAT_TYPE.value(), new WheatType(0))));
    public static final DeferredItem<Item> BIG_FLOUR_AP = ITEMS.registerSimpleItem("big_flour_ap");
    public static final DeferredItem<Item> BIG_FLOUR_BR = ITEMS.registerSimpleItem("big_flour_br");
    public static final DeferredItem<Item> BIG_FLOUR_WW = ITEMS.registerSimpleItem("big_flour_ww");
    public static final DeferredItem<Item> BIG_FLOUR_GW = ITEMS.registerSimpleItem("big_flour_gw");
    public static final DeferredItem<Item> BIG_FLOUR_HG = ITEMS.registerSimpleItem("big_flour_hg");
    public static final DeferredItem<Item> BIG_FLOUR_PK = ITEMS.registerSimpleItem("big_flour_pk");
    public static final DeferredItem<Item> BIG_FLOUR_DU = ITEMS.registerSimpleItem("big_flour_du");
    public static final DeferredItem<Item> BIG_FLOUR_BLANK = ITEMS.register("big_flour_blank", ()-> 

    public static final DeferredItem<Item> RAW_BLACK_OPAL =
            ITEMS.registerItem("raw_black_opal", BigFlour::new, new Item.Properties());


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}