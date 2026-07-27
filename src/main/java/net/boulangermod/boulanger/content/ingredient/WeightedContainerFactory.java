package net.boulangermod.boulanger.content.ingredient;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.value.WeightComponent;
import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.Objects;

public final class WeightedContainerFactory {
    private WeightedContainerFactory() {}

    public static ItemStack create(
            ItemLike container,
            IngredientIdentity identity,
            long milligrams
    ) {
        Objects.requireNonNull(container, "container");
        Objects.requireNonNull(identity, "identity");

        if (milligrams <= 0) {
            throw new IllegalArgumentException(
                    "milligrams must be positive"
            );
        }

        ItemStack result = new ItemStack(container);

        result.set(
                ModDataComponentTypes.INGREDIENT_MILLIGRAMS.get(),
                new WeightComponent(milligrams)
        );

        identity.applyTo(result);
        return result;
    }

    public static ItemStack createFilledBowl(
            IngredientIdentity identity,
            long milligrams
    ) {
        return create(
                ModItems.FILLED_BOWL.get(),
                identity,
                milligrams
        );
    }
}