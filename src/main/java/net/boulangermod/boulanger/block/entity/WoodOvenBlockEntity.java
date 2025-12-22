package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.block.WoodOvenBlock;
import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.BreadType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.item.PanItem;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.screen.WoodOvenMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;


public class WoodOvenBlockEntity extends AbstractProcessingBlockEntity implements AbstractProcessingBlock.Tickable {

    private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger();

    public static final int SLOT_INPUT      = 0;
    public static final int SLOT_FUEL       = 1;
    public static final int SLOT_OUTPUT     = 2;
    public static final int SLOT_PAN_RETURN = 3;

    private int burnTime    = 0;
    private int maxBurnTime = 0;
    private int cookTime    = 0;
    public static final int MAX_COOK_TIME = 200;

    private final ContainerData dataAccess =
            new SimpleContainerData(4) {
                @Override
                public int get(int index) {
                    return switch (index) {
                        case 0 -> burnTime;          // remaining burn
                        case 1 -> maxBurnTime;       // burn duration
                        case 2 -> cookTime;          // elapsed cook
                        case 3 -> MAX_COOK_TIME;     // total cook time
                        default -> 0;
                    };
                }

                @Override
                public void set(int index, int value) {
                    switch (index) {
                        case 0 -> burnTime = value;
                        case 1 -> maxBurnTime = value;
                        case 2 -> cookTime = value;
                        case 3 -> { /* read-only */ }
                    }
                }

                @Override
                public int getCount() { return 4; }
            };

    public WoodOvenBlockEntity(BlockPos pos, BlockState state) {
        // 4 slots: input, fuel, output, pan-return
        super(ModBlockEntities.WOOD_OVEN_BE.get(), pos, state, 4);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        boolean wasBurning = isBurning();

        if (burnTime > 0) burnTime--;

        boolean canSmelt = canCook();
        ItemStack fuelStack = itemHandler.getStackInSlot(SLOT_FUEL);

        if (burnTime == 0 && canSmelt && fuelStack.getItem() == ModItems.SPLIT_PINE_LOGS.get()) {
            burnTime = (int) (160 * 1.5f); // 240 ticks
            maxBurnTime = burnTime;
            fuelStack.shrink(1);
            setChanged();
        }

        if (isBurning() && canSmelt) {
            cookTime++;
            if (cookTime >= MAX_COOK_TIME) {
                cookTime = 0;
                tryBake();
            }
        } else {
            cookTime = 0;
        }

        boolean nowBurning = isBurning();
        if (wasBurning != nowBurning) {
            setChanged();
            level.setBlock(pos, state.setValue(WoodOvenBlock.LIT, nowBurning), Block.UPDATE_CLIENTS);
            level.sendBlockUpdated(pos, state, level.getBlockState(pos), Block.UPDATE_CLIENTS);
        }
    }

    public static boolean isCookableStack(ItemStack stack) {
        if (stack.isEmpty()) return false;

        var DS = ModDataComponentTypes.PROOFING_STATE.get();

        // Accept ANY PanItem, not a single registry object
        if (stack.getItem() instanceof PanItem) {
            PanItem.PanItemHandler h = new PanItem.PanItemHandler(stack);
            for (int i = 0, n = h.getSlots(); i < n; i++) {
                ItemStack s = h.getStackInSlot(i);
                if (s.isEmpty()) continue;
                var proof = s.get(DS);
                if (proof == null || !proof.shaped()) continue;

                if (!getBakeResult(s).isEmpty()) return true; // must resolve to a real baked item
            }
            return false;
        }

        // Loose dough
        var proof = stack.get(DS);
        return proof != null && proof.shaped() && !getBakeResult(stack).isEmpty();
    }

