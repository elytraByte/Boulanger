package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.block.SugarRefineryBlock;
import net.boulangermod.boulanger.block.entity.SugarRefineryBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class SugarRefineryMenu extends AbstractContainerMenu {
    // ── Slot indices (must match BE order) ───────────────────────────────────
    private static final int FUEL_SLOT        = 0;
    private static final int SUGARCANE_SLOT   = 1;
    private static final int SUGAR_SLOT       = 2;
    private static final int MOLASSES_SLOT    = 3;
    private static final int BROWN_SUGAR_SLOT = 4;

    // ── Player/TE index math (player first, then TE) ─────────────────────────
    private static final int HOTBAR = 9, ROWS = 3, COLS = 9;
    private static final int PLAYER_INV = ROWS * COLS;
    private static final int VANILLA = HOTBAR + PLAYER_INV;      // 36
    private static final int VANILLA_FIRST = 0;
    private static final int TE_FIRST = VANILLA_FIRST + VANILLA; // 36
    private static final int TE_COUNT = 5;

    // ── State ────────────────────────────────────────────────────────────────
    @Nullable
    private final SugarRefineryBlockEntity blockEntity;
    private final IItemHandler itemHandlerForMenu;

    // Progress (synced to client via DataSlots)
    private int burnTime, burnTimeTotal, cookTime, cookTimeTotal;

    // ── Client ctor (reads BlockPos if provided) ─────────────────────────────
    public SugarRefineryMenu(int id, Inventory playerInv, @Nullable FriendlyByteBuf buf) {
        this(id, playerInv, buf != null ? playerInv.player.level().getBlockEntity(buf.readBlockPos()) : null);
    }

    // ── Server ctor (BE calls this) ──────────────────────────────────────────
    public SugarRefineryMenu(int id, Inventory playerInv, @Nullable BlockEntity be) {
        super(ModMenuTypes.SUGAR_REFINERY_MENU.get(), id);
        this.blockEntity = (be instanceof SugarRefineryBlockEntity s) ? s : null;
        this.itemHandlerForMenu = (this.blockEntity != null)
                ? this.blockEntity.getInventory()
                : new ItemStackHandler(TE_COUNT); // safe dummy if opened without pos

        // 1) Player inventory FIRST (so indices 0..35 are vanilla)
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));

        // 2) Then the machine’s 5 slots (indices 36..40)
        addSlot(new SlotItemHandler(itemHandlerForMenu, FUEL_SLOT,        26, 32));  // leftmost
        addSlot(new SlotItemHandler(itemHandlerForMenu, SUGARCANE_SLOT,   79, 17));  // top-middle
        addSlot(new SlotItemHandler(itemHandlerForMenu, SUGAR_SLOT,       56, 51));  // left-center
        addSlot(new SlotItemHandler(itemHandlerForMenu, MOLASSES_SLOT,    79, 58));  // center
        addSlot(new SlotItemHandler(itemHandlerForMenu, BROWN_SUGAR_SLOT, 102,51));  // right-center

        // 3) Progress sync (server uses get(); client receives via set() and fills local fields)
        if (this.blockEntity != null) {
            addDataSlot(new DataSlot(){ public int get(){ return blockEntity.getBurnTime(); }      public void set(int v){ burnTime = v; }});
            addDataSlot(new DataSlot(){ public int get(){ return blockEntity.getBurnTimeTotal(); } public void set(int v){ burnTimeTotal = v; }});
            addDataSlot(new DataSlot(){ public int get(){ return blockEntity.getCookTime(); }      public void set(int v){ cookTime = v; }});
            addDataSlot(new DataSlot(){ public int get(){ return blockEntity.getCookTimeTotal(); } public void set(int v){ cookTimeTotal = v; }});
        }
    }

    // ── Used by the Screen for overlays ──────────────────────────────────────
    public int getBurnProgressScaled(int pixels) {
        // burnTime: remaining; burnTimeTotal: total for current fuel
        if (burnTimeTotal <= 0) return 0;
        int scaled = burnTime * pixels / burnTimeTotal;
        if (scaled < 0) scaled = 0;
        if (scaled > pixels) scaled = pixels;
        return scaled;
    }

    public int getCookProgressScaled(int pixels) {
        // cookTime: elapsed; cookTimeTotal: needed
        if (cookTimeTotal <= 0) return 0;
        int scaled = cookTime * pixels / cookTimeTotal;
        if (scaled < 0) scaled = 0;
        if (scaled > pixels) scaled = pixels;
        return scaled;
    }

    // ── Basics ───────────────────────────────────────────────────────────────
    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return true; // tolerate if opened without pos; UI will be inert
        return blockEntity.getLevel()
                .getBlockState(blockEntity.getBlockPos())
                .getBlock() instanceof SugarRefineryBlock;
    }

    // ── Shift-click logic (player ↔ TE) ──────────────────────────────────────
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot srcSlot = this.slots.get(index);
        if (srcSlot == null || !srcSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack src = srcSlot.getItem();
        ItemStack copy = src.copy();

        if (index < VANILLA_FIRST + VANILLA) {
            // player → TE (prefer fuel then cane)
            if (!moveItemStackTo(src, TE_FIRST + FUEL_SLOT, TE_FIRST + FUEL_SLOT + 1, false) &&
                    !moveItemStackTo(src, TE_FIRST + SUGARCANE_SLOT, TE_FIRST + SUGARCANE_SLOT + 1, false))
                return ItemStack.EMPTY;
        } else if (index < TE_FIRST + TE_COUNT) {
            // TE → player
            if (!moveItemStackTo(src, VANILLA_FIRST, VANILLA_FIRST + VANILLA, false))
                return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (src.isEmpty()) srcSlot.set(ItemStack.EMPTY);
        else srcSlot.setChanged();
        srcSlot.onTake(player, src);
        return copy;
    }

    @Nullable
    public SugarRefineryBlockEntity getBlockEntity() { return blockEntity; }
}
