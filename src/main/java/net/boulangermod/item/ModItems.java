package net.boulangermod.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.boulangermod.Boulanger;
import net.boulangermod.item.ModCreativeModeTabs.*;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Boulanger.MODID);

    // Milling ingredients and Flours
    public static final DeferredItem<Item> WHEAT_BERRIES= ITEMS.registerSimpleItem("wheat_berries");
    public static final DeferredItem<Item> BRAN= ITEMS.registerSimpleItem("bran");
    public static final DeferredItem<Item> BREAK_FLOUR = ITEMS.registerSimpleItem("break_flour");
    public static final DeferredItem<Item> MIDDLINGS_FLOUR = ITEMS.registerSimpleItem("middlings_flour");
    public static final DeferredItem<Item> PATENT_FLOUR = ITEMS.registerSimpleItem("patent_flour");
    public static final DeferredItem<Item> SEMOLINA_FLOUR = ITEMS.registerSimpleItem("semolina_flour");

    public static final DeferredItem<Item> FIFTY_POUND_FLOUR= ITEMS.registerSimpleItem("fifty_pound_flour");


    public static final DeferredItem<Item> WHOLE_WHEAT_FLOUR = ITEMS.register("whole_wheat_flour",
            () -> new FlourItem(new Item.Properties(),"whole_wheat_flour")); // Correct tab and flourType

    public static final DeferredItem<Item> RYE_FLOUR = ITEMS.register("rye_flour",
            () -> new FlourItem(new Item.Properties(), "rye_flour")); // Correct tab and flourType

    // Add more flour types here, following the same pattern
    public static final DeferredItem<Item> BREAD_FLOUR = ITEMS.register("bread_flour",
            () -> new FlourItem(new Item.Properties(), "bread_flour"));

    public static final DeferredItem<Item> CAKE_FLOUR = ITEMS.register("cake_flour",
            () -> new FlourItem(new Item.Properties(),"cake_flour"));

    public static final DeferredItem<Item> HIGH_GLUTEN_FLOUR = ITEMS.register("high_gluten_flour",
            () -> new FlourItem(new Item.Properties(), "high_gluten_flour"));

    public static final DeferredItem<Item> WHITE_WHOLE_WHEAT_FLOUR = ITEMS.register("white_whole_wheat_flour",
            () -> new FlourItem(new Item.Properties(), "white_whole_wheat_flour"));


    //Dairy and Eggs
    public static final DeferredItem<Item> BUTTER= ITEMS.registerSimpleItem("butter");
//    public static final DeferredItem<Item> BUTTER_SALTED= ITEMS.registerSimpleItem("");
    public static final DeferredItem<Item> EURO_BUTTER= ITEMS.registerSimpleItem("euro_butter");
//    public static final DeferredItem<Item> EURO_BUTTER_SALTED= ITEMS.registerSimpleItem("");
    public static final DeferredItem<Item> EURO_BUTTER_BLEND= ITEMS.registerSimpleItem("euro_butter_blend");
//    public static final DeferredItem<Item> EURO_BUTTER_BLEND_SALTED= ITEMS.registerSimpleItem("yeast_brewers");

    //Yeasts
    public static final DeferredItem<Item> SAF_RED= ITEMS.registerSimpleItem("saf_red");
    public static final DeferredItem<Item> SAF_GOLD= ITEMS.registerSimpleItem("saf_gold");
    public static final DeferredItem<Item> FLEISCHMANN= ITEMS.registerSimpleItem("fleischmann");
    public static final DeferredItem<Item> FRESH_YEAST= ITEMS.registerSimpleItem("fresh_yeast");

    //Other
    public static final DeferredItem<Item> SALT_KOSHER = ITEMS.registerSimpleItem("salt_kosher");
    public static final DeferredItem<Item> DOUGH = ITEMS.registerSimpleItem("dough");

    public static final DeferredItem<Item> HARD_RED_SPRING_WHEAT = ITEMS.registerSimpleItem("hard_red_spring_wheat");
//    public static final DeferredItem<Item> HARD_RED_SPRING_WHEAT_SEEDS = ITEMS.register("hard_red_spring_wheat_seeds"),
//            () -> new ItemNameBlockItem(ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get(),
//            new Item.Properties().component(ModDataComponententTypes.WHEAT_TYPE.value(), new WheatType(0))));
//    public static final DeferredItem<Item> BIG_FLOUR_AP = ITEMS.register("big_flour_ap", () -> new BigFlour(new Item.Properties().durability(226796)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}