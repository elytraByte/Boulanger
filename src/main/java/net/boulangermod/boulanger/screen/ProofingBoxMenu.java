package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.block.entity.ProofingBoxBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ProofingBoxMenu extends AbstractMachineMenu {

    private final ProofingBoxBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    private static final int TE_FIRST = 0;
    private static final int TE_COUNT = 5;
    private static final int TE_LAST_EXCL = TE_FIRST + TE_COUNT; // 5

    private static final int PLAYER_INV_FIRST = TE_LAST_EXCL;     // 5
    private static final int PLAYER_INV_COUNT = 27;
    private static final int PLAYER_INV_LAST_EXCL = PLAYER_INV_FIRST + PLAYER_INV_COUNT; // 32

    private static final int HOTBAR_FIRST = PLAYER_INV_LAST_EXCL; // 32
    private static final int HOTBAR_COUNT = 9;
    private static final int HOTBAR_LAST_EXCL = HOTBAR_FIRST + HOTBAR_COUNT; // 41


    public ProofingBoxMenu(int id, Inventory playerInv, ProofingBoxBlockEntity blockEntity) {
        super(ModMenuTypes.PROOFING_BOX_MENU.get(), id, playerInv, blockEntity);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        int slot = 0;
// Proofer (hopper) row: 5 slots centered
        for (int i = 0; i < 5; ++i) {
            this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), slot++,
                    44 + i * 18, 20));
        }

// Player inventory 3×9 (top-left 8,51)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18, 51 + row * 18));
            }
        }
// Hotbar (top-left 8,109)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 109));
        }
    }

    public ProofingBoxMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, (ProofingBoxBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, ModBlocks.PROOFING_BOX.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot source = this.slots.get(index);
        if (source == null || !source.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = source.getItem();
        ItemStack original = stack.copy();

        boolean fromTE = index >= TE_FIRST && index < TE_LAST_EXCL;
        boolean fromPlayer = index >= PLAYER_INV_FIRST && index < HOTBAR_LAST_EXCL;

        if (fromPlayer) {
            // Player → TE (try all 9 proofing slots)
            if (!this.moveItemStackTo(stack, TE_FIRST, TE_LAST_EXCL, false)) {
                // swap between inv/hotbar if TE is full
                if (index >= PLAYER_INV_FIRST && index < PLAYER_INV_LAST_EXCL) {
                    if (!this.moveItemStackTo(stack, HOTBAR_FIRST, HOTBAR_LAST_EXCL, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= HOTBAR_FIRST && index < HOTBAR_LAST_EXCL) {
                    if (!this.moveItemStackTo(stack, PLAYER_INV_FIRST, PLAYER_INV_LAST_EXCL, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }
        } else if (fromTE) {
            // TE → Player (main inv first, then hotbar)
            if (!this.moveItemStackTo(stack, PLAYER_INV_FIRST, PLAYER_INV_LAST_EXCL, false)
                    && !this.moveItemStackTo(stack, HOTBAR_FIRST, HOTBAR_LAST_EXCL, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            source.set(ItemStack.EMPTY);
        } else {
            source.setChanged();
        }
        source.onTake(player, stack);

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        return original;
    }
}
