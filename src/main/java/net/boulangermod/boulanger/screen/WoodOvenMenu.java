package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.block.entity.WoodOvenBlockEntity;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.ProofingStateComponent;
import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.Objects;

/**
 * Container for the Wood Oven.
 *
 * Slots:
 *   0: INPUT        (loose shaped dough OR pan containing shaped doughs)
 *   1: FUEL         (split pine logs only)
 *   2: OUTPUT       (baked bread)          [no insert]
 *   3: PAN_RETURN   (empty pan comes back) [no insert]
 *
 * Data slots (ContainerData):
 *   [0] burnTime
 *   [1] maxBurnTime
 *   [2] cookTime
 *   [3] MAX_COOK_TIME (constant)
 */
public class WoodOvenMenu extends AbstractContainerMenu {
    // ---- indices provided by the BE ----
    public static final int SLOT_INPUT      = WoodOvenBlockEntity.SLOT_INPUT;
    public static final int SLOT_FUEL       = WoodOvenBlockEntity.SLOT_FUEL;
    public static final int SLOT_OUTPUT     = WoodOvenBlockEntity.SLOT_OUTPUT;
    public static final int SLOT_PAN_RETURN = WoodOvenBlockEntity.SLOT_PAN_RETURN;

    // vanilla player inventory/hotbar sizes
    private static final int PLAYER_INV_ROWS = 3;
    private static final int PLAYER_INV_COLS = 9;
    private static final int PLAYER_INV_SIZE = PLAYER_INV_ROWS * PLAYER_INV_COLS; // 27
    private static final int HOTBAR_SIZE     = 9;

    // container index ranges (inclusive start, exclusive end)
    private static final int BE_FIRST_SLOT        = 0;
    private static final int BE_SLOT_COUNT        = 4;
    private static final int INV_FIRST_SLOT       = BE_FIRST_SLOT + BE_SLOT_COUNT;              // 4
    private static final int INV_LAST_SLOT_EXCL   = INV_FIRST_SLOT + PLAYER_INV_SIZE;           // 31
    private static final int HOTBAR_FIRST_SLOT    = INV_LAST_SLOT_EXCL;                         // 31
    private static final int HOTBAR_LAST_SLOT_EXCL= HOTBAR_FIRST_SLOT + HOTBAR_SIZE;            // 40

    private final WoodOvenBlockEntity be;
    private final ContainerLevelAccess access;
    private final ContainerData data; // [0]=burn, [1]=burnMax, [2]=cook, [3]=cookTotal

