package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.IngredientTypeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ScaleBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_BULK         = 0;
    public static final int SLOT_BOWL_IN      = 1;
    public static final int SLOT_BOWL_OUT     = 2;
    public static final int SLOT_RESIDUAL     = 3;

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
        ItemStack bowl = items.getStackInSlot(SLOT_BOWL_IN);

        if (bulk.isEmpty() || bowl.isEmpty() || !bowl.is(Items.BOWL) || weightToTransfer <= 0) {
            return;
        }

        // --- Water bucket branch: treat as 4000g of water ---
        if (bulk.getItem() == Items.WATER_BUCKET) {
            // How much to pour out (max 4000g)
            int toTransfer = Math.min(4000, weightToTransfer);

            // Build the filled‑bowl of water
            ItemStack filled = new ItemStack(ModItems.FILLED_BOWL_ITEM.get());
            filled.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(toTransfer));
            filled.set(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), IngredientCategory.LIQUID);
            filled.set(ModDataComponentTypes.INGREDIENT_TYPE.get(), new IngredientTypeComponent(Items.WATER_BUCKET));
            items.setStackInSlot(SLOT_BOWL_OUT, filled);

            // Give back the empty bucket
            items.setStackInSlot(SLOT_RESIDUAL, new ItemStack(Items.BUCKET));

            // Clear the inputs and reset
            items.setStackInSlot(SLOT_BULK, ItemStack.EMPTY);
            items.setStackInSlot(SLOT_BOWL_IN, ItemStack.EMPTY);
            weightToTransfer = 0;

            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            return;
        }

        // --- Original flour/ingredient logic ---
        // 1) figure full‑stack weight
        FlourType flourType = bulk.get(ModDataComponentTypes.FLOUR_TYPE.get());
        float perUnit = flourType != null
                ? flourType.getWeight()
                : bulk.has(ModDataComponentTypes.INGREDIENT_GRAMS.get())
                ? bulk.get(ModDataComponentTypes.INGREDIENT_GRAMS.get()).getWeight()
                : bulk.has(ModDataComponentTypes.FOOD_ADDITIVE.get())
                ? bulk.get(ModDataComponentTypes.FOOD_ADDITIVE.get()).getWeight()
                : 113f;

        float totalAvailable = bulk.getCount() * perUnit;
        int toTransfer = Math.min((int) totalAvailable, weightToTransfer);

        // 2) build the filled bowl
        ItemStack filled = new ItemStack(ModItems.FILLED_BOWL_ITEM.get());
        filled.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(toTransfer));
        filled.set(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), IngredientCategory.getIngredientCategory(bulk));
        filled.set(ModDataComponentTypes.INGREDIENT_TYPE.get(), new IngredientTypeComponent(bulk.getItem()));
        if (flourType != null) {
            filled.set(ModDataComponentTypes.FLOUR_TYPE.get(), flourType);
        }
        items.setStackInSlot(SLOT_BOWL_OUT, filled);

        // 3) compute remainder and refill bulk/residual
        float remaining = totalAvailable - toTransfer;
        if (remaining < 0.1f) {
            items.setStackInSlot(SLOT_BULK, ItemStack.EMPTY);
            items.setStackInSlot(SLOT_RESIDUAL, ItemStack.EMPTY);
        } else {
            int fullRemain = (int)(remaining / perUnit);
            float partialRemain = remaining - fullRemain * perUnit;

            // refill bulk slot, preserving NBT
            if (fullRemain > 0) {
                ItemStack newBulk = bulk.copy();
                newBulk.setCount(fullRemain);
                items.setStackInSlot(SLOT_BULK, newBulk);
            } else {
                items.setStackInSlot(SLOT_BULK, ItemStack.EMPTY);
            }

            // put partial remainder into residual, preserving NBT
            if (partialRemain > 0f) {
                ItemStack res = bulk.copy();
                res.setCount(1);
                res.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(partialRemain));
                items.setStackInSlot(SLOT_RESIDUAL, res);
            } else {
                items.setStackInSlot(SLOT_RESIDUAL, ItemStack.EMPTY);
            }
        }

        // 4) clear input and reset
        items.setStackInSlot(SLOT_BOWL_IN, ItemStack.EMPTY);
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
