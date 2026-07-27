package net.boulangermod.boulanger.content.ingredient.mass;

import net.boulangermod.boulanger.content.ingredient.IngredientIdentity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record PortionResult(
        Status status,
        long transferredMilligrams,
        ItemStack sourceRemainder,
        ItemStack partialRemainder,
        @Nullable IngredientIdentity identity
) {
    public PortionResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(sourceRemainder, "sourceRemainder");
        Objects.requireNonNull(partialRemainder, "partialRemainder");

        sourceRemainder = sourceRemainder.copy();
        partialRemainder = partialRemainder.copy();

        if (transferredMilligrams < 0) {
            throw new IllegalArgumentException(
                    "transferred milligrams must not be negative"
            );
        }
    }

    public boolean succeeded() {
        return status == Status.SUCCESS;
    }

    public static PortionResult success(
            long transferredMilligrams,
            ItemStack sourceRemainder,
            ItemStack partialRemainder,
            IngredientIdentity identity
    ) {
        return new PortionResult(
                Status.SUCCESS,
                transferredMilligrams,
                sourceRemainder,
                partialRemainder,
                identity
        );
    }

    public static PortionResult failure(Status status) {
        if (status == Status.SUCCESS) {
            throw new IllegalArgumentException(
                    "Use success() for successful results"
            );
        }

        return new PortionResult(
                status,
                0L,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                null
        );
    }

    public enum Status {
        SUCCESS,
        EMPTY_SOURCE,
        INVALID_AMOUNT,
        UNSUPPORTED_INGREDIENT,
        AMOUNT_TOO_SMALL,
        INDIVISIBLE_CONTAINER
    }
}