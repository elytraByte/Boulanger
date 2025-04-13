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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WoodOvenBlockEntity extends BlockEntity implements AbstractProcessingBlock.Tickable, MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
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

    private int burnTime = 0;
    private int maxBurnTime = 0;
    private int cookTime = 0;
    private static final int MAX_COOK_TIME = 200; // Standard furnace time

    public int getBurnTime() { return burnTime; }
    public int getMaxBurnTime() { return maxBurnTime; }
    public int getCookTime() { return cookTime; }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        boolean wasBurning = isBurning();

        if (burnTime > 0) {
            burnTime--;
        }

        boolean canSmelt = canCook();
        ItemStack fuelStack = itemHandler.getStackInSlot(SLOT_FUEL);

        if (burnTime == 0 && canSmelt && fuelStack.getItem() == ModItems.SPLIT_PINE_LOGS.get()) {
            burnTime = (int)(160 * 1.5f);
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
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);

        // Only allow cooking your dough into your bread
        if (input.isEmpty() || input.getItem() != ModItems.DOUGH.get()) return false;
        if (output.isEmpty()) return true;
        if (output.getItem() != ModItems.BREAD.get()) return false;
        return output.getCount() < output.getMaxStackSize();
    }

    private void cook() {
        if (!canCook()) return;

        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);

        // Read all components from the dough
        BakerPctComponent bakerPct = input.get(ModDataComponentTypes.BAKER_PERCENTAGES.get());
        DoughRecipeComponent doughRecipe = input.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        WeightComponent weightComp = input.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());

        // Create the new bread stack (or grow existing)
        ItemStack breadStack;
        if (output.isEmpty()) {
            breadStack = new ItemStack(ModItems.BREAD.get());
        } else {
            breadStack = output.copy();
            breadStack.grow(1);
        }

        // 1) Copy baker's percentages
        if (bakerPct != null) {
            breadStack.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(), bakerPct);
        }

        // 2) Copy the dough recipe component
        if (doughRecipe != null) {
            breadStack.set(ModDataComponentTypes.DOUGH_RECIPE.get(), doughRecipe);
        }

        // 3) Copy weight
        if (weightComp != null) {
            breadStack.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), weightComp);
        }

        // 4) Set the BREAD_TYPE from the recipe name
        if (doughRecipe != null) {
            String recipeName = doughRecipe.recipeName();          // e.g. "baguette"
            BreadType type = BreadType.byId(recipeName)
                    .orElse(BreadType.BAGUETTE);
            breadStack.set(ModDataComponentTypes.BREAD_TYPE.get(), type);
        }

        // 5) Commit to the output slot and consume one dough
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
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WoodOvenMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // Persist the three inventory slots
        tag.put("Inventory", itemHandler.serializeNBT(registries));

        // Persist oven state
        tag.putInt("BurnTime",  this.burnTime);
        tag.putInt("MaxBurnTime", this.maxBurnTime);
        tag.putInt("CookTime",  this.cookTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // Restore the inventory
        itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));

        // Restore oven state
        this.burnTime     = tag.getInt("BurnTime");
        this.maxBurnTime  = tag.getInt("MaxBurnTime");
        this.cookTime     = tag.getInt("CookTime");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }


}
