package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WheatVarietyRecord;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.boulangermod.boulanger.Boulanger;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Boulanger.MODID);

    // Milling ingredients and Flours
    public static final DeferredItem<Item> WHEAT_BERRIES = ITEMS.registerSimpleItem("wheat_berries");
    public static final DeferredItem<Item> FLOUR_ITEM = ITEMS.register("flour",
            () -> new FlourItem(new Item.Properties())
    );

    public static final DeferredItem<Item> KAOLINITE_CLAY_BALL = ITEMS.registerSimpleItem("kaolinite_clay_ball");
    public static final DeferredItem<Item> BONE_ASH = ITEMS.registerSimpleItem("bone_ash");
    public static final DeferredItem<Item> GLASS_DUST = ITEMS.registerSimpleItem("glass_dust");
    public static final DeferredItem<Item> PORCELAIN_MIX = ITEMS.registerSimpleItem("porcelain_mix");
    public static final DeferredItem<Item> UNFIRED_PORCELAIN_BRICK = ITEMS.registerSimpleItem("unfired_porcelain_brick");
    public static final DeferredItem<Item> PORCELAIN_BRICK = ITEMS.registerSimpleItem("porcelain_brick");
    public static final DeferredItem<Item> SPLIT_PINE_LOGS = ITEMS.registerSimpleItem("split_pine_logs");
    public static final DeferredItem<Item> FILLED_BOWL_ITEM = ITEMS.register("filled_bowl",
            () -> new FilledBowlItem(new Item.Properties()));

    //Dairy and Eggs
    public static final DeferredItem<Item> BUTTER = ITEMS.registerSimpleItem("butter");
//    public static final DeferredItem<Item> BUTTER_SALTED = ITEMS.registerSimpleItem("");
    public static final DeferredItem<Item> EURO_BUTTER = ITEMS.registerSimpleItem("euro_butter");
//    public static final DeferredItem<Item> EURO_BUTTER_SALTED = ITEMS.registerSimpleItem("");
    public static final DeferredItem<Item> EURO_BUTTER_BLEND = ITEMS.registerSimpleItem("euro_butter_blend");
//    public static final DeferredItem<Item> EURO_BUTTER_BLEND_SALTED = ITEMS.registerSimpleItem("yeast_brewers");

    //Yeasts
    public static final DeferredItem<Item> SAF_RED = ITEMS.registerSimpleItem("saf_red");
    public static final DeferredItem<Item> SAF_GOLD = ITEMS.registerSimpleItem("saf_gold");
    public static final DeferredItem<Item> FLEISCHMANN = ITEMS.registerSimpleItem("fleischmann");
    public static final DeferredItem<Item> FRESH_YEAST = ITEMS.registerSimpleItem("fresh_yeast");

    //Other
    public static final DeferredItem<Item> SALT_KOSHER = ITEMS.registerSimpleItem("salt_kosher");
    public static final DeferredItem<Item> DOUGH = ITEMS.registerSimpleItem("generic_dough");
    public static final DeferredItem<Item> HARD_RED_SPRING_WHEAT = ITEMS.registerSimpleItem("hard_red_spring_wheat");
    public static final DeferredItem<Item> WHEAT_SEED = ITEMS.register("wheat_seeds",
            () -> new WheatSeedItem(
                    ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get(),
                    new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}