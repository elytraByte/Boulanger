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
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == ModItems.SPLIT_PINE_LOGS.get();
            }
        });
        this.addSlot(new SlotItemHandler(h, 1, 26, 57) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(ItemTags.LOGS);
            }
        });
        this.addSlot(new SlotItemHandler(h, 2, 56, 21) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == ModItems.GASIFIER_FILTER.get();
            }
        });
        this.addSlot(new SlotItemHandler(h, 3, 56, 57) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == ModItems.GASIFIER_FILTER.get();
            }
        });

        // ─── PLAYER INV + HOTBAR ────────────────────────────────────
        final int yOffset = 5;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(
                        playerInv,
                        col + row * 9 + 9,
                        8  + col * 18,
                        84 + row * 18 + yOffset
                ));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(
                    playerInv,
                    col,
                    8  + col * 18,
                    142 + yOffset
            ));
        }

        this.addDataSlots(data);
    }

    public WoodGasifierMenu(int windowId,
                            Inventory playerInv,
                            FriendlyByteBuf buf) {
        this(windowId,
                playerInv,
                (WoodGasifierBlockEntity) playerInv.player
                        .level()
                        .getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(3)
        );
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                ContainerLevelAccess.create(blockEntity.getLevel(), pos),
                player,
                ModBlocks.WOOD_GASIFIER.get()
        );
    }

    private static final int HOTBAR_SLOT_COUNT             = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT    = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT   = PLAYER_INVENTORY_ROW_COUNT * PLAYER_INVENTORY_COLUMN_COUNT;
    private static final int VANILLA_SLOT_COUNT            = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT       = 4;

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        if (index < VANILLA_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack,
                    TE_INVENTORY_FIRST_SLOT_INDEX,
                    TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT,
                    false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack,
                    0,
                    VANILLA_SLOT_COUNT,
                    false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyStack;
    }

    public int getBurnProgress()    { return data.get(0); }
    public int getEnergyStored()    { return data.get(1); }
    public int getMaxEnergyStored() { return data.get(2); }

    // The total burn time for one log. Matches the BE's BURN_TIME_PER_LOG.
    public int getMaxBurnProgress() {
        return WoodGasifierBlockEntity.getBurnTimePerLog();
    }
}
