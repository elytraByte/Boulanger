package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.content.ingredient.WeightedContainerFactory;
import net.boulangermod.boulanger.content.ingredient.mass.IngredientMassResolver;
import net.boulangermod.boulanger.content.ingredient.mass.IngredientPortioner;
import net.boulangermod.boulanger.content.ingredient.mass.PortionResult;
import net.boulangermod.boulanger.screen.ScaleBlockMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class ScaleBlockEntity
        extends AbstractProcessingBlockEntity {

    public static final int SLOT_SOURCE = 0;
    public static final int SLOT_BOWL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_RESIDUAL = 3;

    public ScaleBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.SCALE_BLOCK.get(),
                pos,
                state,
                4
        );
    }

    public boolean measure(long requestedMilligrams) {
        if (level == null
                || level.isClientSide()
                || requestedMilligrams <= 0) {
            return false;
        }

        ItemStack source =
                itemHandler.getStackInSlot(SLOT_SOURCE);

        ItemStack bowls =
                itemHandler.getStackInSlot(SLOT_BOWL);

        if (source.isEmpty()
                || bowls.isEmpty()
                || !bowls.is(Items.BOWL)) {
            return false;
        }

        PortionResult plan = IngredientPortioner.plan(
                source,
                requestedMilligrams
        );

        if (!plan.succeeded() || plan.identity() == null) {
            return false;
        }

        ItemStack filledBowl =
                WeightedContainerFactory.createFilledBowl(
                        plan.identity(),
                        plan.transferredMilligrams()
                );

        Optional<ItemStack> proposedOutput = merge(
                itemHandler.getStackInSlot(SLOT_OUTPUT),
                filledBowl,
                itemHandler.getSlotLimit(SLOT_OUTPUT)
        );

        if (proposedOutput.isEmpty()) {
            return false;
        }

        Optional<ItemStack> proposedResidual = merge(
                itemHandler.getStackInSlot(SLOT_RESIDUAL),
                plan.partialRemainder(),
                itemHandler.getSlotLimit(SLOT_RESIDUAL)
        );

        if (proposedResidual.isEmpty()) {
            return false;
        }

        ItemStack bowlsAfter = bowls.copy();
        bowlsAfter.shrink(1);

        /*
         * Every possible failure has now been checked. Only now do
         * we mutate the inventory.
         */
        updateInventoryAtomically(() -> {
            itemHandler.setStackInSlot(
                    SLOT_SOURCE,
                    plan.sourceRemainder().copy()
            );

            itemHandler.setStackInSlot(
                    SLOT_BOWL,
                    bowlsAfter
            );

            itemHandler.setStackInSlot(
                    SLOT_OUTPUT,
                    proposedOutput.get()
            );

            itemHandler.setStackInSlot(
                    SLOT_RESIDUAL,
                    proposedResidual.get()
            );
        });

        return true;
    }

    @Override
    protected boolean isItemValid(
            int slot,
            ItemStack stack
    ) {
        return switch (slot) {
            case SLOT_SOURCE ->
                    IngredientMassResolver.resolve(stack).isPresent();

            case SLOT_BOWL ->
                    stack.is(Items.BOWL);

            case SLOT_OUTPUT, SLOT_RESIDUAL ->
                    false;

            default -> false;
        };
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

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "block.boulanger.scale"
        );
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory inventory,
            Player player
    ) {
        return new ScaleBlockMenu(
                containerId,
                inventory,
                this
        );
    }
}