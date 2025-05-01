// EnergyStorageBlockEntity.java
package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.ModAttachments;
import net.boulangermod.boulanger.block.ModBlocks; // <-- implement this
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class EnergyStorageBlockEntity extends BlockEntity {
    public EnergyStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_STORAGE_BE.get(), pos, state);
    }

    /** NeoForge will call this when neighbors ask for IEnergyStorage.BLOCK */
    @Nullable
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        // Pick basic vs advanced based on which block was placed
        boolean isBasic = getBlockState().getBlock() == ModBlocks.BATTERY.get();
        var holder    = isBasic
                ? ModAttachments.BASIC_BATTERY    // DeferredHolder<…> for Tier.BASIC
                : ModAttachments.ADVANCED_BATTERY; // DeferredHolder<…> for Tier.ADVANCED

        // get() the actual AttachmentType<BatteryEnergyStorage>, then getData() returns the live storage
        return getData(holder.get());
    }
}

