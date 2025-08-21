package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.block.entity.StoneMillBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class StoneMillBlockMenu extends AbstractContainerMenu {
    public static final int SLOT_IN0 = 0;
    public static final int SLOT_IN1 = 1;
    public static final int SLOT_IN2 = 2;
    public static final int SLOT_OUT = 3;

    private final StoneMillBlockEntity blockEntity;

    /** progress + running */
    private final ContainerData data;

    /** energy sync for tooltip (cur/cap) */
    private final ContainerData energyData = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> blockEntity.getEnergyStored();
                case 1 -> blockEntity.getEnergyCapacity();
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { /* server authoritative */ }
        @Override public int getCount() { return 2; }
    };

    public StoneMillBlockMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public StoneMillBlockMenu(int id, Inventory playerInv, BlockEntity be) {
        super(ModMenuTypes.STONE_MILL_BLOCK_MENU.get(), id);

        if (!(be instanceof StoneMillBlockEntity mill)) {
            throw new IllegalStateException("Expected StoneMillBlockEntity but got: " + be);
        }
        this.blockEntity = mill;

        // --- Tile slots (GUI coords) ---
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(null), SLOT_IN0, 60, 44));
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(null), SLOT_IN1, 80, 51));
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(null), SLOT_IN2, 101, 44));
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(null), SLOT_OUT, 134, 38));

        // --- Player inventory ---
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int hot = 0; hot < 9; ++hot) {
            this.addSlot(new Slot(playerInv, hot, 8 + hot * 18, 142));
        }

        // --- Sync progress + running ---
        this.data = new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> blockEntity.getMixProgress();
                    case 1 -> blockEntity.isMilling() ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 2; }
        };
        addDataSlots(this.data);

        // --- Sync energy (for bulb tooltip) ---
        addDataSlots(this.energyData);
    }

    public StoneMillBlockEntity getBlockEntity() { return blockEntity; }

    // Helpers used by the Screen tooltip
    public int getEnergyStored()   { return energyData.get(0); }
    public int getEnergyCapacity() { return energyData.get(1); }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel().getBlockState(blockEntity.getBlockPos())
                .is(blockEntity.getBlockState().getBlock());
    }

    // --- Shift-click routing ---
    private static final int HOTBAR_SLOT_COUNT             = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT    = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT   = PLAYER_INVENTORY_ROW_COUNT * PLAYER_INVENTORY_COLUMN_COUNT;
    private static final int VANILLA_SLOT_COUNT            = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX      = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT       = 4; // 3 inputs + 1 output

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < VANILLA_SLOT_COUNT) {
            // Player -> TE
            if (sourceStack.is(Items.WHEAT)) {
                // Prefer input slots
                if (!moveItemStackTo(sourceStack,
                        TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + 3, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Not wheat: try output slot (usually won't insert)
                if (!moveItemStackTo(sourceStack,
                        TE_INVENTORY_FIRST_SLOT_INDEX + 3, TE_INVENTORY_FIRST_SLOT_INDEX + 4, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // TE -> Player
            if (!moveItemStackTo(sourceStack,
                    VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) sourceSlot.set(ItemStack.EMPTY);
        else sourceSlot.setChanged();
        sourceSlot.onTake(playerIn, sourceStack);
        return copy;
    }
}
