package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.screen.SugarRefineryMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;          // NEW
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;             // NEW
import net.minecraft.sounds.SoundEvents;                   // NEW
import net.minecraft.sounds.SoundSource;                   // NEW
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IItemStackExtension;
import net.neoforged.neoforge.items.ItemStackHandler;

public class SugarRefineryBlockEntity extends AbstractProcessingBlockEntity
        implements AbstractProcessingBlock.Tickable {

    public static final int FUEL_SLOT        = 0;
    public static final int SUGARCANE_SLOT   = 1;
    public static final int SUGAR_SLOT       = 2;
    public static final int MOLASSES_SLOT    = 3;
    public static final int BROWN_SUGAR_SLOT = 4;

    private static final int CANE_PER_BATCH         = 4;
    private static final int SUGAR_PER_BATCH        = 2;
    private static final int MOLASSES_PER_BATCH     = 1;
    private static final int BROWN_SUGAR_PER_BATCH  = 1;

    private int burnTime;
    private int burnTimeTotal;
    private int cookTime;
    private int cookTimeTotal = 200;

    public SugarRefineryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUGAR_REFINERY_BE.get(), pos, state, 5);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        boolean dirty = false;

        if (burnTime > 0) burnTime--;

        boolean canRefine = canRefineSugarCane();

        if (canRefine && burnTime == 0) {
            ItemStack fuel = itemHandler.getStackInSlot(FUEL_SLOT);
            int time = fuelTime(fuel);
            if (time > 0) {
                burnTime = burnTimeTotal = time;

                ItemStack remainder = fuel.getCraftingRemainingItem();
                fuel.shrink(1);
                if (fuel.isEmpty() && !remainder.isEmpty()) {
                    itemHandler.setStackInSlot(FUEL_SLOT, remainder.copy());
                }
                setChanged();
            }
        }

        if (burnTime > 0 && canRefine) {
            cookTime++;
            if (cookTime >= cookTimeTotal) {
                cookTime = 0;
                doRefineSugarCane();
                spawnCraftEffects();   // <— NEW: fire particles + sounds when a batch finishes
                dirty = true;
            }
        } else if (cookTime > 0) {
            cookTime = Math.max(0, cookTime - 2);
            dirty = true;
        }

        if (dirty) setChanged();
    }

    private boolean canRefineSugarCane() {
        ItemStack cane = itemHandler.getStackInSlot(SUGARCANE_SLOT);
        if (!cane.is(Items.SUGAR_CANE) || cane.getCount() < CANE_PER_BATCH) return false;

        ItemStack sugar  = itemHandler.getStackInSlot(SUGAR_SLOT);
        ItemStack molass = itemHandler.getStackInSlot(MOLASSES_SLOT);
        ItemStack brown  = itemHandler.getStackInSlot(BROWN_SUGAR_SLOT);

        boolean sugarOk = sugar.isEmpty()
                || (sugar.is(Items.SUGAR) && sugar.getCount() <= sugar.getMaxStackSize() - SUGAR_PER_BATCH);
        boolean molOk = molass.isEmpty()
                || (molass.is(ModItems.MOLASSES.get()) && molass.getCount() <= molass.getMaxStackSize() - MOLASSES_PER_BATCH);
        boolean brownOk = brown.isEmpty()
                || (brown.is(ModItems.BROWN_SUGAR.get()) && brown.getCount() <= brown.getMaxStackSize() - BROWN_SUGAR_PER_BATCH);

        return sugarOk && molOk && brownOk;
    }

    private void doRefineSugarCane() {
        // consume input
        itemHandler.extractItem(SUGARCANE_SLOT, CANE_PER_BATCH, false);

        // +2 sugar
        ItemStack sugarOut = itemHandler.getStackInSlot(SUGAR_SLOT);
        if (sugarOut.isEmpty()) sugarOut = new ItemStack(Items.SUGAR, SUGAR_PER_BATCH);
        else sugarOut.grow(SUGAR_PER_BATCH);
        itemHandler.setStackInSlot(SUGAR_SLOT, sugarOut);

        // +1 molasses
        ItemStack molass = itemHandler.getStackInSlot(MOLASSES_SLOT);
        if (molass.isEmpty()) molass = new ItemStack(ModItems.MOLASSES.get(), MOLASSES_PER_BATCH);
        else molass.grow(MOLASSES_PER_BATCH);
        itemHandler.setStackInSlot(MOLASSES_SLOT, molass);

        // +1 brown sugar
        ItemStack brown = itemHandler.getStackInSlot(BROWN_SUGAR_SLOT);
        if (brown.isEmpty()) brown = new ItemStack(ModItems.BROWN_SUGAR.get(), BROWN_SUGAR_PER_BATCH);
        else brown.grow(BROWN_SUGAR_PER_BATCH);
        itemHandler.setStackInSlot(BROWN_SUGAR_SLOT, brown);
    }

    /** NEW: particle + sound burst when crafting completes */
    private void spawnCraftEffects() {
        if (!(level instanceof ServerLevel sl)) return;

        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1.0; // center-top of the stand
        double z = worldPosition.getZ() + 0.5;

        // Poof of fire/smoke
        sl.sendParticles(ParticleTypes.FLAME,       x, y, z, 16, 0.20, 0.05, 0.20, 0.01);
        sl.sendParticles(ParticleTypes.SMALL_FLAME, x, y, z, 12, 0.15, 0.03, 0.15, 0.01);
        sl.sendParticles(ParticleTypes.WHITE_SMOKE, x, y + 0.05, z, 10, 0.18, 0.04, 0.18, 0.01);

        // Whoosh + crackle
        sl.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE,       SoundSource.BLOCKS, 0.6f, 1.15f + sl.random.nextFloat() * 0.2f);
        sl.playSound(null, worldPosition, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.35f, 0.9f  + sl.random.nextFloat() * 0.2f);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SugarRefineryMenu(id, playerInv, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.boulanger.sugar_refinery");
    }

    public ItemStackHandler getInventory() { return itemHandler; }
    public int getBurnTime()      { return burnTime; }
    public int getBurnTimeTotal() { return burnTimeTotal; }
    public int getCookTime()      { return cookTime; }
    public int getCookTimeTotal() { return cookTimeTotal; }

    @Override
    public void drops() {
        drops(level, worldPosition, itemHandler);
    }

    private static int fuelTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int time = ((IItemStackExtension)(Object) stack).getBurnTime(null);
        if (time > 0) return time;
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTimeTotal", burnTimeTotal);
        tag.putInt("CookTime", cookTime);
        tag.putInt("CookTimeTotal", cookTimeTotal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        burnTime      = tag.getInt("BurnTime");
        burnTimeTotal = tag.getInt("BurnTimeTotal");
        cookTime      = tag.getInt("CookTime");
        cookTimeTotal = Math.max(1, tag.getInt("CookTimeTotal"));
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.SUGAR_REFINERY_BE.get();
    }
}
