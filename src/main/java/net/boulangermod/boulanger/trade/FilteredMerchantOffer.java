package net.boulangermod.boulanger.trade;

import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Objects;
import java.util.function.Predicate;

/** Merchant offer whose acceptance is gated by a custom predicate (tolerant match). */
public final class FilteredMerchantOffer extends MerchantOffer {
    private final Predicate<ItemStack> predicate;
    private final int requiredCount;

    public FilteredMerchantOffer(ItemStack previewCostA,
                                 ItemStack result,
                                 int maxUses,
                                 int villagerXp,
                                 float priceMult,
                                 Predicate<ItemStack> predicate) {
        // Use a normal ItemCost so the trade UI displays the exact preview stack + components.
        super(new ItemCost(
                        previewCostA.getItem().builtInRegistryHolder(),
                        previewCostA.getCount(),
                        DataComponentPredicate.EMPTY,
                        previewCostA.copy()
                ),
                result, maxUses, villagerXp, priceMult);
        this.predicate = Objects.requireNonNull(predicate);
        this.requiredCount = Math.max(1, previewCostA.getCount());
    }

    @Override
    public boolean satisfiedBy(ItemStack stackA, ItemStack stackB) {
        // No second cost for these offers; accept A if it passes the tolerant predicate.
        return stackB.isEmpty()
                && stackA.getCount() >= requiredCount
                && predicate.test(stackA);
    }
}
