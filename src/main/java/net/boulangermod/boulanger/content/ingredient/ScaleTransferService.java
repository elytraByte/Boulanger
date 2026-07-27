package net.boulangermod.boulanger.content.ingredient;

import net.boulangermod.boulanger.content.ingredient.mass.IngredientPortioner;
import net.boulangermod.boulanger.content.ingredient.mass.PortionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Objects;
import java.util.Optional;

/**
 * Creates a complete proposed scale transaction without mutating any
 * inventory or supplied stack.
 */
public final class ScaleTransferService {
    private ScaleTransferService() {
    }

    public static Optional<Result> plan(
            ItemStack source,
            ItemStack bowls,
            ItemStack existingOutput,
            ItemStack existingResidual,
            int outputSlotLimit,
            int residualSlotLimit,
            long requestedMilligrams
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(bowls, "bowls");
        Objects.requireNonNull(existingOutput, "existingOutput");
        Objects.requireNonNull(existingResidual, "existingResidual");

        if (requestedMilligrams <= 0L
                || source.isEmpty()
                || bowls.isEmpty()
                || !bowls.is(Items.BOWL)) {
            return Optional.empty();
        }

        PortionResult portion = IngredientPortioner.plan(
                source,
                requestedMilligrams
        );

        if (!portion.succeeded()
                || portion.identity() == null
                || portion.transferredMilligrams() <= 0L) {
            return Optional.empty();
        }

        ItemStack filledBowl =
                WeightedContainerFactory.createFilledBowl(
                        portion.identity(),
                        portion.transferredMilligrams()
                );

        if (filledBowl.isEmpty()) {
            return Optional.empty();
        }

        Optional<ItemStack> outputAfter = merge(
                existingOutput,
                filledBowl,
                outputSlotLimit
        );

        if (outputAfter.isEmpty()) {
            return Optional.empty();
        }

        Optional<ItemStack> residualAfter = merge(
                existingResidual,
                portion.partialRemainder(),
                residualSlotLimit
        );

        if (residualAfter.isEmpty()) {
            return Optional.empty();
        }

        ItemStack bowlsAfter = bowls.copy();
        bowlsAfter.shrink(1);

        if (bowlsAfter.isEmpty()) {
            bowlsAfter = ItemStack.EMPTY;
        }

        return Optional.of(
                new Result(
                        portion.sourceRemainder(),
                        bowlsAfter,
                        outputAfter.get(),
                        residualAfter.get()
                )
        );
    }

    private static Optional<ItemStack> merge(
            ItemStack existing,
            ItemStack addition,
            int slotLimit
    ) {
        if (addition.isEmpty()) {
            return Optional.of(existing.copy());
        }

        int limit = Math.min(
                slotLimit,
                addition.getMaxStackSize()
        );

        if (existing.isEmpty()) {
            if (addition.getCount() > limit) {
                return Optional.empty();
            }

            return Optional.of(addition.copy());
        }

        if (!ItemStack.isSameItemSameComponents(
                existing,
                addition
        )) {
            return Optional.empty();
        }

        int combinedCount =
                existing.getCount() + addition.getCount();

        limit = Math.min(
                limit,
                existing.getMaxStackSize()
        );

        if (combinedCount > limit) {
            return Optional.empty();
        }

        ItemStack combined = existing.copy();
        combined.setCount(combinedCount);

        return Optional.of(combined);
    }

    public record Result(
            ItemStack sourceAfter,
            ItemStack bowlsAfter,
            ItemStack outputAfter,
            ItemStack residualAfter
    ) {
        public Result {
            Objects.requireNonNull(sourceAfter, "sourceAfter");
            Objects.requireNonNull(bowlsAfter, "bowlsAfter");
            Objects.requireNonNull(outputAfter, "outputAfter");
            Objects.requireNonNull(residualAfter, "residualAfter");

            sourceAfter = sourceAfter.copy();
            bowlsAfter = bowlsAfter.copy();
            outputAfter = outputAfter.copy();
            residualAfter = residualAfter.copy();
        }
    }
}