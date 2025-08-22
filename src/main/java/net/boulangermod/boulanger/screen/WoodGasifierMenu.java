package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.block.entity.WoodGasifierBlockEntity;
import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class WoodGasifierMenu extends AbstractContainerMenu {
    private final WoodGasifierBlockEntity blockEntity;
    private final SimpleContainerData data;
    private final BlockPos pos;

    public WoodGasifierMenu(int windowId,
                            Inventory playerInv,
                            WoodGasifierBlockEntity be,
                            SimpleContainerData data) {
        super(ModMenuTypes.WOOD_GASIFIER_MENU.get(), windowId);
        this.blockEntity = be;
        this.data        = data;
        this.pos         = be.getBlockPos();

        IItemHandler h = be.getItemHandler(null);

        // ─── TE SLOTS ────────────────────────────────────────────────
        this.addSlot(new SlotItemHandler(h, 0, 26, 21) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() == ModItems.SPLIT_PINE_LOGS.get(); }
        });
        this.addSlot(new SlotItemHandler(h, 1, 26, 57) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ItemTags.LOGS); }
        });
        this.addSlot(new SlotItemHandler(h, 2, 56, 21) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() == ModItems.GASIFIER_FILTER.get(); }
        });
        this.addSlot(new SlotItemHandler(h, 3, 56, 57) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() == ModItems.GASIFIER_FILTER.get(); }
        });

        // ─── PLAYER INV + HOTBAR ────────────────────────────────────
        final int yOffset = 6;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18 + yOffset));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142 + yOffset));
        }

        this.addDataSlots(data);
    }

    public WoodGasifierMenu(int windowId, Inventory playerInv, FriendlyByteBuf buf) {
        this(windowId,
                playerInv,
                (WoodGasifierBlockEntity) playerInv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(5)   // ← expanded: burn, energy, maxEnergy, gas, gasCap
        );
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), pos), player, ModBlocks.WOOD_GASIFIER.get());
    }

    // ---- Slot index layout (you add TE first, then player inventory, then hotbar) ----
    private static final int TE_FIRST              = 0;
    private static final int TE_COUNT              = 4;
    private static final int TE_LAST_EXCL          = TE_FIRST + TE_COUNT;             // 0..4

    private static final int PLAYER_INV_FIRST      = TE_LAST_EXCL;                    // 4
    private static final int PLAYER_INV_COUNT      = 27;
    private static final int PLAYER_INV_LAST_EXCL  = PLAYER_INV_FIRST + PLAYER_INV_COUNT; // 4..31

    private static final int HOTBAR_FIRST          = PLAYER_INV_LAST_EXCL;            // 31
    private static final int HOTBAR_COUNT          = 9;
    private static final int HOTBAR_LAST_EXCL      = HOTBAR_FIRST + HOTBAR_COUNT;     // 31..40

    // TE slot aliases
    private static final int TE_SLOT_SPLIT         = TE_FIRST + 0; // split pine logs
    private static final int TE_SLOT_LOG           = TE_FIRST + 1; // any logs
    private static final int TE_SLOT_FILTER_A      = TE_FIRST + 2; // filter
    private static final int TE_SLOT_FILTER_B      = TE_FIRST + 3; // filter

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot source = this.slots.get(index);
        if (source == null || !source.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = source.getItem();
        ItemStack original = stack.copy();
        boolean moved = false;

        // ---- From TE -> Player
        if (index >= TE_FIRST && index < TE_LAST_EXCL) {
            // Prefer main inventory, then hotbar
            moved = moveItemStackTo(stack, PLAYER_INV_FIRST, PLAYER_INV_LAST_EXCL, false)
                    || moveItemStackTo(stack, HOTBAR_FIRST, HOTBAR_LAST_EXCL, false);

            // ---- From Player -> TE (route by item type)
        } else {
            if (stack.getItem() == ModItems.SPLIT_PINE_LOGS.get()) {
                moved = moveItemStackTo(stack, TE_SLOT_SPLIT, TE_SLOT_SPLIT + 1, false);
            } else if (stack.is(ItemTags.LOGS)) {
                moved = moveItemStackTo(stack, TE_SLOT_LOG, TE_SLOT_LOG + 1, false);
            } else if (stack.getItem() == ModItems.GASIFIER_FILTER.get()) {
                // try both filter slots
                moved = moveItemStackTo(stack, TE_SLOT_FILTER_A, TE_SLOT_FILTER_B + 1, false);
            } else {
                moved = false;
            }

            // If it didn't fit the TE, bounce between inv/hotbar like vanilla
            if (!moved) {
                if (index >= PLAYER_INV_FIRST && index < PLAYER_INV_LAST_EXCL) {
                    moved = moveItemStackTo(stack, HOTBAR_FIRST, HOTBAR_LAST_EXCL, false);
                } else if (index >= HOTBAR_FIRST && index < HOTBAR_LAST_EXCL) {
                    moved = moveItemStackTo(stack, PLAYER_INV_FIRST, PLAYER_INV_LAST_EXCL, false);
                }
            }
        }

        if (!moved) return ItemStack.EMPTY;

        if (stack.isEmpty()) source.set(ItemStack.EMPTY);
        else source.setChanged();

        source.onTake(player, stack);
        return original;
    }



    // ─── Data accessors ─────────────────────────────────────────────
    public int getBurnProgress()    { return data.get(0); }
    public int getEnergyStored()    { return data.get(1); }
    public int getMaxEnergyStored() { return data.get(2); }
    public int getGasAmount()       { return data.get(3); } // mB
    public int getGasCapacity()     { return data.get(4); } // mB

    // The total burn time for one log. Matches the BE's BURN_TIME_PER_LOG.
    public int getMaxBurnProgress() {
        return WoodGasifierBlockEntity.getBurnTimePerLog();
    }
}
