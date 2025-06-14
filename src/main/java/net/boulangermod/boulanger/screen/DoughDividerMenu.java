package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.block.DoughDividerBlock;
import net.boulangermod.boulanger.block.entity.DoughDividerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class DoughDividerMenu extends AbstractContainerMenu {
    private final DoughDividerBlockEntity blockEntity;
    private static final int INPUT_SLOT_X  = 60;
    private static final int INPUT_SLOT_Y  = 55;
    private static final int OUTPUT_SLOT_X = INPUT_SLOT_X + 72; // keep the same +72px spacing
    private static final int OUTPUT_SLOT_Y = INPUT_SLOT_Y;

    public DoughDividerMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id,
                playerInventory,
                playerInventory.player.level().getBlockEntity(extraData.readBlockPos())
        );
    }

    public DoughDividerMenu(int id, Inventory playerInv, BlockEntity be) {
        super(ModMenuTypes.DOUGH_DIVIDER_MENU.get(), id);
        this.blockEntity = (DoughDividerBlockEntity) be;

        // Tile-entity slots: 0 = input, 1 = output
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(),
                0,
                INPUT_SLOT_X,
                INPUT_SLOT_Y
        ));
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(),
                1,
                OUTPUT_SLOT_X,
                OUTPUT_SLOT_Y
        ));

        // … then your vanilla player inventory below as before …
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(playerInv,
                        col + row * 9 + 9,
                        8 + col * 18,
                        84 + row * 18
                ));
            }
        }
        for (int i = 0; i < 9; ++i) {
            addSlot(new Slot(playerInv, i, 8 + i * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel()
                .getBlockState(blockEntity.getBlockPos())
                .getBlock() instanceof DoughDividerBlock;
    }

    // --------------------------------------------
    // Slot Indices for shift-click logic
    // --------------------------------------------
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT = 2; // input + output

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // From player inventory → TE input slot
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // From TE → player inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.err.println("Invalid slot index: " + index);
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    public BlockEntity getBlockEntity() {
        return blockEntity;
    }
}
