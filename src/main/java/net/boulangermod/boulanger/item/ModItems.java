package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.component.IngredientTypeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.PanTypeComponent;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.entity.ModEntities;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomModelData;
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
    public static final DeferredItem<Item> WOODEN_BUCKET = ITEMS.registerSimpleItem("wooden_bucket");
    public static final DeferredItem<Item> WOODEN_BUCKET_OF_SHELL_EGG = ITEMS.registerSimpleItem("wooden_shell_egg_bucket");
    public static final DeferredItem<Item> WOODEN_BUCKET_OF_WHOLE_MILK = ITEMS.registerSimpleItem("wooden_milk_bucket");
    public static final DeferredItem<Item> BONE_ASH = ITEMS.registerSimpleItem("bone_ash");
    public static final DeferredItem<Item> GLASS_DUST = ITEMS.registerSimpleItem("glass_dust");
    public static final DeferredItem<Item> PORCELAIN_MIX = ITEMS.registerSimpleItem("porcelain_mix");
    public static final DeferredItem<Item> UNFIRED_PORCELAIN_BRICK = ITEMS.registerSimpleItem("unfired_porcelain_brick");
    public static final DeferredItem<Item> UNFIRED_LIGHT_BLUE_PORCELAIN_BRICK = ITEMS.registerSimpleItem("unfired_light_blue_porcelain_brick");
    public static final DeferredItem<Item> UNFIRED_BLUE_PORCELAIN_BRICK = ITEMS.registerSimpleItem("unfired_blue_porcelain_brick");
    public static final DeferredItem<Item> UNFIRED_BLACK_PORCELAIN_BRICK = ITEMS.registerSimpleItem("unfired_black_porcelain_brick");
    public static final DeferredItem<Item> PORCELAIN_BRICK = ITEMS.registerSimpleItem("porcelain_brick");
    public static final DeferredItem<Item> LIGHT_BLUE_PORCELAIN_BRICK = ITEMS.registerSimpleItem("light_blue_porcelain_brick");
    public static final DeferredItem<Item> BLUE_PORCELAIN_BRICK = ITEMS.registerSimpleItem("blue_porcelain_brick");
    public static final DeferredItem<Item> BLACK_PORCELAIN_BRICK = ITEMS.registerSimpleItem("black_porcelain_brick");
    public static final DeferredItem<Item> SPLIT_PINE_LOGS = ITEMS.registerSimpleItem("split_pine_logs");
    public static final DeferredItem<BrickMoldItem> BRICK_MOLD =
            ITEMS.register("brick_mold",
                    () -> new BrickMoldItem(new Item.Properties().durability(256)));
    public static final DeferredItem<Item> DIORITE_PLATE = ITEMS.registerSimpleItem("diorite_plate");
    public static final DeferredItem<Item> PINE_RESIN = ITEMS.registerSimpleItem("pine_resin");
    public static final DeferredItem<Item> COPPER_CHANNEL = ITEMS.registerSimpleItem("copper_channel");
    public static final DeferredItem<Item> PCB = ITEMS.registerSimpleItem("pcb");
    public static final DeferredItem<Item> GILDED_PCB = ITEMS.registerSimpleItem("gilded_pcb");
    public static final DeferredItem<Item> REINFORCED_DIORITE_PLATE = ITEMS.registerSimpleItem("reinforced_diorite_plate");
    public static final DeferredItem<Item> DIORITE_BRICK = ITEMS.registerSimpleItem("diorite_brick");
    public static final DeferredItem<Item> FILLED_BOWL_ITEM = ITEMS.register("filled_bowl",
            () -> new FilledBowlItem(new Item.Properties()));
    public static final DeferredItem<Item> WOOD_GAS_BUCKET = ITEMS.registerSimpleItem("wood_gas_bucket");
    public static final DeferredItem<Item> MILLIGRAM_SCALE = ITEMS.register("milligram_scale",
            () -> new MilligramScaleItem(new Item.Properties()));

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
    public static final DeferredItem<Item> BREWERS_YEAST =
            ITEMS.register("brewers_yeast", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.BREWERS_YEAST));
    public static final DeferredItem<Item> FLEISCHMANN =
            ITEMS.register("fleischmann", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.FLEISCHMANNS_YEAST));
    public static final DeferredItem<Item> SOURDOUGH_STARTER =
            ITEMS.register("sourdough_starter", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.SOURDOUGH_STARTER));
    public static final DeferredItem<Item> RYE_SOUR_STARTER =
            ITEMS.register("rye_sour", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.RYE_SOUR_STARTER));
    public static final DeferredItem<Item> CANOLA_OIL =
            ITEMS.register("canola_oil", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.CANOLA_OIL));
    public static final DeferredItem<Item> SOYBEAN_OIL =
            ITEMS.register("soybean_oil", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.SOYBEAN_OIL));
    public static final DeferredItem<Item> FRESH_YEAST =
            ITEMS.register("fresh_yeast", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.FRESH_YEAST));
    public static final DeferredItem<Item> MOLASSES =
            ITEMS.register("molasses", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.MOLASSES));
    public static final DeferredItem<Item> POWDERED_SUGAR =
            ITEMS.register("powdered_sugar", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.POWDERED_SUGAR));
    public static final DeferredItem<Item> EGG_YOLK =
            ITEMS.register("egg_yolk", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.EGG_YOLK));
    public static final DeferredItem<Item> EGG_WHITE =
            ITEMS.register("egg_white", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.EGG_WHITE));
    public static final DeferredItem<Item> FANCY_EGG =
            ITEMS.register("fancy_egg", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.FANCY_EGG));
    public static final DeferredItem<Item> BROWN_SUGAR =
            ITEMS.register("brown_sugar", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.BROWN_SUGAR));
    public static final DeferredItem<Item> WHOLE_MILK =
            ITEMS.register("whole_milk", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.WHOLE_MILK));

    // Other additives (Food Additives)
    public static final DeferredItem<Item> SALT_KOSHER =
            ITEMS.register("salt_kosher", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.SALT));

    // Other items (Non–additives)
    public static final DeferredItem<Item> DOUGH =
            ITEMS.register("generic_dough", () -> new DoughItem(new Item.Properties()));

    public static final DeferredItem<Item> BREAD =
            ITEMS.register("bread", () -> new BreadItem(new Item.Properties()));

    public static final DeferredItem<Item> WHEAT_BUSHEL = ITEMS.register("wheat_bushel",
            () -> new WheatBushelItem(new Item.Properties()));

    public static final DeferredItem<Item> WHEAT_SEED = ITEMS.register("wheat_seeds",
            () -> new WheatSeedItem(ModBlocks.WHEAT_BUSHEL_BLOCK.get(), new Item.Properties()));


    public static final DeferredItem<Item> IRON_WEDGE = ITEMS.register("iron_wedge",
            () -> new IronWedgeItem(ModBlocks.IRON_WEDGE.get(), new Item.Properties()));
    public static final DeferredItem<Item> SLEDGEHAMMER = ITEMS.register("sledgehammer",
            () -> new SledgehammerItem(new Item.Properties()));

    public static final DeferredItem<SpawnEggItem> HEN_SPAWN_EGG = ITEMS.register("hen_spawn_egg",
            () -> new SpawnEggItem(
                    ModEntities.HEN.get(),
                    0xF2E6C5,
                    0xB07A4F,
                    new Item.Properties()
            )
    );

    // Bakery additives (BakeryAdditiveItem)
    public static final DeferredItem<Item> ASCORBIC_ACID =
            ITEMS.register("ascorbic_acid",
                    () -> new BakeryAdditiveItem(new Item.Properties().stacksTo(16), BakeryAdditiveType.ASCORBIC_ACID));
    public static final DeferredItem<Item> CALCIUM_PROPIONATE =
            ITEMS.register("calcium_propionate",
                    () -> new BakeryAdditiveItem(new Item.Properties().stacksTo(16), BakeryAdditiveType.CALCIUM_PROPIONATE));
    public static final DeferredItem<Item> DIASTATIC_MALT_POWDER =
            ITEMS.register("diastatic_malt_powder",
                    () -> new BakeryAdditiveItem(new Item.Properties().stacksTo(16), BakeryAdditiveType.DIASTATIC_MALT_POWDER));
    public static final DeferredItem<Item> NONDIASTATIC_MALT_POWDER =
            ITEMS.register("nondiastatic_malt_powder",
                    () -> new BakeryAdditiveItem(new Item.Properties().stacksTo(16), BakeryAdditiveType.NONDIASTATIC_MALT_POWDER));
    public static final DeferredItem<Item> L_CYSTEINE =
            ITEMS.register("l_cysteine",
                    () -> new BakeryAdditiveItem(new Item.Properties().stacksTo(16), BakeryAdditiveType.L_CYSTEINE));

    public static final DeferredItem<SpawnEggItem> HOLSTEIN_FRIESIAN_COW_SPAWN_EGG = ITEMS.register("holstein_friesian_cow_spawn_egg",
            () -> new SpawnEggItem(
                    ModEntities.HOLSTEIN_FRIESIAN.get(),
                    0xC0C0C0,
                    0x8B4513,
                    new Item.Properties()
            )
    );

    public static final DeferredItem<Item> GASIFIER_FILTER =
            ITEMS.register("filter_canister", () -> new Item(new Item.Properties().durability(1000)));

    // Pan registry and helpers
    public static final DeferredItem<Item> PAN =
            ITEMS.register("pan", () -> new PanItem(new Item.Properties(), PanType.LOAF));

    /** Creates a pan stack tagged with PAN_TYPE (and default empty CMD). */
    public static ItemStack createPan(PanType type) {
        ItemStack stack = new ItemStack(ModItems.PAN.get());
        stack.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent(type.getId()));
        // Give it a sensible default appearance (empty)
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.getEmptyModelIndex()));
        return stack;
    }

    /** Creates a pan stack tagged with PAN_TYPE and the requested empty/full appearance. */
    public static ItemStack createPanVariant(PanType type, boolean full) {
        ItemStack s = createPan(type); // sets PAN_TYPE + empty CMD
        s.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(full ? type.getFullModelIndex()
                : type.getEmptyModelIndex()));
        return s;
    }

    // Flour bags
    public static final DeferredItem<Item> FIFTY_POUND_BAG =
            ITEMS.register("fifty_pound_bag", () -> new FiftyPoundBagItem(new Item.Properties()));

    public static ItemStack createFlourBag(FiftyPoundBagType type) {
        ItemStack bag = new ItemStack(ModItems.FIFTY_POUND_BAG.get());
        bag.set(ModDataComponentTypes.FLOUR_TYPE.get(), type.toFlourType());
        bag.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(FiftyPoundBagItem.MAX_GRAMS));
        bag.set(ModDataComponentTypes.INGREDIENT_TYPE.get(), new IngredientTypeComponent(ModItems.FLOUR_ITEM.get()));
        bag.set(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), IngredientCategory.FLOUR);
        bag.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.getModelIndex()));
        bag.set(DataComponents.CUSTOM_NAME, Component.translatable("item.boulanger.fifty_pound_bag." + type.getId()));
        return bag;
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
