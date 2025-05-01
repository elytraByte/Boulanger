package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.block.entity.StoneMillBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class StoneMillBlockMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT   = 0;
    public static final int OUTPUT_SLOT = 1;

    private final StoneMillBlockEntity blockEntity;
   private final ContainerData data;

    // FriendlyByteBuf constructor (called on client)
    public StoneMillBlockMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, (StoneMillBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    // Primary constructor (server & client after the above redirects here)
    public StoneMillBlockMenu(int id, Inventory playerInv, BlockEntity be) {
        super(ModMenuTypes.STONE_MILL_BLOCK_MENU.get(), id);

        if (!(be instanceof StoneMillBlockEntity mixer)) {
            throw new IllegalStateException("Expected StoneMillBlockEntity but got: " + be);
        }
        this.blockEntity = mixer;

        // --- TileEntity slots ---
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(), INPUT_SLOT,  44, 35)); // input bowl
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(), OUTPUT_SLOT,   116, 35)); // output bowl
        // --- Player inventory slots ---
        // main inventory, 3 rows × 9 cols
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }
        // hotbar
        for (int hot = 0; hot < 9; ++hot) {
            this.addSlot(new Slot(playerInv, hot,
                    8 + hot * 18, 142));
        }

        // --- Syncing progress/mixing state ---
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> blockEntity.getMixProgress();
                    case 1 -> blockEntity.isMilling() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // no-op: server writes into BE, client only reads
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
        addDataSlots(this.data);
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel()
                .getBlockState(blockEntity.getBlockPos())
                .is(blockEntity.getBlockState().getBlock());
    }

    // Quick‐move (shift‐click) logic
    private static final int HOTBAR_SLOT_COUNT             = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT    = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT   = PLAYER_INVENTORY_ROW_COUNT * PLAYER_INVENTORY_COLUMN_COUNT;
    private static final int VANILLA_SLOT_COUNT            = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX      = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT       = 3; // input, fuel, output

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        if (index < VANILLA_SLOT_COUNT) {
            // from player inventory → TE
            if (!moveItemStackTo(sourceStack,
                    TE_INVENTORY_FIRST_SLOT_INDEX,
                    TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT,
                    false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // from TE → player inventory
            if (!moveItemStackTo(sourceStack,
                    VANILLA_FIRST_SLOT_INDEX,
                    VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT,
                    false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyStack;
    }
}