    public boolean tryBake() {
        IItemHandler inv = getItemHandler(null);
        ItemStack input = inv.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return false;

        // ---------- A) Pan input: bake from ONE pan out of the stack ----------
        if (input.getItem() instanceof PanItem) {
            // Work on a single pan copy; do NOT mutate the stacked ItemStack directly
            ItemStack working = input.copyWithCount(1);
            PanItem.PanItemHandler panInv = new PanItem.PanItemHandler(working);
            int slots = panInv.getSlots();
            if (slots <= 0) return false;

            var DS = ModDataComponentTypes.PROOFING_STATE.get();

            // First pass: pick a sample bread + count how many matching cavities are bakeable
            ItemStack sampleBread = ItemStack.EMPTY;
            int bakeableCount = 0;
            boolean heterogeneous = false;

            for (int s = 0; s < slots; s++) {
                ItemStack dough = panInv.getStackInSlot(s);
                if (dough.isEmpty()) continue;

                var proof = dough.get(DS);
                if (proof == null || !proof.shaped()) continue;

                ItemStack bread = getBakeResult(dough);
                if (bread.isEmpty()) continue;

                if (sampleBread.isEmpty()) {
                    sampleBread = bread.copy();
                } else if (!ItemStack.isSameItemSameComponents(sampleBread, bread)) {
                    heterogeneous = true;
                    break;
                }
                bakeableCount++;
            }
            if (sampleBread.isEmpty() || bakeableCount <= 0 || heterogeneous) return false;

            // Output capacity check (allow baking when output has room for at least 1)
            ItemStack out = inv.getStackInSlot(SLOT_OUTPUT);
            int maxStack = Math.min(sampleBread.getMaxStackSize(), 64);
            int toOutput;
            if (out.isEmpty()) {
                toOutput = Math.min(bakeableCount, maxStack);
            } else if (ItemStack.isSameItemSameComponents(out, sampleBread)) {
                int free = maxStack - out.getCount();
                if (free <= 0) return false;
                toOutput = Math.min(bakeableCount, free);
            } else {
                return false; // different item in output slot
            }
            if (toOutput <= 0) return false;

            // Second pass: extract exactly 'toOutput' doughs from the ONE 'working' pan
            int extracted = 0;
            for (int s = 0; s < slots && extracted < toOutput; s++) {
                ItemStack dough = panInv.getStackInSlot(s);
                if (dough.isEmpty()) continue;

                var proof = dough.get(DS);
                if (proof == null || !proof.shaped()) continue;

                ItemStack bread = getBakeResult(dough);
                if (bread.isEmpty() || !ItemStack.isSameItemSameComponents(sampleBread, bread)) continue;

                panInv.extractItem(s, 1, false);
                extracted++;
            }
            if (extracted <= 0) return false;

            // Push the baked batch
            ItemStack rem = inv.insertItem(SLOT_OUTPUT, sampleBread.copyWithCount(extracted), false);
            if (!rem.isEmpty()) return false; // shouldn't happen, we checked capacity

            // Remove ONE pan from the original input stack
            inv.extractItem(SLOT_INPUT, 1, false);

            // Update the processed pan’s visuals
            PanItem.syncModelToContents(working);

            // Put the processed pan somewhere sensible
            boolean workingEmpty = isPanNowEmpty(working);
            if (workingEmpty) {
                // Prefer the return slot for empty pans; if it can't accept, put it back in input
                ItemStack r = inv.insertItem(SLOT_PAN_RETURN, working.copy(), false);
                if (!r.isEmpty()) {
                    inv.insertItem(SLOT_INPUT, working, false);
                }
            } else {
                // Pan still has dough → put it back in input (it won’t stack with others if components differ)
                ItemStack r = inv.insertItem(SLOT_INPUT, working.copy(), false);
                if (!r.isEmpty()) {
                    // If input can't accept (unlikely), try the return slot as a fallback
                    inv.insertItem(SLOT_PAN_RETURN, working, false);
                }
            }

            setChanged();
            return true;
        }

        // ---------- B) Loose dough: bake one ----------
        if (!isCookableStack(input)) return false;

        ItemStack one = input.copyWithCount(1);
        ItemStack bread = getBakeResult(one);
        if (bread.isEmpty()) return false;

        // Only bake if output has room (see canCook() fix below)
        if (!canOutput(bread, 1)) return false;

        inv.extractItem(SLOT_INPUT, 1, false);
        inv.insertItem(SLOT_OUTPUT, bread, false);
        setChanged();
        return true;
    }

