package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.content.ingredient.ScaleTransferService;
import net.boulangermod.boulanger.content.ingredient.mass.IngredientMassResolver;
import net.boulangermod.boulanger.item.MilligramScaleItem;
import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

public final class MilligramScaleMenu
        extends AbstractContainerMenu {

    public static final int SLOT_SOURCE = 0;
    public static final int SLOT_BOWL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_RESIDUAL = 3;

    private static final int MACHINE_SLOT_COUNT = 4;

    private static final int PLAYER_INVENTORY_START =
            MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END =
            PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START =
            PLAYER_INVENTORY_END;
    private static final int HOTBAR_END =
            HOTBAR_START + 9;

    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack scaleStack;
    private final ItemStackHandler scaleInventory;

    private boolean suppressPersistence;

    public MilligramScaleMenu(
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf data
    ) {
        this(
                containerId,
                playerInventory,
                data.readEnum(InteractionHand.class)
        );
    }

    public MilligramScaleMenu(
            int containerId,
            Inventory playerInventory,
            InteractionHand hand
    ) {
        super(
                ModMenuTypes.MILLIGRAM_SCALE_MENU.get(),
                containerId
        );

        this.owner = playerInventory.player;
        this.hand = hand;
        this.scaleStack = owner.getItemInHand(hand);

        if (!scaleStack.is(
                ModItems.MILLIGRAM_SCALE.get()
        )) {
            throw new IllegalStateException(
                    "The selected hand does not contain a milligram scale"
            );
        }

        this.scaleInventory =
                new ItemStackHandler(MACHINE_SLOT_COUNT) {
                    @Override
                    public boolean isItemValid(
                            int slot,
                            ItemStack stack
                    ) {
                        return MilligramScaleMenu.this
                                .isScaleItemValid(slot, stack);
                    }

                    @Override
                    protected void onContentsChanged(
                            int slot
                    ) {
                        if (!suppressPersistence
                                && !owner.level().isClientSide()) {
                            persistContents();
                        }
                    }
                };

        loadContents();
        addMachineSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void loadContents() {
        ItemContainerContents contents =
                scaleStack.getOrDefault(
                        DataComponents.CONTAINER,
                        ItemContainerContents.EMPTY
                );

        suppressPersistence = true;

        try {
            for (int slot = 0;
                 slot < MACHINE_SLOT_COUNT;
                 slot++) {
                ItemStack stored =
                        slot < contents.getSlots()
                                ? contents.getStackInSlot(slot)
                                : ItemStack.EMPTY;

                scaleInventory.setStackInSlot(
                        slot,
                        stored.copy()
                );
            }
        } finally {
            suppressPersistence = false;
        }
    }

    private void persistContents() {
        List<ItemStack> contents =
                new ArrayList<>(MACHINE_SLOT_COUNT);

        for (int slot = 0;
             slot < MACHINE_SLOT_COUNT;
             slot++) {
            contents.add(
                    scaleInventory
                            .getStackInSlot(slot)
                            .copy()
            );
        }

        scaleStack.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(contents)
        );
    }

    private void addMachineSlots() {
        addSlot(new SlotItemHandler(
                scaleInventory,
                SLOT_SOURCE,
                26,
                17
        ));

        addSlot(new SlotItemHandler(
                scaleInventory,
                SLOT_BOWL,
                62,
                17
        ));

        addSlot(createOutputSlot(
                SLOT_RESIDUAL,
                26,
                53
        ));

        addSlot(createOutputSlot(
                SLOT_OUTPUT,
                62,
                53
        ));
    }

    private SlotItemHandler createOutputSlot(
            int slot,
            int x,
            int y
    ) {
        return new SlotItemHandler(
                scaleInventory,
                slot,
                x,
                y
        ) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
    }

    private boolean isScaleItemValid(
            int slot,
            ItemStack stack
    ) {
        return switch (slot) {
            case SLOT_SOURCE ->
                    !stack.is(
                            ModItems.MILLIGRAM_SCALE.get()
                    )
                            && IngredientMassResolver
                            .resolve(stack)
                            .isPresent();

            case SLOT_BOWL -> stack.is(Items.BOWL);

            case SLOT_OUTPUT, SLOT_RESIDUAL -> false;

            default -> false;
        };
    }

    private void addPlayerInventory(
            Inventory inventory
    ) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0;
                 column < 9;
                 column++) {
                addSlot(new Slot(
                        inventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        84 + row * 18
                ));
            }
        }
    }

    private void addPlayerHotbar(
            Inventory inventory
    ) {
        for (int column = 0;
             column < 9;
             column++) {
            addSlot(new Slot(
                    inventory,
                    column,
                    8 + column * 18,
                    142
            ));
        }
    }

    @Override
    public boolean clickMenuButton(
            Player player,
            int requestedMilligrams
    ) {
        if (requestedMilligrams <= 0
                || requestedMilligrams
                > MilligramScaleItem
                .MAX_CAPACITY_MILLIGRAMS) {
            return false;
        }

        return measureMilligrams(
                requestedMilligrams
        );
    }

    private boolean measureMilligrams(
            long requestedMilligrams
    ) {
        if (owner.level().isClientSide()) {
            return false;
        }

        var proposed = ScaleTransferService.plan(
                scaleInventory.getStackInSlot(
                        SLOT_SOURCE
                ),
                scaleInventory.getStackInSlot(
                        SLOT_BOWL
                ),
                scaleInventory.getStackInSlot(
                        SLOT_OUTPUT
                ),
                scaleInventory.getStackInSlot(
                        SLOT_RESIDUAL
                ),
                scaleInventory.getSlotLimit(
                        SLOT_OUTPUT
                ),
                scaleInventory.getSlotLimit(
                        SLOT_RESIDUAL
                ),
                requestedMilligrams
        );

        if (proposed.isEmpty()) {
            return false;
        }

        ScaleTransferService.Result result =
                proposed.get();

        suppressPersistence = true;

        try {
            scaleInventory.setStackInSlot(
                    SLOT_SOURCE,
                    result.sourceAfter()
            );

            scaleInventory.setStackInSlot(
                    SLOT_BOWL,
                    result.bowlsAfter()
            );

            scaleInventory.setStackInSlot(
                    SLOT_OUTPUT,
                    result.outputAfter()
            );

            scaleInventory.setStackInSlot(
                    SLOT_RESIDUAL,
                    result.residualAfter()
            );
        } finally {
            suppressPersistence = false;
        }

        persistContents();
        broadcastChanges();

        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner
                && owner.isAlive()
                && owner.getItemInHand(hand)
                == scaleStack;
    }

    @Override
    public void removed(Player player) {
        if (!player.level().isClientSide()) {
            persistContents();
        }

        super.removed(player);
    }

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {
        Slot slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();

        /*
         * Do not allow the scale currently hosting this menu
         * to be moved into its own internal inventory.
         */
        if (stackInSlot == scaleStack) {
            return ItemStack.EMPTY;
        }

        ItemStack originalStack =
                stackInSlot.copy();

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
                    SLOT_BOWL,
                    SLOT_BOWL + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (scaleInventory.isItemValid(
                SLOT_SOURCE,
                stackInSlot
        )) {
            if (!moveItemStackTo(
                    stackInSlot,
                    SLOT_SOURCE,
                    SLOT_SOURCE + 1,
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

        if (stackInSlot.getCount()
                == originalStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stackInSlot);
        return originalStack;
    }
}