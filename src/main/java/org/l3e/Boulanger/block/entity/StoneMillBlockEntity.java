package org.l3e.Boulanger.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.l3e.Boulanger.recipe.StoneMillRecipe;
import org.l3e.Boulanger.screen.StoneMillMenu;

import java.util.Optional;

public class StoneMillBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 78;


    public StoneMillBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.STONE_MILL.get(), blockPos, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> StoneMillBlockEntity.this.progress;
                    case 1 -> StoneMillBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> StoneMillBlockEntity.this.progress = value;
                    case 1 -> StoneMillBlockEntity.this.maxProgress = value;
                }

            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        //can this be localized?
        return Component.literal("Stone Mill");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new StoneMillMenu(i, inventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
        if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return lazyItemHandler.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));
        super.load(nbt);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public static void tick(Level level, BlockPos blockPos, BlockState state, StoneMillBlockEntity e) {
        if(level.isClientSide()) {
            return;
        }

        if(hasRecipe(e)) {
            e.progress++;
            setChanged(level, blockPos, state);

            if(e.progress >= e.maxProgress) {
                craftItem(e);
            }
        } else {
            e.resetProgress();
            setChanged(level, blockPos, state);
        }
    }

    private void resetProgress() {
        this.progress = 0;
    }

    private static void craftItem(StoneMillBlockEntity pEntity) {
        Level level = pEntity.level;
        SimpleContainer inventory = new SimpleContainer(pEntity.itemHandler.getSlots());


        for (int i = 0; i < pEntity.itemHandler.getSlots(); i++) {
            inventory.setItem(i, pEntity.itemHandler.getStackInSlot(i));
        }

        Optional<StoneMillRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(StoneMillRecipe.Type.INSTANCE, inventory, level);

        if(hasRecipe(pEntity)) {
            pEntity.itemHandler.extractItem(1, 1, false);
            pEntity.itemHandler.setStackInSlot(0, new ItemStack(recipe.get().getResultItem(level.registryAccess()).getItem(),
                    pEntity.itemHandler.getStackInSlot(0).getCount() + 1));

            pEntity.resetProgress();
        }
    }

    private static boolean hasRecipe(StoneMillBlockEntity e) {
        Level level = e.level;
        SimpleContainer inventory = new SimpleContainer(e.itemHandler.getSlots());
        for (int i = 0; i < e.itemHandler.getSlots(); i ++) {
            inventory.setItem(i, e.itemHandler.getStackInSlot(i));
        }

        Optional<StoneMillRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(StoneMillRecipe.Type.INSTANCE, inventory, level);


        return recipe.isPresent() && canInsertAmountIntoOutputSlot(inventory) && canInsertItemIntoOutputSlot(inventory,  recipe.get().getResultItem(level.registryAccess()));

    }

    private static boolean canInsertItemIntoOutputSlot(SimpleContainer inventory, ItemStack itemStack) {
        return inventory.getItem(0).getItem() == itemStack.getItem() || inventory.getItem(0).isEmpty();
    }

    private static boolean canInsertAmountIntoOutputSlot(SimpleContainer inventory) {
        return inventory.getItem(0).getMaxStackSize() > inventory.getItem(0).getCount();
    }
}
