package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.ProofingStateComponent;
import net.boulangermod.boulanger.screen.ProofingBoxMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ProofingBoxBlockEntity extends BlockEntity implements MenuProvider {

    private static final int SLOT_COUNT = 54;
    private static final int PROOF_TIME_TICKS = 20 * 60 * 5; // 5 minutes in ticks (6000 ticks)

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    };

    private int proofTimer = 0;

    public ProofingBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROOFING_BOX.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        proofTimer = tag.getInt("ProofTimer");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.putInt("ProofTimer", proofTimer);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("proofing_box.boulanger");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ProofingBoxMenu(id, playerInventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ProofingBoxBlockEntity blockEntity) {
        if (level.isClientSide) return;

        blockEntity.proofTimer++;

        if (blockEntity.proofTimer >= PROOF_TIME_TICKS) {
            blockEntity.proofTimer = 0;
            blockEntity.proofDough();
        }
    }

    private void proofDough() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.has(ModDataComponentTypes.DOUGH_RECIPE.get())) {

                // Skip if already proofed
                ProofingStateComponent current = stack.get(ModDataComponentTypes.PROOFING_STATE.get());
                if (current != null && current.proofed()) continue;

                // Add or update proofing state
                stack.set(ModDataComponentTypes.PROOFING_STATE.get(), new ProofingStateComponent(true, 0));
                itemHandler.setStackInSlot(i, stack); // Update inventory
            }
        }
        setChanged();
    }

    public static boolean punchDownDough(ItemStack stack) {
        if (!stack.has(ModDataComponentTypes.DOUGH_RECIPE.get())) return false;

        ProofingStateComponent state = stack.get(ModDataComponentTypes.PROOFING_STATE.get());
        if (state == null || !state.proofed()) return false;

        int punches = state.punchCount();
        stack.set(ModDataComponentTypes.PROOFING_STATE.get(), new ProofingStateComponent(false, punches + 1));
        return true;
    }
}
