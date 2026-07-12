package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.value.IngredientItemComponent;
import net.boulangermod.boulanger.component.value.WeightComponent;
import net.boulangermod.boulanger.content.WheatVariety;
import net.boulangermod.boulanger.content.flour.FiftyPoundBagType;
import net.boulangermod.boulanger.content.ingredient.IngredientCategory;
import net.boulangermod.boulanger.item.pneumatic.BlindFlangeItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
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

    // Flour bags
    public static final DeferredItem<Item> FIFTY_POUND_BAG =
            ITEMS.register("fifty_pound_bag", () -> new FiftyPoundBagItem(new Item.Properties()));


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

    public static final DeferredItem<BlindFlangeItem> BLIND_FLANGE =
            ITEMS.register("blind_flange",
                    () -> new BlindFlangeItem(new Item.Properties().stacksTo(16)));


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
