package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.util.ScaleLogic;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.List;

public class MilligramScaleMenu extends AbstractContainerMenu {
    private static final int SCALE_SLOT_COUNT = 4;

    // ==== quick-move constants (copy/paste from ScaleBlockMenu) ====
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_ROW_COUNT * PLAYER_INVENTORY_COLUMN_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT = SCALE_SLOT_COUNT;
    // =============================================================

    private final ItemStack scaleStack;
    private final ItemStackHandler handler;
    private int targetMg = 0;

    public MilligramScaleMenu(int id, Inventory inv, ItemStack stack) {
        super(ModMenuTypes.MILLIGRAM_SCALE_MENU.get(), id);
        this.scaleStack = stack;

        // hydrate handler from the stack’s saved container
        ItemContainerContents cont = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        this.handler = new ItemStackHandler(SCALE_SLOT_COUNT);
        for (int i = 0; i < SCALE_SLOT_COUNT; i++) {
            if (i < cont.getSlots()) {
                handler.setStackInSlot(i, cont.getStackInSlot(i));
            }
        }

        // ==== four scale slots, same coords as ScaleBlockMenu ====
        addSlot(new SlotItemHandler(handler, 0, 26, 17)); // bulk
        addSlot(new SlotItemHandler(handler, 1, 62, 17)); // bowl-in
        addSlot(new SlotItemHandler(handler, 2, 62, 53)); // bowl-out
        addSlot(new SlotItemHandler(handler, 3, 26, 53)); // residual

        // ==== player inventory (3×9) ====
        for (int row = 0; row < PLAYER_INVENTORY_ROW_COUNT; row++) {
            for (int col = 0; col < PLAYER_INVENTORY_COLUMN_COUNT; col++) {
                addSlot(new Slot(inv,
                        col + row * PLAYER_INVENTORY_COLUMN_COUNT + 9,
                        8 + col * 18,
                        84 + row * 18
                ));
            }
        }

        // ==== hotbar ====
        for (int col = 0; col < HOTBAR_SLOT_COUNT; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    /** Called by the packet handler when “Measure” is clicked */
    public void onMeasureClick(int mg) {
        ScaleLogic.TransferResult r =
                ScaleLogic.transfer(handler.getStackInSlot(0), mg);
        handler.setStackInSlot(2, ScaleLogic.createFilledBowl(handler.getStackInSlot(0), r.transferredMg));
        handler.setStackInSlot(0, r.newBulkStack);
        handler.setStackInSlot(3, r.residualStack);

        broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        List<ItemStack> items = List.of(
                handler.getStackInSlot(0),
                handler.getStackInSlot(1),
                handler.getStackInSlot(2),
                handler.getStackInSlot(3)
        );
        scaleStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** quick-move = shift-click handling (copied from ScaleBlockMenu) */
    @Override
    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSource = sourceStack.copy();

        // clicked in player inventory/hotbar?
        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            if (!moveItemStackTo(
                    sourceStack,
                    TE_INVENTORY_FIRST_SLOT_INDEX,
                    TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        }
        // clicked in TE (scale) slots?
        else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            if (!moveItemStackTo(
                    sourceStack,
                    VANILLA_FIRST_SLOT_INDEX,
                    VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        }
        else {
            System.out.println("Invalid slotIndex:" + pIndex);
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSource;
    }

    public int getContainerId() {
        return this.containerId;
    }
}

