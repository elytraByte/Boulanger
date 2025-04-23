// src/main/java/net/boulangermod/boulanger/block/entity/WoodOvenBlockEntity.java
package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.BakerPctComponent;
import net.boulangermod.boulanger.component.DoughRecipeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.item.BreadType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.screen.WoodOvenMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.Containers;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WoodOvenBlockEntity extends BlockEntity implements AbstractProcessingBlock.Tickable, MenuProvider {
    public static final int SLOT_INPUT  = 0;
    public static final int SLOT_FUEL   = 1;
    public static final int SLOT_OUTPUT = 2;

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    public WoodOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOOD_OVEN_BE.get(), pos, state);
    }

    private int burnTime    = 0;
    private int maxBurnTime = 0;
    private int cookTime    = 0;
    private static final int MAX_COOK_TIME = 200;

    public int getBurnTime()    { return burnTime; }
    public int getMaxBurnTime() { return maxBurnTime; }
    public int getCookTime()    { return cookTime;    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        boolean wasBurning = isBurning();
        if (burnTime > 0) burnTime--;

        boolean canSmelt    = canCook();
        ItemStack fuelStack = itemHandler.getStackInSlot(SLOT_FUEL);

        if (burnTime == 0 && canSmelt && fuelStack.getItem() == ModItems.SPLIT_PINE_LOGS.get()) {
            burnTime    = (int)(160 * 1.5f);
            maxBurnTime = burnTime;
            fuelStack.shrink(1);
        }

        if (isBurning() && canSmelt) {
            cookTime++;
            if (cookTime >= MAX_COOK_TIME) {
                cookTime = 0;
                cook();
            }
        } else {
            cookTime = 0;
        }

        if (wasBurning != isBurning()) {
            setChanged();
        }
    }

    private boolean canCook() {
        ItemStack in  = itemHandler.getStackInSlot(SLOT_INPUT);
        ItemStack out = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (in.isEmpty() || in.getItem() != ModItems.DOUGH.get()) return false;
        if (out.isEmpty()) return true;
        if (out.getItem() != ModItems.BREAD.get()) return false;
        return out.getCount() < out.getMaxStackSize();
    }

    private void cook() {
        if (!canCook()) return;

        ItemStack input  = itemHandler.getStackInSlot(SLOT_INPUT);
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);

        BakerPctComponent    bakerPct    = input.get(ModDataComponentTypes.BAKER_PERCENTAGES.get());
        DoughRecipeComponent doughRecipe = input.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        WeightComponent      weightComp  = input.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());

        ItemStack breadStack;
        if (output.isEmpty()) {
            breadStack = new ItemStack(ModItems.BREAD.get());
        } else {
            breadStack = output.copy();
            breadStack.grow(1);
        }

        if (bakerPct    != null) breadStack.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(), bakerPct);
        if (doughRecipe != null) breadStack.set(ModDataComponentTypes.DOUGH_RECIPE.get(),      doughRecipe);
        if (weightComp  != null) breadStack.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(),  weightComp);

        if (doughRecipe != null) {
            String recipeName = doughRecipe.recipeName();
            BreadType type = BreadType.byId(recipeName).orElse(BreadType.BAGUETTE);
            breadStack.set(ModDataComponentTypes.BREAD_TYPE.get(), type);

            // ← write the custom_model_data component
            breadStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.getModelIndex()));
        }

        itemHandler.setStackInSlot(SLOT_OUTPUT, breadStack);
        input.shrink(1);
    }

    private boolean isBurning() {
        return burnTime > 0;
    }

    @Override
    public void drops() {
        SimpleContainer container = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            container.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, container);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("woodoven.boulanger");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new WoodOvenMenu(id, inv, this);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("Inventory", itemHandler.serializeNBT(regs));
        tag.putInt("BurnTime",    burnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
        tag.putInt("CookTime",    cookTime);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, HolderLookup.Provider regs) {
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
    public @NotNull net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        return saveWithoutMetadata(regs);
    }
}
