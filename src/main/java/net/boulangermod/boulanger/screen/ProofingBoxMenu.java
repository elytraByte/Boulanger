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

    public ProofingBoxMenu(int id, Inventory playerInv, ProofingBoxBlockEntity blockEntity) {
        super(ModMenuTypes.PROOFING_BOX_MENU.get(), id, playerInv, blockEntity);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        // --- Add TE slots: 27 input (top), 27 output (middle)
        int slot = 0;
        // Input: 3 rows × 9 cols
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), slot++, 8 + col * 18, 18 + row * 18));
            }
        }
        // Output: 3 rows × 9 cols (shifted down)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int index = 27 + col + row * 9;
                this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), index, 8 + col * 18, 88 + row * 18));
            }
        }

        // --- Add player inventory (3 rows)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 154 + row * 18));

            }
        }

        // --- Hotbar (1 row)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 154 + 58));

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
        final int TE_SLOT_COUNT = blockEntity.getItemHandler().getSlots();
        final int PLAYER_SLOT_COUNT = 36; // 27 main + 9 hotbar
        final int TE_FIRST_SLOT_INDEX = PLAYER_SLOT_COUNT;
        final int TE_LAST_SLOT_INDEX = TE_FIRST_SLOT_INDEX + TE_SLOT_COUNT;

        Slot sourceSlot = this.slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < PLAYER_SLOT_COUNT) {
            // From player inventory to TE
            if (!moveItemStackTo(sourceStack, TE_FIRST_SLOT_INDEX, TE_LAST_SLOT_INDEX, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TE_LAST_SLOT_INDEX) {
            // From TE to player inventory
            if (!moveItemStackTo(sourceStack, 0, PLAYER_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.err.println("Invalid slotIndex: " + index);
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        return copy;
    }
}
