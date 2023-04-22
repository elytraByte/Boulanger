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
import org.l3e.Boulanger.recipe.AspiratorRecipe;
import org.l3e.Boulanger.screen.AspiratorMenu;

import java.util.Objects;
import java.util.Optional;

public class AspiratorBlockEntity  extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 78;


    public AspiratorBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.ASPIRATOR.get(), blockPos, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> AspiratorBlockEntity.this.progress;
                    case 1 -> AspiratorBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> AspiratorBlockEntity.this.progress = value;
                    case 1 -> AspiratorBlockEntity.this.maxProgress = value;
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
        return Component.literal("Aspirator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new AspiratorMenu(i, inventory, this, this.data);
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

    public static void tick(Level level, BlockPos blockPos, BlockState state, AspiratorBlockEntity e) {
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

    private static void craftItem(AspiratorBlockEntity pEntity) {
        System.out.println("We're in craftItem now");
        Level level = pEntity.level;
        SimpleContainer inventory = new SimpleContainer(pEntity.itemHandler.getSlots());


        for (int i = 0; i < pEntity.itemHandler.getSlots(); i++) {
            inventory.setItem(i, pEntity.itemHandler.getStackInSlot(i));
        }

        Optional<AspiratorRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(AspiratorRecipe.Type.INSTANCE, inventory, level);



        if(hasRecipe(pEntity)) {
            System.out.println("We're in  the if statement now");
            pEntity.itemHandler.extractItem(0, 1, false);


            ItemStack output1 = new ItemStack((recipe.get().getResultItem(level.registryAccess()).getItem()),pEntity.itemHandler.getStackInSlot(1).getCount() + 1);
            CompoundTag nbtData = new CompoundTag();
            nbtData.putString("boulanger:test", "aspirated wheat berries");
            output1.setTag(nbtData);
            pEntity.itemHandler.setStackInSlot(1, output1);

            float[] chance = AspiratorRecipe.chance();
            double randomChance = Math.random() * chance[1];
            System.out.println(randomChance);

            if (randomChance >= 0.08) {
                ItemStack output2 = new ItemStack(AspiratorRecipe.getSecondaryResult().getItem(),pEntity.itemHandler.getStackInSlot(2).getCount() + 1);
                pEntity.itemHandler.setStackInSlot(2, output2);
            }

            pEntity.resetProgress();
            }
        }

    private static boolean hasRecipe(AspiratorBlockEntity e) {
        Level level = e.level;
        SimpleContainer inventory = new SimpleContainer(e.itemHandler.getSlots());
        for (int i = 0; i < e.itemHandler.getSlots(); i++) {
            inventory.setItem(i, e.itemHandler.getStackInSlot(i));
        }
        Optional<AspiratorRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(AspiratorRecipe.Type.INSTANCE, inventory, level);

        ItemStack is = e.itemHandler.getStackInSlot(0);
        CompoundTag nbt = is.getOrCreateTag();

        return recipe.isPresent() && canInsertAmountIntoOutputSlot(inventory)
                && canInsertItemIntoOutputSlot(inventory, recipe.get().getResultItem(level.registryAccess()))
                && nbt.getString("boulanger:test").equals("separated wheat berries");

    }
    private static boolean canInsertItemIntoOutputSlot(SimpleContainer inventory, ItemStack itemStack) {
        return inventory.getItem(1).getItem() == itemStack.getItem() || inventory.getItem(1).isEmpty();
    }

    private static boolean canInsertAmountIntoOutputSlot(SimpleContainer inventory) {
        return inventory.getItem(1).getMaxStackSize() > inventory.getItem(1).getCount();
    }
}