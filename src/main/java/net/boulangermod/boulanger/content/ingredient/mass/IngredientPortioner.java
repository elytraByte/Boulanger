package net.boulangermod.boulanger.content.ingredient.mass;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.value.WeightComponent;
import net.boulangermod.boulanger.content.ingredient.IngredientIdentity;
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

        ItemStack sourceRemainder = ItemStack.EMPTY;

        if (remaining > 0) {
            sourceRemainder = source.copy();
            sourceRemainder.setCount(1);
            sourceRemainder.set(
                    ModDataComponentTypes.INGREDIENT_MILLIGRAMS.get(),
                    new WeightComponent(remaining)
            );
            identity.applyTo(sourceRemainder);
        }

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
        if (fullUnits > 0) {
            sourceRemainder = source.copy();
            sourceRemainder.setCount(Math.toIntExact(fullUnits));
        }

        ItemStack partialRemainder = ItemStack.EMPTY;
        if (partialMilligrams > 0) {
            partialRemainder = source.copy();
            partialRemainder.setCount(1);

            partialRemainder.set(
                    ModDataComponentTypes.INGREDIENT_MILLIGRAMS.get(),
                    new WeightComponent(partialMilligrams)
            );

            identity.applyTo(partialRemainder);
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
        long unitMilligrams = mass.unitMilligrams();
        long candidate = Math.min(
                requestedMilligrams,
                mass.totalMilligrams()
        );

        if (candidate < unitMilligrams) {
            return PortionResult.failure(
                    PortionResult.Status.AMOUNT_TOO_SMALL
            );
        }

        /*
         * If more source remains, the requested portion must be a whole
         * number of containers. A request larger than all available mass
         * may consume the complete source.
         */
        if (candidate < mass.totalMilligrams()
                && candidate % unitMilligrams != 0) {
            return PortionResult.failure(
                    PortionResult.Status.INDIVISIBLE_CONTAINER
            );
        }

        long transferredUnits =
                candidate / unitMilligrams;

        long transferredMilligrams = Math.multiplyExact(
                transferredUnits,
                unitMilligrams
        );

        long remainingUnits =
                (long) source.getCount() - transferredUnits;

        ItemStack sourceRemainder = ItemStack.EMPTY;
        if (remainingUnits > 0) {
            sourceRemainder = source.copy();
            sourceRemainder.setCount(
                    Math.toIntExact(remainingUnits)
            );
        }

        ItemStack emptyContainers = createEmptyContainers(
                source.getItem(),
                transferredUnits
        );

        if (emptyContainers == null) {
            return PortionResult.failure(
                    PortionResult.Status.INDIVISIBLE_CONTAINER
            );
        }

        return PortionResult.success(
                transferredMilligrams,
                sourceRemainder,
                emptyContainers,
                identity
        );
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