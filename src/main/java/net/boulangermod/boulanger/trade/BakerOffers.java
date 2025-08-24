package net.boulangermod.boulanger.trade;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.BreadType;
import net.boulangermod.boulanger.item.FlourItemType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;
import java.util.Map;

public final class BakerOffers {

    /** Creates a finished baguette bread ItemStack with all the components from your log line. */
    public static ItemStack baguetteBread() {
        ItemStack stack = new ItemStack(ModItems.BREAD.get(), 1);

        // Vanilla custom model data
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));

        // Your components
        stack.set(ModDataComponentTypes.PAN_TYPE.get(), PanTypeComponent.of(PanType.BAGUETTE));
        stack.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), WeightComponent.ofGrams(455f));
        stack.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(),
                BakerPctComponent.of(Map.of(
                        IngredientCategory.FLOUR, 100.0,
                        IngredientCategory.WATER, 70.0,
                        IngredientCategory.YEAST, 4.0,
                        IngredientCategory.SALT,  3.0
                )));

        // --- Dough recipe snapshot --------------------------------------------------
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "baguette");

        // Single flour item; attach which flour via flourType on IngredientInfo
        final String FLOUR_ITEM_ID = BuiltInRegistries.ITEM.getKey(ModItems.FLOUR_ITEM.get()).toString();

        List<IngredientInfo> ingredients = List.of(
                // yeast
                IngredientInfo.of(
                        BuiltInRegistries.ITEM.getKey(ModItems.FLEISCHMANN.get()).toString(),
                        IngredientCategory.YEAST, 10
                ),
                // bread flour (single flour item id + flourType variant)
                IngredientInfo.of(FLOUR_ITEM_ID, IngredientCategory.FLOUR, 244)
                        .withFlourType(FlourItemType.BREAD_FLOUR.toFlourType()),
                // water
                IngredientInfo.of(
                        BuiltInRegistries.ITEM.getKey(Items.WATER_BUCKET).toString(),
                        IngredientCategory.WATER, 180
                ),
                // vital wheat gluten (flour variant)
                IngredientInfo.of(FLOUR_ITEM_ID, IngredientCategory.FLOUR, 13)
                        .withFlourType(FlourItemType.VITAL_WHEAT_GLUTEN.toFlourType()),
                // salt
                IngredientInfo.of(
                        BuiltInRegistries.ITEM.getKey(ModItems.SALT_KOSHER.get()).toString(),
                        IngredientCategory.SALT, 8
                )
        );

        Map<IngredientCategory, Double> targets = Map.of(
                IngredientCategory.FLOUR, 100.0,
                IngredientCategory.WATER, 70.0,
                IngredientCategory.YEAST, 4.0,
                IngredientCategory.SALT,  3.0
        );

        stack.set(ModDataComponentTypes.DOUGH_RECIPE.get(),
                DoughRecipeComponent.of(recipeId, ingredients, /*totalWeight*/455, targets));

        // Bread type (string id)
        stack.set(ModDataComponentTypes.BREAD_TYPE.get(), BreadType.BAGUETTE);

        return stack;
    }

    private BakerOffers() {}
}
