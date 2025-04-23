package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.entity.ModEntities;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.boulangermod.boulanger.Boulanger;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Boulanger.MODID);

    // Milling ingredients and Flours
    public static final DeferredItem<Item> WHEAT_BERRIES = ITEMS.registerSimpleItem("wheat_berries");
    public static final DeferredItem<Item> FLOUR_ITEM = ITEMS.register("flour",
            () -> new FlourItem(new Item.Properties()));

    public static final DeferredItem<Item> KAOLINITE_CLAY_BALL = ITEMS.registerSimpleItem("kaolinite_clay_ball");
    public static final DeferredItem<Item> FANCY_EGG = ITEMS.registerSimpleItem("fancy_egg");
    public static final DeferredItem<Item> WOODEN_BUCKET = ITEMS.registerSimpleItem("wooden_bucket");
    public static final DeferredItem<Item> WOODEN_BUCKET_OF_SHELL_EGG = ITEMS.registerSimpleItem("wooden_shell_egg_bucket");
    public static final DeferredItem<Item> WOODEN_BUCKET_OF_WHOLE_MILK = ITEMS.registerSimpleItem("wooden_milk_bucket");
    public static final DeferredItem<Item> WHOLE_MILK = ITEMS.registerSimpleItem("whole_milk");
    public static final DeferredItem<Item> BONE_ASH = ITEMS.registerSimpleItem("bone_ash");
    public static final DeferredItem<Item> GLASS_DUST = ITEMS.registerSimpleItem("glass_dust");
    public static final DeferredItem<Item> PORCELAIN_MIX = ITEMS.registerSimpleItem("porcelain_mix");
    public static final DeferredItem<Item> UNFIRED_PORCELAIN_BRICK = ITEMS.registerSimpleItem("unfired_porcelain_brick");
    public static final DeferredItem<Item> PORCELAIN_BRICK = ITEMS.registerSimpleItem("porcelain_brick");
    public static final DeferredItem<Item> SPLIT_PINE_LOGS = ITEMS.registerSimpleItem("split_pine_logs");
    public static final DeferredItem<Item> BRICK_MOLD = ITEMS.registerSimpleItem("brick_mold");
    public static final DeferredItem<Item> DIORITE_PLATE = ITEMS.registerSimpleItem("diorite_plate");
    public static final DeferredItem<Item> PINE_RESIN = ITEMS.registerSimpleItem("pine_resin");
    public static final DeferredItem<Item> PCB = ITEMS.registerSimpleItem("pcb");
    public static final DeferredItem<Item> GILDED_PCB = ITEMS.registerSimpleItem("gilded_pcb");
    public static final DeferredItem<Item> REINFORCED_DIORITE_PLATE = ITEMS.registerSimpleItem("reinforced_diorite_plate");
    public static final DeferredItem<Item> DIORITE_BRICK = ITEMS.registerSimpleItem("diorite_brick");
    public static final DeferredItem<Item> FILLED_BOWL_ITEM = ITEMS.register("filled_bowl",
            () -> new FilledBowlItem(new Item.Properties()));

    // Dairy and Eggs (Food Additives)
    public static final DeferredItem<Item> BUTTER =
            ITEMS.register("butter", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.BUTTER));

    public static final DeferredItem<Item> EURO_BUTTER =
            ITEMS.register("euro_butter", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.EUROPEAN_BUTTER));

    public static final DeferredItem<Item> EURO_BUTTER_BLEND =
            ITEMS.register("euro_butter_blend", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.EUROPEAN_BUTTER_BLEND));

    // Yeasts (Food Additives)
    public static final DeferredItem<Item> SAF_RED =
            ITEMS.register("saf_red", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.SAF_RED_YEAST));

    public static final DeferredItem<Item> SAF_GOLD =
            ITEMS.register("saf_gold", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.SAF_GOLD_YEAST));

    public static final DeferredItem<Item> FLEISCHMANN =
            ITEMS.register("fleischmann", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.FLEISCHMANNS_YEAST));

    public static final DeferredItem<Item> FRESH_YEAST =
            ITEMS.register("fresh_yeast", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.FRESH_YEAST));

    // Other additives (Food Additives)
    public static final DeferredItem<Item> SALT_KOSHER =
            ITEMS.register("salt_kosher", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.SALT));

    // Other items (Non–additives)
    public static final DeferredItem<Item> DOUGH =
            ITEMS.register("generic_dough",
                    () -> new DoughItem(new Item.Properties()));

    public static final DeferredItem<Item> BREAD =
            ITEMS.register("bread",
                    () -> new BreadItem(new Item.Properties()));

    public static final DeferredItem<Item> HARD_RED_SPRING_WHEAT = ITEMS.registerSimpleItem("hard_red_spring_wheat");
    public static final DeferredItem<Item> WHEAT_SEED = ITEMS.register("wheat_seeds",
            () -> new WheatSeedItem(
                    ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get(),
                    new Item.Properties()));

    public static final DeferredItem<Item> IRON_WEDGE = ITEMS.register("iron_wedge",
            () -> new IronWedgeItem(ModBlocks.IRON_WEDGE.get(),
                    new Item.Properties())
    );

    public static final DeferredItem<Item> SLEDGEHAMMER = ITEMS.register("sledgehammer",
            () -> new SledgehammerItem(new Item.Properties()));



    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