    private static boolean isPanNowEmpty(ItemStack pan) {
        PanItem.PanItemHandler h = new PanItem.PanItemHandler(pan);
        for (int i = 0; i < h.getSlots(); i++) {
            if (!h.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    private boolean canOutput(ItemStack item, int count) {
        if (item.isEmpty() || count <= 0) return false;
        ItemStack out = itemHandler.getStackInSlot(SLOT_OUTPUT);
        int max = Math.min(item.getMaxStackSize(), 64);
        if (out.isEmpty()) return count <= max;
        if (!ItemStack.isSameItemSameComponents(out, item)) return false;
        return out.getCount() + count <= max;
    }


    private static ItemStack getBakeResult(ItemStack dough) {
        if (dough == null || dough.isEmpty()) {
            if (LOG.isDebugEnabled()) LOG.debug("[Oven.getBakeResult] empty dough");
            return ItemStack.EMPTY;
        }

        // Prefer explicit process id; fall back to the dough recipe id
        ResourceLocation proc = dough.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
        if (proc == null) {
            var rr = dough.get(ModDataComponentTypes.DOUGH_RECIPE.get());
            if (rr != null) {
                try { proc = rr.recipeId(); } catch (Throwable ignored) {}
            }
        }
        if (LOG.isDebugEnabled()) LOG.debug("[Oven.getBakeResult] proc/recipe id={}", proc);
        if (proc == null) return ItemStack.EMPTY;

        // Normalize to last path segment (e.g. "dough_process/baguette" -> "baguette")
        String basePath = proc.getPath();
        int cut = basePath.lastIndexOf('/');
        String base = (cut >= 0) ? basePath.substring(cut + 1) : basePath;

        // Map to BreadType (your enum already has a helper)
        var btOpt = BreadType.fromRecipeId(proc); // equivalent to byId(base)
        if (btOpt.isEmpty()) {
            if (LOG.isDebugEnabled()) LOG.debug("[Oven.getBakeResult] no BreadType for base='{}'", base);
            return ItemStack.EMPTY;
        }
        BreadType bt = btOpt.get();

        // Build the output: single BREAD item with type + model index
        ItemStack out = new ItemStack(ModItems.BREAD.get());
        out.set(ModDataComponentTypes.BREAD_TYPE.get(), bt);
        out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(bt.getModelIndex()));

        // Carry a few facts forward for tooltips, if present on the dough
        var grams = dough.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        if (grams != null) out.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), grams);
        var bakerPct = dough.get(ModDataComponentTypes.BAKER_PERCENTAGES.get());
        if (bakerPct != null) out.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(), bakerPct);
        var recipe = dough.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        if (recipe != null) out.set(ModDataComponentTypes.DOUGH_RECIPE.get(), recipe);

        if (LOG.isDebugEnabled())
            LOG.debug("[Oven.getBakeResult] -> BREAD type={} (model={})", bt.id(), bt.getModelIndex());

        return out;
    }

    private boolean canCook() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return false;

        // Pan: at least one cavity produces an item that can fit in OUTPUT
        if (input.getItem() instanceof PanItem) {
            PanItem.PanItemHandler h = new PanItem.PanItemHandler(input);
            var DS = ModDataComponentTypes.PROOFING_STATE.get();

            // Find a sample output and check for at least one slot that can fit
            ItemStack sample = ItemStack.EMPTY;
            int bakeable = 0;

            for (int i = 0, n = h.getSlots(); i < n; i++) {
                ItemStack s = h.getStackInSlot(i);
                if (s.isEmpty()) continue;
                var proof = s.get(DS);
                if (proof == null || !proof.shaped()) continue;

                ItemStack bread = getBakeResult(s);
                if (bread.isEmpty()) continue;

                if (sample.isEmpty()) sample = bread.copy();
                else if (!ItemStack.isSameItemSameComponents(sample, bread)) {
                    // heterogeneous outputs: we can still bake whichever fits; keep 'sample' as the first seen
                }
                bakeable++;
            }
            if (sample.isEmpty() || bakeable <= 0) return false;

            // Only require room for at least ONE item
            return canOutput(sample, 1);
        }

        // Loose dough
        if (!isCookableStack(input)) return false;
        ItemStack bread = getBakeResult(input);
        if (bread.isEmpty()) return false;
        return canOutput(bread, 1);
    }

    private boolean isBurning() {
        return burnTime > 0;
    }

    @Override
    public void drops() {
        super.drops();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("woodoven.boulanger");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new WoodOvenMenu(id, inv, this, this.dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("Inventory", itemHandler.serializeNBT(regs));
        tag.putInt("BurnTime",    burnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
        tag.putInt("CookTime",    cookTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        itemHandler.deserializeNBT(regs, tag.getCompound("Inventory"));
        burnTime    = tag.getInt("BurnTime");
        maxBurnTime = tag.getInt("MaxBurnTime");
        cookTime    = tag.getInt("CookTime");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        return saveWithoutMetadata(regs);
    }
}
