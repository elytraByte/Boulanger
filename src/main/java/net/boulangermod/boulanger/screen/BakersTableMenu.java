package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.block.entity.BakersTableBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class BakersTableMenu extends AbstractContainerMenu {

    private final BakersTableBlockEntity blockEntity;

    public BakersTableMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public BakersTableMenu(int id, Inventory playerInv, BlockEntity entity) {
        super(ModMenuTypes.BAKERS_TABLE_MENU.get(), id);
        this.blockEntity = (BakersTableBlockEntity) entity;

        // Slots: 0 = dough, 1 = pan, 2 = output
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, 44, 30)); // Dough
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 1, 80, 30)); // Pan
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 2, 116, 30)); // Output

        // Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            addSlot(new Slot(playerInv, i, 8 + i * 18, 142));
        }
    }


    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Implement shift-click logic if desired
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getBlockPos().distSqr(player.blockPosition()) <= 64;
    }
    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);

        if (blockEntity.getItemHandler().getStackInSlot(2).isEmpty()) {
            blockEntity.tryShape();
        }
    }

}
