package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.IngredientTypeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.item.FiftyPoundBagItem;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.screen.ScaleBlockMenu;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScaleBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_BULK         = 0;
    public static final int SLOT_BOWL_IN      = 1;
    public static final int SLOT_BOWL_OUT     = 2;
    public static final int SLOT_RESIDUAL     = 3;
    private static final Logger LOGGER = LogManager.getLogger();

    private final ItemStackHandler items = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    };

    private int weightToTransfer = 0;


    public ScaleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCALE_BLOCK_BE.get(), pos, state);
    }

    public void setTargetWeight(int grams) {
        this.weightToTransfer = grams;
    }

    public void setGramsToWeigh(int grams) {
        setTargetWeight(grams);
    }

    public void transferToBowl() {
        if (level == null || level.isClientSide()) return;

        ItemStack bulk = items.getStackInSlot(SLOT_BULK);
        ItemStack bowlIn = items.getStackInSlot(SLOT_BOWL_IN);

        if (bulk.isEmpty() || bowlIn.isEmpty() || !bowlIn.is(Items.BOWL) || weightToTransfer <= 0) {
            return;
        }

        // Prevent transfer if residual slot is occupied
        if (!items.getStackInSlot(SLOT_RESIDUAL).isEmpty()) {
            LOGGER.warn("Residual slot is not empty — cannot transfer remaining bulk item.");
            return;
        }

        // --- Water bucket special-case: treat as 4000g of water ---
        if (bulk.getItem() == Items.WATER_BUCKET) {
            int toTransfer = Math.min(4000, weightToTransfer);

            ItemStack filled = new ItemStack(ModItems.FILLED_BOWL_ITEM.get());
            filled.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(toTransfer));
            filled.set(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), IngredientCategory.WATER);
            filled.set(ModDataComponentTypes.INGREDIENT_TYPE.get(), new IngredientTypeComponent(Items.WATER_BUCKET));
            items.setStackInSlot(SLOT_BOWL_OUT, filled);

            items.setStackInSlot(SLOT_RESIDUAL, new ItemStack(Items.BUCKET));
            items.setStackInSlot(SLOT_BULK, ItemStack.EMPTY);

            bowlIn.shrink(1);
            items.setStackInSlot(SLOT_BOWL_IN, bowlIn.isEmpty() ? ItemStack.EMPTY : bowlIn);

            weightToTransfer = 0;
            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            return;
        }

        // === General ingredient logic (flour, additives, etc.) ===

        // Determine weight per item or per bag
        FlourType flourType = bulk.get(ModDataComponentTypes.FLOUR_TYPE.get());

        float perUnit = flourType != null
                ? flourType.getWeight()
                : bulk.has(ModDataComponentTypes.INGREDIENT_GRAMS.get())
                ? bulk.get(ModDataComponentTypes.INGREDIENT_GRAMS.get()).getWeight()
                : bulk.has(ModDataComponentTypes.FOOD_ADDITIVE.get())
                ? bulk.get(ModDataComponentTypes.FOOD_ADDITIVE.get()).getWeight()
                : 113f;

        float totalAvailable;
        boolean isBulkBag = bulk.getItem() instanceof FiftyPoundBagItem &&
                bulk.has(ModDataComponentTypes.INGREDIENT_GRAMS.get());

        if (isBulkBag) {
            totalAvailable = bulk.get(ModDataComponentTypes.INGREDIENT_GRAMS.get()).grams();
            perUnit = totalAvailable;
        } else {
            totalAvailable = bulk.getCount() * perUnit;
        }

        int toTransfer = Math.min((int) totalAvailable, weightToTransfer);

        // Build the filled bowl
        ItemStack filled = new ItemStack(ModItems.FILLED_BOWL_ITEM.get());
        filled.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(toTransfer));
        filled.set(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), IngredientCategory.getIngredientCategory(bulk));

        Item ingredientItem = bulk.getItem() instanceof FiftyPoundBagItem
                ? ModItems.FLOUR_ITEM.get()
                : bulk.getItem();
        filled.set(ModDataComponentTypes.INGREDIENT_TYPE.get(), new IngredientTypeComponent(ingredientItem));

        if (flourType != null) {
            filled.set(ModDataComponentTypes.FLOUR_TYPE.get(), flourType);
        }

        items.setStackInSlot(SLOT_BOWL_OUT, filled);

        // Handle leftovers
        float remaining = totalAvailable - toTransfer;

        if (isBulkBag) {
            if (remaining <= 0f) {
                items.setStackInSlot(SLOT_BULK, ItemStack.EMPTY);
                items.setStackInSlot(SLOT_RESIDUAL, ItemStack.EMPTY);
            } else {
                ItemStack updatedBag = bulk.copy();
                updatedBag.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(remaining));
                items.setStackInSlot(SLOT_BULK, ItemStack.EMPTY);
                items.setStackInSlot(SLOT_RESIDUAL, updatedBag);
            }
        } else {
            int fullRemain = (int)(remaining / perUnit);
            float partialRemain = remaining - fullRemain * perUnit;

            if (fullRemain > 0) {
                ItemStack newBulk = bulk.copy();
                newBulk.setCount(fullRemain);
                items.setStackInSlot(SLOT_BULK, newBulk);
            } else {
                items.setStackInSlot(SLOT_BULK, ItemStack.EMPTY);
            }

            if (partialRemain > 0f) {
                ItemStack residual = bulk.copy();
                residual.setCount(1);
                residual.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(partialRemain));
                items.setStackInSlot(SLOT_RESIDUAL, residual);
            } else {
                items.setStackInSlot(SLOT_RESIDUAL, ItemStack.EMPTY);
            }
        }

        // Use up one empty bowl
        bowlIn.shrink(1);
        items.setStackInSlot(SLOT_BOWL_IN, bowlIn.isEmpty() ? ItemStack.EMPTY : bowlIn);

        LOGGER.debug("Transferred {}g from {} to bowl. Remaining: {}g",
                toTransfer, bulk.getItem(), remaining);

        weightToTransfer = 0;
        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }


    public ItemStackHandler getItemHandler() {
        return items;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("scale.boulanger");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player pl) {
        return new ScaleBlockMenu(id, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("Inventory", items.serializeNBT(regs));
        tag.putInt("WeightToTransfer", weightToTransfer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        items.deserializeNBT(regs, tag.getCompound("Inventory"));
        weightToTransfer = tag.getInt("WeightToTransfer");
    }
}
