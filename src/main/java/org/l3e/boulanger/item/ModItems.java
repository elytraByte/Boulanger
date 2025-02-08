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

    // Milling ingredients and Flours
    public static final DeferredItem<Item> WHEAT_BERRIES= ITEMS.registerSimpleItem("wheat_berries");
    public static final DeferredItem<Item> BRAN= ITEMS.registerSimpleItem("bran");
    public static final DeferredItem<Item> BREAK_FLOUR = ITEMS.registerSimpleItem("break_flour");
    public static final DeferredItem<Item> MIDDLINGS_FLOUR = ITEMS.registerSimpleItem("middlings_flour");
    public static final DeferredItem<Item> PATENT_FLOUR = ITEMS.registerSimpleItem("patent_flour");
    public static final DeferredItem<Item> RYE_FLOUR = ITEMS.registerSimpleItem("rye_flour");
    public static final DeferredItem<Item> SEMOLINA_FLOUR = ITEMS.registerSimpleItem("semolina_flour");
    public static final DeferredItem<Item> WHOLE_WHEAT_FLOUR = ITEMS.registerSimpleItem("whole_wheat_flour");
    public static final DeferredItem<Item> ALL_PURPOSE_FLOUR = ITEMS.registerSimpleItem("all_purpose_flour");
    public static final DeferredItem<Item> BREAD_FLOUR = ITEMS.registerSimpleItem("bread_flour");
    public static final DeferredItem<Item> HIGH_GLUTEN_FLOUR = ITEMS.registerSimpleItem("high_gluten_flour");
    public static final DeferredItem<Item> VITAL_WHEAT_GLUTEN = ITEMS.registerSimpleItem("vital_wheat_gluten");
    public static final DeferredItem<Item> FIFTY_POUND_FLOUR= ITEMS.registerSimpleItem("fifty_pound_flour");

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
//            new Item.Properties().component(ModDataCompnentTypes.WHEAT_TYPE.value(), new WheatType(0))));
//    public static final DeferredItem<Item> BIG_FLOUR_AP = ITEMS.register("big_flour_ap", () -> new BigFlour(new Item.Properties().durability(226796)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}