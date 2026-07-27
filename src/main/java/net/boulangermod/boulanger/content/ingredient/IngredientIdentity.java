package net.boulangermod.boulanger.content.ingredient;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.value.BakeryAdditiveComponent;
import net.boulangermod.boulanger.component.value.FoodAdditiveComponent;
import net.boulangermod.boulanger.component.value.IngredientItemComponent;
import net.boulangermod.boulanger.content.additive.BakeryAdditiveType;
import net.boulangermod.boulanger.content.additive.FoodAdditiveType;
import net.boulangermod.boulanger.content.flour.FlourType;
import net.boulangermod.boulanger.content.ingredient.profile.IngredientProfiles;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record IngredientIdentity(
        Item sourceItem,
        IngredientCategory category,
        @Nullable FlourType flourType,
        @Nullable FoodAdditiveComponent foodAdditive,
        @Nullable BakeryAdditiveComponent bakeryAdditive
) {
    public IngredientIdentity {
        Objects.requireNonNull(sourceItem, "sourceItem");
        Objects.requireNonNull(category, "category");
    }

    public static IngredientIdentity from(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");

        if (stack.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot extract ingredient identity from an empty stack"
            );
        }

        FlourType flour =
                stack.get(ModDataComponentTypes.FLOUR_TYPE.get());

        FoodAdditiveComponent foodAdditive =
                stack.get(ModDataComponentTypes.FOOD_ADDITIVE.get());

        BakeryAdditiveComponent bakeryAdditive =
                stack.get(ModDataComponentTypes.BAKERY_ADDITIVE.get());

        IngredientItemComponent sourceComponent =
                stack.get(ModDataComponentTypes.INGREDIENT_TYPE.get());

        Item sourceItem = sourceComponent != null
                ? sourceComponent.item()
                : stack.getItem();

        IngredientCategory category =
                stack.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get());

        if (category == null && flour != null) {
            category = IngredientCategory.FLOUR;
        }

        if (category == null && foodAdditive != null) {
            category = FoodAdditiveType.byId(foodAdditive.id())
                    .map(FoodAdditiveType::category)
                    .orElse(null);
        }

        if (category == null && bakeryAdditive != null) {
            category = BakeryAdditiveType.byId(bakeryAdditive.id())
                    .map(BakeryAdditiveType::category)
                    .orElse(null);
        }

        if (category == null) {
            category = IngredientProfiles.profileOf(stack).category();
        }

        return new IngredientIdentity(
                sourceItem,
                category,
                flour,
                foodAdditive,
                bakeryAdditive
        );
    }

    public void applyTo(ItemStack target) {
        Objects.requireNonNull(target, "target");

        target.set(
                ModDataComponentTypes.INGREDIENT_TYPE.get(),
                new IngredientItemComponent(sourceItem)
        );

        target.set(
                ModDataComponentTypes.INGREDIENT_CATEGORY.get(),
                category
        );

        applyOrRemove(
                target,
                ModDataComponentTypes.FLOUR_TYPE.get(),
                flourType
        );

        applyOrRemove(
                target,
                ModDataComponentTypes.FOOD_ADDITIVE.get(),
                foodAdditive
        );

        applyOrRemove(
                target,
                ModDataComponentTypes.BAKERY_ADDITIVE.get(),
                bakeryAdditive
        );
    }

    private static <T> void applyOrRemove(
            ItemStack target,
            net.minecraft.core.component.DataComponentType<T> component,
            @Nullable T value
    ) {
        if (value != null) {
            target.set(component, value);
        } else {
            target.remove(component);
        }
    }
}