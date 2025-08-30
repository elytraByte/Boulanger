package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.block.entity.MixingBlockEntity;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.util.IngredientStack;
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

public class MixingBlockMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT  = 0; // input bowl
    public static final int FUEL_SLOT   = 1; // (your “output bowl” / spare bowl slot)
    public static final int OUTPUT_SLOT = 2; // dough

    private final MixingBlockEntity blockEntity;
    private final ContainerData data;

    // ---- Slot index layout (TE slots are added FIRST in this menu) ----
    private static final int TE_FIRST = 0;
    private static final int TE_COUNT = 3;
    private static final int TE_LAST_EXCL = TE_FIRST + TE_COUNT; // 3

    private static final int PLAYER_INV_FIRST = TE_LAST_EXCL;         // 3
    private static final int PLAYER_INV_COUNT = 27;                   // 3 rows × 9
    private static final int PLAYER_INV_LAST_EXCL = PLAYER_INV_FIRST + PLAYER_INV_COUNT; // 30

    private static final int HOTBAR_FIRST = PLAYER_INV_LAST_EXCL;     // 30
    private static final int HOTBAR_COUNT = 9;
    private static final int HOTBAR_LAST_EXCL = HOTBAR_FIRST + HOTBAR_COUNT; // 39

    // FriendlyByteBuf constructor (client)
    public MixingBlockMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    // Primary constructor
    public MixingBlockMenu(int id, Inventory playerInv, BlockEntity be) {
        super(ModMenuTypes.MIXING_BLOCK_MENU.get(), id);
        if (!(be instanceof MixingBlockEntity mixer)) {
            throw new IllegalStateException("Expected MixingBlockEntity but got: " + be);
        }
        this.blockEntity = mixer;

        // --- TE slots (added FIRST) ---
        // Input: only accepts “filled bowls”; force stack size 1
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(null), INPUT_SLOT, 44, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return isWeighedBowl(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });

        addSlot(new SlotItemHandler(blockEntity.getItemHandler(null), FUEL_SLOT, 17, 35) {
            @Override public boolean mayPlace(ItemStack stack) {
                // only bowls belong here
                return stack.is(Items.BOWL);
            }
            @Override public int getMaxStackSize() {
                // hard-cap the slot to 64, regardless of handler limits
                return 64;
            }
            @Override public int getMaxStackSize(ItemStack stack) {
                // also cap per-stack checks that moveItemStackTo uses
                return Math.min(64, super.getMaxStackSize(stack));
            }
        });

        // Dough output (usually take-only)
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(null), OUTPUT_SLOT,116, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        // --- Player inventory (3 rows) ---
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }
        // --- Hotbar ---
        for (int hot = 0; hot < 9; ++hot) {
            this.addSlot(new Slot(playerInv, hot, 8 + hot * 18, 142));
        }

        // --- Syncing progress/mixing state ---
        this.data = new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> blockEntity.getMixProgress();
                    case 1 -> blockEntity.isMixing() ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 2; }
        };
        addDataSlots(this.data);
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel()
                .getBlockState(blockEntity.getBlockPos())
                .is(blockEntity.getBlockState().getBlock());
    }

    // ---- Shift-click behavior ----
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot src = this.slots.get(index);
        if (src == null || !src.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = src.getItem();
        ItemStack original = stack.copy();

        boolean fromTE = index >= TE_FIRST && index < TE_LAST_EXCL;
        boolean fromPlayer = index >= PLAYER_INV_FIRST && index < HOTBAR_LAST_EXCL;

        // 1) Player → TE special case: filled bowl goes to INPUT, exactly one item
        if (fromPlayer && isWeighedBowl(stack)) {
            Slot input = this.slots.get(INPUT_SLOT); // TE index 0
            if (!input.hasItem() && input.mayPlace(stack)) {
                ItemStack one = stack.copy();
                one.setCount(1);
                if (this.moveItemStackTo(one, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                    stack.shrink(1);
                    src.setChanged();
                    return original;
                }
            }
            // If input occupied or move failed, fall through to normal routing
        }

        // 2) Regular routing
        if (fromPlayer) {
            // Try to move into TE (input first; outputs will usually reject)
            if (!this.moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                // Couldn’t go to input → try any TE slot range (in case you later allow more)
                if (!this.moveItemStackTo(stack, TE_FIRST, TE_LAST_EXCL, false)) {
                    // Swap between inv/hotbar
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
            }
        } else if (fromTE) {
            // TE → Player (put into main inv first, then hotbar)
            if (!this.moveItemStackTo(stack, PLAYER_INV_FIRST, PLAYER_INV_LAST_EXCL, false)
                    && !this.moveItemStackTo(stack, HOTBAR_FIRST, HOTBAR_LAST_EXCL, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        // Vanilla bookkeeping
        if (stack.isEmpty()) {
            src.set(ItemStack.EMPTY);
        } else {
            src.setChanged();
        }
        src.onTake(player, stack);

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        return original;
    }

    // “Filled bowl” = has both category and grams data components
    private static boolean isWeighedBowl(ItemStack s) {
        return s != null && !s.isEmpty()
                && s.has(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                && s.has(ModDataComponentTypes.INGREDIENT_GRAMS.get());
    }

    /** Expose the synced data for the Screen to read progress/state. */
    public ContainerData getData() { return data; }

    /** Expose the BE for sending packets (e.g. StartMixingPacket) */
    public MixingBlockEntity getBlockEntity() { return blockEntity; }

    /** How many ingredients have been added so far. */
    public int getIngredientCount() { return blockEntity.getIngredientList().size(); }

    /** Get the i-th IngredientStack from the BE. */
    public IngredientStack getIngredient(int i) { return blockEntity.getIngredientList().get(i); }
}
