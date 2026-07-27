package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.block.entity.ScaleBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class ScaleBlockMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = 4;

    private static final int PLAYER_INVENTORY_START =
            MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END =
            PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START =
            PLAYER_INVENTORY_END;
    private static final int HOTBAR_END =
            HOTBAR_START + 9;

    private final ScaleBlockEntity blockEntity;
    private final IItemHandler scaleInventory;

    public ScaleBlockMenu(
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf data
    ) {
        this(
                containerId,
                playerInventory,
                findScale(playerInventory, data)
        );
    }

    public ScaleBlockMenu(
            int containerId,
            Inventory playerInventory,
            ScaleBlockEntity blockEntity
    ) {
        super(ModMenuTypes.SCALE_MENU.get(), containerId);

        this.blockEntity = blockEntity;
        this.scaleInventory = blockEntity.getItemHandler(null);

        addMachineSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private static ScaleBlockEntity findScale(
            Inventory playerInventory,
            FriendlyByteBuf data
    ) {
        BlockEntity blockEntity =
                playerInventory.player.level()
                        .getBlockEntity(data.readBlockPos());

        if (blockEntity instanceof ScaleBlockEntity scale) {
            return scale;
        }

        throw new IllegalStateException(
                "Expected a ScaleBlockEntity at the supplied position"
        );
    }

    private void addMachineSlots() {
        addSlot(new SlotItemHandler(
                scaleInventory,
                ScaleBlockEntity.SLOT_SOURCE,
                26,
                17
        ));

        addSlot(new SlotItemHandler(
                scaleInventory,
                ScaleBlockEntity.SLOT_BOWL,
                62,
                17
        ));

        addSlot(createOutputSlot(
                ScaleBlockEntity.SLOT_RESIDUAL,
                26,
                53
        ));

        addSlot(createOutputSlot(
                ScaleBlockEntity.SLOT_OUTPUT,
                62,
                53
        ));
    }

    private SlotItemHandler createOutputSlot(
            int inventorySlot,
            int x,
            int y
    ) {
        return new SlotItemHandler(
                scaleInventory,
                inventorySlot,
                x,
                y
        ) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        inventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        84 + row * 18
                ));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(
                    inventory,
                    column,
                    8 + column * 18,
                    142
            ));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity.getLevel() == null) {
            return false;
        }

        return AbstractContainerMenu.stillValid(
                ContainerLevelAccess.create(
                        blockEntity.getLevel(),
                        blockEntity.getBlockPos()
                ),
                player,
                ModBlocks.SCALE.get()
        );
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId <= 0) {
            return false;
        }

        return blockEntity.measure((long) buttonId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack originalStack = stackInSlot.copy();

        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(
                    stackInSlot,
                    PLAYER_INVENTORY_START,
                    HOTBAR_END,
                    true
            )) {
                return ItemStack.EMPTY;
            }
        } else if (stackInSlot.is(Items.BOWL)) {
            if (!moveItemStackTo(
                    stackInSlot,
                    ScaleBlockEntity.SLOT_BOWL,
                    ScaleBlockEntity.SLOT_BOWL + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (scaleInventory.isItemValid(
                ScaleBlockEntity.SLOT_SOURCE,
                stackInSlot
        )) {
            if (!moveItemStackTo(
                    stackInSlot,
                    ScaleBlockEntity.SLOT_SOURCE,
                    ScaleBlockEntity.SLOT_SOURCE + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(
                    stackInSlot,
                    HOTBAR_START,
                    HOTBAR_END,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (index < HOTBAR_END) {
            if (!moveItemStackTo(
                    stackInSlot,
                    PLAYER_INVENTORY_START,
                    PLAYER_INVENTORY_END,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stackInSlot.getCount() == originalStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stackInSlot);
        return originalStack;
    }
}