package net.boulangermod.boulanger.content.ingredient.mass;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.value.BakeryAdditiveComponent;
import net.boulangermod.boulanger.component.value.FoodAdditiveComponent;
import net.boulangermod.boulanger.component.value.WeightComponent;
import net.boulangermod.boulanger.content.additive.BakeryAdditiveType;
import net.boulangermod.boulanger.content.additive.FoodAdditiveType;
import net.boulangermod.boulanger.content.flour.FlourType;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Optional;

/**
 * Resolves only the mass and divisibility of an ingredient stack. It does not
 * mutate stacks, determine ingredient identity, or construct output containers.
 */
public final class IngredientMassResolver {
    private static final long VARIABLE_WEIGHT_UNIT_MILLIGRAMS = 1L;

    private IngredientMassResolver() {}

    public static Optional<IngredientMass> resolve(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        WeightComponent explicitWeight =
                stack.get(ModDataComponentTypes.INGREDIENT_MILLIGRAMS.get());
        if (explicitWeight != null) {
            return resolveExplicitWeight(stack, explicitWeight);
        }

        FlourType flour = stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
        if (flour != null) {
            return resolveUnits(flour.unitMg(), stack.getCount(), PortionMode.DISCRETE_UNITS);
        }

        FoodAdditiveComponent foodAdditive =
                stack.get(ModDataComponentTypes.FOOD_ADDITIVE.get());
        if (foodAdditive != null) {
            return FoodAdditiveType.byId(foodAdditive.id())
                    .flatMap(type -> resolveUnits(
                            type.unitMg(),
                            stack.getCount(),
                            PortionMode.DISCRETE_UNITS
                    ));
        }

        BakeryAdditiveComponent bakeryAdditive =
                stack.get(ModDataComponentTypes.BAKERY_ADDITIVE.get());
        if (bakeryAdditive != null) {
            return BakeryAdditiveType.byId(bakeryAdditive.id())
                    .flatMap(type -> resolveUnits(
                            type.unitMg(),
                            stack.getCount(),
                            PortionMode.DISCRETE_UNITS
                    ));
        }

        return IngredientMassRegistry.find(stack.getItem())
                .flatMap(entry -> resolveUnits(
                        entry.unitMilligrams(),
                        stack.getCount(),
                        entry.portionMode()
                ));
    }

    private static Optional<IngredientMass> resolveExplicitWeight(
            ItemStack stack,
            WeightComponent explicitWeight
    ) {
        /*
         * INGREDIENT_MILLIGRAMS is the total for this particular stack. Until
         * WeightComponent itself is migrated to long, widen its int immediately.
         * Reject stacked instances because total-stack weight would be ambiguous.
         */
        if (stack.getCount() != 1 || explicitWeight.milligrams() < 0) {
            return Optional.empty();
        }

        long totalMilligrams = explicitWeight.milligrams();
        return Optional.of(new IngredientMass(
                totalMilligrams,
                VARIABLE_WEIGHT_UNIT_MILLIGRAMS,
                PortionMode.VARIABLE_WEIGHT
        ));
    }

    private static Optional<IngredientMass> resolveUnits(
            long unitMilligrams,
            int count,
            PortionMode portionMode
    ) {
        if (unitMilligrams <= 0 || count <= 0) {
            return Optional.empty();
        }

        try {
            long totalMilligrams = Math.multiplyExact(unitMilligrams, (long) count);
            return Optional.of(new IngredientMass(
                    totalMilligrams,
                    unitMilligrams,
                    portionMode
            ));
        } catch (ArithmeticException overflow) {
            return Optional.empty();
        }
    }
}
