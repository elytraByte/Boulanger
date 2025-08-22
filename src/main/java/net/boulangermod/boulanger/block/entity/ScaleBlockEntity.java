package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.screen.ScaleBlockMenu;
import net.boulangermod.boulanger.util.ScaleLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScaleBlockEntity extends AbstractProcessingBlockEntity {
    public static final int SLOT_BULK     = 0;
    public static final int SLOT_BOWL_IN  = 1;
    public static final int SLOT_BOWL_OUT = 2;
    public static final int SLOT_RESIDUAL = 3;

    private static final Logger LOGGER = LogManager.getLogger();

    private int weightToTransfer = 0; // in grams

    public ScaleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCALE_BLOCK_BE.get(), pos, state, 4);
    }

    /** Called by the menu when the user enters a target weight */
    public void setTargetWeight(int grams) {
        this.weightToTransfer = grams;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("scale.boulanger");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ScaleBlockMenu(id, inv, this);
    }

    /** Perform the transfer: uses ScaleLogic.transfer() & createFilledBowl() */
    public void transferToBowl() {
        if (level == null || level.isClientSide()) return;

        ItemStack bulk   = itemHandler.getStackInSlot(SLOT_BULK);
        ItemStack bowlIn = itemHandler.getStackInSlot(SLOT_BOWL_IN);
        ItemStack residualSlot = itemHandler.getStackInSlot(SLOT_RESIDUAL);

        if (bulk.isEmpty() || bowlIn.isEmpty() || !bowlIn.is(Items.BOWL) || weightToTransfer <= 0) {
            return;
        }
        if (!residualSlot.isEmpty()) {
            LOGGER.warn("Residual slot occupied, cannot transfer");
            return;
        }

        // request mg = grams * 1000
        int requestedMg = weightToTransfer * 1000;
        ScaleLogic.TransferResult result = ScaleLogic.transfer(bulk, requestedMg);

        // nothing moved?
        if (result.transferredMg <= 0) {
            return;
        }

        // put the filled bowl
        ItemStack filled = ScaleLogic.createFilledBowl(bulk, result.transferredMg);
        itemHandler.setStackInSlot(SLOT_BOWL_OUT, filled);

        // update bulk + residual
        itemHandler.setStackInSlot(SLOT_BULK, result.newBulkStack);
        itemHandler.setStackInSlot(SLOT_RESIDUAL, result.residualStack);

        // consume one empty bowl
        bowlIn.shrink(1);
        itemHandler.setStackInSlot(SLOT_BOWL_IN, bowlIn.isEmpty() ? ItemStack.EMPTY : bowlIn);

        LOGGER.debug("Transferred {} mg ({} g) from {} into bowl",
                result.transferredMg,
                result.transferredMg / 1000f,
                bulk.getItem());

        // reset and notify
        weightToTransfer = 0;
        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("Inventory", itemHandler.serializeNBT(regs));
        tag.putInt("WeightToTransfer", weightToTransfer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        itemHandler.deserializeNBT(regs, tag.getCompound("Inventory"));
        weightToTransfer = tag.getInt("WeightToTransfer");
    }
}