    // --- Client ctor (receives BlockPos over the wire) ---
    public WoodOvenMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv.player.level(), buf.readBlockPos()),
                new SimpleContainerData(4));
    }

    // --- Server ctor (BE passes its live ContainerData) ---
    public WoodOvenMenu(int id, Inventory playerInv, WoodOvenBlockEntity be, ContainerData data) {
        super(ModMenuTypes.WOOD_OVEN_MENU.get(), id);
        this.be = be;
        this.data = data;
        this.access = ContainerLevelAccess.create(Objects.requireNonNull(be.getLevel()), be.getBlockPos());

        IItemHandler handler = be.getItemHandler(null);

        // ---- BE slots ----
        this.addSlot(new InputSlot(handler, SLOT_INPUT, 56, 17));     // dough / panned dough
        this.addSlot(new FuelSlot(handler,  SLOT_FUEL,  56, 53));     // split pine logs
        this.addSlot(new OutputSlot(handler, SLOT_OUTPUT, 116, 35));  // baked bread (no insert)
        this.addSlot(new PanReturnSlot(handler, SLOT_PAN_RETURN, 134, 53)); // empty pan (no insert)

        // ---- Player inventory (3×9) ----
        int invY = 84, invX = 8;
        for (int row = 0; row < PLAYER_INV_ROWS; row++) {
            for (int col = 0; col < PLAYER_INV_COLS; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }

        // ---- Hotbar (9) ----
        int hotbarY = invY + 58;
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            this.addSlot(new Slot(playerInv, i, invX + i * 18, hotbarY));
        }

        // ---- live data sync ----
        this.addDataSlots(data);
    }

    private static WoodOvenBlockEntity getBlockEntity(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof WoodOvenBlockEntity oven)) {
            throw new IllegalStateException("WoodOvenMenu: expected WoodOvenBlockEntity at " + pos);
        }
        return oven;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.WOOD_OVEN.get());
    }

    // ---------------- Screen helpers ----------------

    /** true while any burn time remains */
    public boolean isLit() {
        return data.get(0) > 0;
    }

    /** show the arrow when cooking is in progress or there is a valid input present */
    public boolean isCrafting() {
        // Show as crafting when timer is moving or when a valid input is present
        return data.get(2) > 0 || beHasPotentialRecipe();
    }

    /** width in pixels (0..px) */
    public int getCookingProgressScaled(int px) {
        final int cook  = data.get(2);
        final int total = Math.max(1, data.get(3));
        return Mth.clamp(Math.round((cook / (float) total) * px), 0, px);
    }

    /** height in pixels (0..px) for the flame fill */
    public int getLitProgressScaled(int px) {
        final int remaining = data.get(0);
        final int total     = Math.max(1, data.get(1));
        return Mth.clamp(Math.round((remaining / (float) total) * px), 0, px);
    }

    private boolean beHasPotentialRecipe() {
        ItemStack in = be.getItemHandler(null).getStackInSlot(SLOT_INPUT);
        return isValidInputItem(in);
    }

    // ---------------- Shift-click logic ----------------

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return empty;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        // From BE slots -> player inventory/hotbar
        if (index < INV_FIRST_SLOT) {
            if (index == SLOT_OUTPUT || index == SLOT_PAN_RETURN) {
                if (!this.moveItemStackTo(stack, INV_FIRST_SLOT, HOTBAR_LAST_SLOT_EXCL, true))
                    return ItemStack.EMPTY;
                slot.onQuickCraft(stack, original);
            } else {
                if (!this.moveItemStackTo(stack, INV_FIRST_SLOT, HOTBAR_LAST_SLOT_EXCL, false))
                    return ItemStack.EMPTY;
            }
        } else {
            // From player inventory/hotbar -> BE
            if (isFuel(stack)) {
                if (!this.moveItemStackTo(stack, SLOT_FUEL, SLOT_FUEL + 1, false))
                    return ItemStack.EMPTY;
            } else if (isValidInputItem(stack)) {
                if (!this.moveItemStackTo(stack, SLOT_INPUT, SLOT_INPUT + 1, false))
                    return ItemStack.EMPTY;
            } else {
                // shuffle between inv <-> hotbar
                if (index < HOTBAR_FIRST_SLOT) {
                    if (!this.moveItemStackTo(stack, HOTBAR_FIRST_SLOT, HOTBAR_LAST_SLOT_EXCL, false))
                        return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(stack, INV_FIRST_SLOT, INV_LAST_SLOT_EXCL, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return original;
    }

    // ---------------- Slot filters ----------------

    /** Accept loose shaped dough, or a pan that contains at least one shaped dough. */
    private static boolean isValidInputItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        var DS = ModDataComponentTypes.PROOFING_STATE.get();
        var PT = ModDataComponentTypes.DOUGH_PROCESS_TYPE.get();

        // Loose dough: must be shaped
        if (stack.has(DS) && stack.has(PT)) {
            ProofingStateComponent proof = stack.get(DS);
            return proof != null && proof.shaped();
        }

        // Pan with cavity doughs
        if (stack.is(ModItems.PAN.get())) {
            ItemContainerContents c = stack.get(DataComponents.CONTAINER);
            if (c == null || c.getSlots() == 0) return false;
            for (int i = 0; i < c.getSlots(); i++) {
                ItemStack it = c.getStackInSlot(i);
                if (!it.isEmpty() && it.has(DS) && it.has(PT)) {
                    ProofingStateComponent proof = it.get(DS);
                    if (proof != null && proof.shaped()) return true;
                }
            }
        }
        return false;
    }

    private static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.SPLIT_PINE_LOGS.get());
    }

    // ---------------- Custom Slot types ----------------

    private static class InputSlot extends SlotItemHandler {
        public InputSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }
        @Override public boolean mayPlace(ItemStack stack) { return isValidInputItem(stack); }
    }

    private static class FuelSlot extends SlotItemHandler {
        public FuelSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }
        @Override public boolean mayPlace(ItemStack stack) { return isFuel(stack); }
    }

    private static class OutputSlot extends SlotItemHandler {
        public OutputSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }

    private static class PanReturnSlot extends SlotItemHandler {
        public PanReturnSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }

    // -------------- Public helpers for screens/tests --------------

    public boolean isClientLitCached() { return isLit(); }
    public int getBurnTime()    { return data.get(0); }
    public int getMaxBurnTime() { return data.get(1); }
    public int getCookTime()    { return data.get(2); }
    public int getCookTotal()   { return data.get(3); }
}
