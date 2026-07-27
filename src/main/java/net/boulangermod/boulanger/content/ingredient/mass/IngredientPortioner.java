package net.boulangermod.boulanger.content.ingredient.mass;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.value.WeightComponent;
import net.boulangermod.boulanger.content.ingredient.IngredientIdentity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class IngredientPortioner {
    private IngredientPortioner() {}

    public static PortionResult plan(
            ItemStack source,
            long requestedMilligrams
    ) {
        if (source == null || source.isEmpty()) {
            return PortionResult.failure(
                    PortionResult.Status.EMPTY_SOURCE
            );
        }

        if (requestedMilligrams <= 0) {
            return PortionResult.failure(
                    PortionResult.Status.INVALID_AMOUNT
            );
        }

        var resolved = IngredientMassResolver.resolve(source);
        if (resolved.isEmpty()) {
            return PortionResult.failure(
                    PortionResult.Status.UNSUPPORTED_INGREDIENT
            );
        }

        IngredientMass mass = resolved.get();

        if (mass.isEmpty()) {
            return PortionResult.failure(
                    PortionResult.Status.EMPTY_SOURCE
            );
        }

        IngredientIdentity identity =
                IngredientIdentity.from(source);

        return switch (mass.portionMode()) {
            case VARIABLE_WEIGHT -> planVariable(
                    source,
                    requestedMilligrams,
                    mass,
                    identity
            );

            case DISCRETE_UNITS -> planDiscrete(
                    source,
                    requestedMilligrams,
                    mass,
                    identity
            );

            case ALL_OR_NOTHING -> planAllOrNothing(
                    source,
                    requestedMilligrams,
                    mass,
                    identity
            );
        };
    }

    private static PortionResult planVariable(
            ItemStack source,
            long requestedMilligrams,
            IngredientMass mass,
            IngredientIdentity identity
    ) {
        long transferred = Math.min(
                requestedMilligrams,
                mass.totalMilligrams()
        );

        long remaining =
                mass.totalMilligrams() - transferred;

        ItemStack sourceRemainder =
                createVariableWeightRemainder(
                        source,
                        remaining,
                        identity
                );

        return PortionResult.success(
                transferred,
                sourceRemainder,
                ItemStack.EMPTY,
                identity
        );
    }

    private static PortionResult planDiscrete(
            ItemStack source,
            long requestedMilligrams,
            IngredientMass mass,
            IngredientIdentity identity
    ) {
        long transferred = Math.min(
                requestedMilligrams,
                mass.totalMilligrams()
        );

        long remaining =
                mass.totalMilligrams() - transferred;

        long fullUnits =
                remaining / mass.unitMilligrams();

        long partialMilligrams =
                remaining % mass.unitMilligrams();

        ItemStack sourceRemainder = ItemStack.EMPTY;
        ItemStack partialRemainder = ItemStack.EMPTY;

        if (fullUnits > 0L) {
            sourceRemainder = source.copy();
            sourceRemainder.setCount(
                    Math.toIntExact(fullUnits)
            );
        }

        if (partialMilligrams > 0L) {
            ItemStack weighedRemainder =
                    createVariableWeightRemainder(
                            source,
                            partialMilligrams,
                            identity
                    );

            if (fullUnits == 0L) {
                /*
                 * A single divided unit can remain in the source
                 * slot and be weighed again immediately.
                 */
                sourceRemainder = weighedRemainder;
            } else {
                /*
                 * Whole and partial units have different components
                 * and cannot occupy the same slot.
                 */
                partialRemainder = weighedRemainder;
            }
        }

        return PortionResult.success(
                transferred,
                sourceRemainder,
                partialRemainder,
                identity
        );
    }

    private static PortionResult planAllOrNothing(
            ItemStack source,
            long requestedMilligrams,
            IngredientMass mass,
            IngredientIdentity identity
    ) {
        long unitMilligrams =
                mass.unitMilligrams();

        long transferableMilligrams = Math.min(
                requestedMilligrams,
                mass.totalMilligrams()
        );

        /*
         * Even one complete container cannot be transferred.
         */
        if (transferableMilligrams < unitMilligrams) {
            return PortionResult.failure(
                    PortionResult.Status.AMOUNT_TOO_SMALL
            );
        }

        /*
         * If some source would remain, the requested amount must
         * represent an exact number of complete containers.
         *
         * A request greater than the available mass may consume
         * the entire source because its total is already composed
         * of complete containers.
         */
        if (transferableMilligrams
                < mass.totalMilligrams()
                && transferableMilligrams
                % unitMilligrams != 0L) {
            return PortionResult.failure(
                    PortionResult.Status.INDIVISIBLE_CONTAINER
            );
        }

        long transferredUnits =
                transferableMilligrams
                        / unitMilligrams;

        long transferredMilligrams =
                Math.multiplyExact(
                        transferredUnits,
                        unitMilligrams
                );

        long remainingUnits =
                (long) source.getCount()
                        - transferredUnits;

        ItemStack sourceRemainder =
                ItemStack.EMPTY;

        if (remainingUnits > 0L) {
            sourceRemainder = source.copy();
            sourceRemainder.setCount(
                    Math.toIntExact(remainingUnits)
            );
        }

        ItemStack emptyContainers =
                createEmptyContainers(
                        source.getItem(),
                        transferredUnits
                );

        /*
         * createEmptyContainers returns null only when the empty
         * containers cannot fit in the single residual stack.
         */
        if (emptyContainers == null) {
            return PortionResult.failure(
                    PortionResult.Status
                            .INDIVISIBLE_CONTAINER
            );
        }

        return PortionResult.success(
                transferredMilligrams,
                sourceRemainder,
                emptyContainers,
                identity
        );
    }

    private static ItemStack createVariableWeightRemainder(
            ItemStack source,
            long milligrams,
            IngredientIdentity identity
    ) {
        if (milligrams <= 0L) {
            return ItemStack.EMPTY;
        }

        ItemStack remainder = source.copy();
        remainder.setCount(1);

        /*
         * Prevent two identically weighted partial ingredients from
         * merging. INGREDIENT_MILLIGRAMS is the total represented by
         * this one stack.
         */
        remainder.set(
                DataComponents.MAX_STACK_SIZE,
                1
        );

        remainder.set(
                ModDataComponentTypes
                        .INGREDIENT_MILLIGRAMS
                        .get(),
                WeightComponent.ofMilligrams(milligrams)
        );

        identity.applyTo(remainder);

        return remainder;
    }

    /*
     * null means the registered remainder cannot fit in the one
     * residual stack represented by PortionResult.
     */
    private static ItemStack createEmptyContainers(
            Item sourceItem,
            long count
    ) {
        Item emptyContainer = IngredientMassRegistry
                .find(sourceItem)
                .map(IngredientMassRegistry.Entry::emptyContainer)
                .orElse(null);

        if (emptyContainer == null) {
            return ItemStack.EMPTY;
        }

        int containerCount = Math.toIntExact(count);
        ItemStack result = new ItemStack(emptyContainer);

        if (containerCount > result.getMaxStackSize()) {
            return null;
        }

        result.setCount(containerCount);
        return result;
    }
}