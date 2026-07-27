package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.value.WeightComponent;
import net.boulangermod.boulanger.content.WheatVariety;
import net.boulangermod.boulanger.content.additive.BakeryAdditiveType;
import net.boulangermod.boulanger.content.additive.FoodAdditiveType;
import net.boulangermod.boulanger.content.flour.FiftyPoundBagType;
//pneumatic ducting deprecated for the time being
//import net.boulangermod.boulanger.item.pneumatic.BlindFlangeItem;
import net.boulangermod.boulanger.content.flour.FlourItemType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Boulanger.MOD_ID);

    public static final DeferredItem<Item> WHEAT_SEEDS = ITEMS.register("wheat_seeds",
            () -> new WheatSeedItem(
                    ModBlocks.WHEAT_BUSHEL_BLOCK.get(),
                    new Item.Properties()
                            .stacksTo(64)
                            .component(ModDataComponentTypes.WHEAT_VARIETY.get(), WheatVariety.HARD_RED_WINTER)
            )
    );

    public static final DeferredItem<Item> WHEAT_BUSHEL = ITEMS.register("wheat_bushel",
            () -> new WheatBushelItem(new Item.Properties()));

    public static final DeferredItem<Item> FLOUR_ITEM = ITEMS.register("flour",
            () -> new FlourItem(new Item.Properties()));


    public static final DeferredItem<Item> FILLED_BOWL =
            ITEMS.register("filled_bowl", () -> new FilledBowlItem(new Item.Properties().stacksTo(1)));


    // Flour bags
    public static final DeferredItem<Item> FIFTY_POUND_BAG =
            ITEMS.register("fifty_pound_bag", () -> new FiftyPoundBagItem(new Item.Properties().stacksTo(1)));


    public static ItemStack createFlourBag(FiftyPoundBagType type) {
        ItemStack stack = new ItemStack(ModItems.FIFTY_POUND_BAG.get());

        stack.set(ModDataComponentTypes.FLOUR_TYPE.get(), type.toFlourType());
        stack.set(ModDataComponentTypes.INGREDIENT_MILLIGRAMS.get(), new WeightComponent(FiftyPoundBagType.MAX_MILLIGRAMS));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.modelIndex()));
        stack.remove(DataComponents.CUSTOM_NAME);

        return stack;
    }

    public static final DeferredItem<Item> SPLIT_PINE_LOGS = ITEMS.registerSimpleItem("split_pine_logs");

    public static final DeferredItem<Item> PINE_RESIN = ITEMS.registerSimpleItem("pine_resin");

    public static final DeferredItem<Item> KAOLINITE_CLAY_BALL = ITEMS.registerSimpleItem("kaolinite_clay_ball");

    public static final DeferredItem<Item> WHEAT_BERRIES = ITEMS.registerSimpleItem("wheat_berries");

    public static final DeferredItem<BrickMoldItem> BRICK_MOLD =
            ITEMS.register("brick_mold",
                    () -> new BrickMoldItem(new Item.Properties().durability(256)));

    public static final DeferredItem<Item> KOSHER_SALT =
            ITEMS.register("kosher_salt", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(8), FoodAdditiveType.KOSHER_SALT));

    public static final DeferredItem<Item> MOLASSES =
            ITEMS.register("molasses", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.MOLASSES));

    public static final DeferredItem<Item> BROWN_SUGAR =
            ITEMS.register("brown_sugar", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(64), FoodAdditiveType.BROWN_SUGAR));

    public static final DeferredItem<Item> POWDERED_SUGAR =
            ITEMS.register("powdered_sugar", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(64), FoodAdditiveType.POWDERED_SUGAR));

    public static final DeferredItem<Item> BUTTER =
            ITEMS.register("butter", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.BUTTER));

    public static final DeferredItem<Item> EUROPEAN_BUTTER =
            ITEMS.register("european_butter", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.EUROPEAN_BUTTER));

    public static final DeferredItem<Item> EUROPEAN_BUTTER_BLEND =
            ITEMS.register("european_butter_blend", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.EUROPEAN_BUTTER_BLEND));

    public static final DeferredItem<Item> BUTTER_SALTED =
            ITEMS.register("butter_salted", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.BUTTER_SALTED));

    public static final DeferredItem<Item> EUROPEAN_BUTTER_SALTED =
            ITEMS.register("european_butter_salted", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.EUROPEAN_BUTTER_SALTED));

    public static final DeferredItem<Item> EUROPEAN_BUTTER_BLEND_SALTED =
            ITEMS.register("european_butter_blend_salted", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.EUROPEAN_BUTTER_BLEND_SALTED));

    public static final DeferredItem<Item> SAF_RED_YEAST =
            ITEMS.register("saf_red_yeast", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.SAF_RED_YEAST));

    public static final DeferredItem<Item> SAF_GOLD_YEAST =
            ITEMS.register("saf_gold_yeast", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.SAF_GOLD_YEAST));

    public static final DeferredItem<Item> FLEISCHMANNS_YEAST =
            ITEMS.register("fleischmanns_yeast", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.FLEISCHMANNS_YEAST));

    public static final DeferredItem<Item> BREWERS_YEAST =
            ITEMS.register("brewers_yeast", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.BREWERS_YEAST));

    public static final DeferredItem<Item> FRESH_YEAST =
            ITEMS.register("fresh_yeast", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.FRESH_YEAST));

    public static final DeferredItem<Item> RYE_SOUR_STARTER =
            ITEMS.register("rye_sour_starter", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(1), FoodAdditiveType.RYE_SOUR_STARTER));

    public static final DeferredItem<Item> SOURDOUGH_STARTER =
            ITEMS.register("sourdough_starter", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(1), FoodAdditiveType.SOURDOUGH_STARTER));

    public static final DeferredItem<Item> S_500_RED =
            ITEMS.register("s_500_red", () -> new BakeryAdditiveItem(new Item.Properties().stacksTo(1), BakeryAdditiveType.S_500_RED));

    public static final DeferredItem<Item> IM_PROVE_200 =
            ITEMS.register("im_prove_200", () -> new BakeryAdditiveItem(new Item.Properties().stacksTo(1), BakeryAdditiveType.IM_PROVE_200));

    public static final DeferredItem<Item> ADVANTAGE_500_CL =
            ITEMS.register("advantage_500_cl", () -> new BakeryAdditiveItem(new Item.Properties().stacksTo(1), BakeryAdditiveType.ADVANTAGE_500_CL));

    public static final DeferredItem<Item> SOYBEAN_OIL =
            ITEMS.register("soybean_oil", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(1), FoodAdditiveType.SOYBEAN_OIL));

    public static final DeferredItem<Item> CANOLA_OIL =
            ITEMS.register("canola_oil", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(1), FoodAdditiveType.CANOLA_OIL));

    public static final DeferredItem<Item> MARGARINE =
            ITEMS.register("margarine", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.MARGARINE));

    public static final DeferredItem<Item> LARD =
            ITEMS.register("lard", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.LARD));

    public static final DeferredItem<Item> DRY_WHOLE_MILK_POWDER =
            ITEMS.register("dry_whole_milk_powder", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.DRY_WHOLE_MILK_POWDER));

    public static final DeferredItem<Item> DRY_BUTTERMILK_POWDER =
            ITEMS.register("dry_buttermilk_powder", () -> new FoodAdditiveItem(new Item.Properties().stacksTo(16), FoodAdditiveType.DRY_BUTTERMILK_POWDER));

//pneumatic ducting deprecated for the time being
//    public static final DeferredItem<BlindFlangeItem> BLIND_FLANGE =
//            ITEMS.register("blind_flange",
//                    () -> new BlindFlangeItem(new Item.Properties().stacksTo(16)));


//    public static final DeferredHolder<Item, Item> BUTTER = ITEMS.register("butter",
//            () -> new FoodAdditiveItem(
//                    new Item.Properties()
//                            .stacksTo(64)
//                            .component(ModDataComponentTypes.FOOD_ADDITIVE.get(), new FoodAdditiveComponent("butter")),
//                    FoodAdditiveType.BUTTER
//            )
//    );

//    public static final DeferredHolder<Item, Item> ASCORBIC_ACID =ITEMS.register("ascorbic_acid", () ->
//            new BakeryAdditiveItem(
//        new Item.Properties()
//            .stacksTo(64)
//            .component(ModDataComponentTypes.FOOD_ADDITIVE.get(), BakeryAdditiveType.ASCORBIC_ACID.toComponent())
//            .component(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), IngredientCategory.ADDITIVE),
//    BakeryAdditiveType.ASCORBIC_ACID
//            )
//    );


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
