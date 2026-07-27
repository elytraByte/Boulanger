package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.content.ingredient.ScaleTransferService;
import net.boulangermod.boulanger.content.ingredient.mass.IngredientMassResolver;
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

    public boolean measureGrams(long requestedGrams) {
        if (requestedGrams <= 0L) {
            return false;
        }

        final long requestedMilligrams;

        try {
            requestedMilligrams = Math.multiplyExact(
                    requestedGrams,
                    1_000L
            );
        } catch (ArithmeticException overflow) {
            return false;
        }

        return measureMilligrams(requestedMilligrams);
    }

    private boolean measureMilligrams(
            long requestedMilligrams
    ) {
        if (level == null
                || level.isClientSide()
                || requestedMilligrams <= 0L) {
            return false;
        }

        var proposed = ScaleTransferService.plan(
                itemHandler.getStackInSlot(SLOT_SOURCE),
                itemHandler.getStackInSlot(SLOT_BOWL),
                itemHandler.getStackInSlot(SLOT_OUTPUT),
                itemHandler.getStackInSlot(SLOT_RESIDUAL),
                itemHandler.getSlotLimit(SLOT_OUTPUT),
                itemHandler.getSlotLimit(SLOT_RESIDUAL),
                requestedMilligrams
        );

        if (proposed.isEmpty()) {
            return false;
        }

        ScaleTransferService.Result result =
                proposed.get();

        updateInventoryAtomically(() -> {
            itemHandler.setStackInSlot(
                    SLOT_SOURCE,
                    result.sourceAfter()
            );

            itemHandler.setStackInSlot(
                    SLOT_BOWL,
                    result.bowlsAfter()
            );

            itemHandler.setStackInSlot(
                    SLOT_OUTPUT,
                    result.outputAfter()
            );

            itemHandler.setStackInSlot(
                    SLOT_RESIDUAL,
                    result.residualAfter()
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