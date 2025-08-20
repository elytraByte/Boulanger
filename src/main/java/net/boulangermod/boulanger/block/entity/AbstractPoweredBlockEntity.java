package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.energy.ModEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public abstract class AbstractPoweredBlockEntity extends AbstractProcessingBlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final ModEnergyStorage energy;
    protected final int energyPerTick;

    protected AbstractPoweredBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int slotCount,
            int energyCapacity,
            int maxReceive,
            int energyPerTick
    ) {
        super(type, pos, state, slotCount);
        this.energyPerTick = energyPerTick;
        this.energy = new ModEnergyStorage(energyCapacity, maxReceive, 0) {
            @Override
            protected void onEnergyChanged() {
                setChanged();
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
        };
    }

    // Returns how much was actually moved.
    protected int pullEnergyFromNeighbors(int maxPullPerTick) {
        if (level == null || level.isClientSide) return 0;
        int movedTotal = 0;
        int free = energy.getMaxEnergyStored() - energy.getEnergyStored();
        if (free <= 0) return 0;

        // try each side
        for (var dir : Direction.values()) {
            if (movedTotal >= maxPullPerTick) break;

            var neighborPos = worldPosition.relative(dir);
            // ask for the neighbor's energy storage on the face touching us
            var src = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, dir.getOpposite());
            if (src == null) continue;

            // How much we *could* take this tick
            int want = Math.min(maxPullPerTick - movedTotal, free);

            // Simulate to avoid over-extraction
            int canExtract = src.extractEnergy(want, true);
            if (canExtract <= 0) continue;

            // Receive into us
            int received = energy.receiveEnergy(canExtract, false);
            if (received <= 0) continue;

            // Finalize extraction from neighbor
            int actuallyExtracted = src.extractEnergy(received, false);

            movedTotal += actuallyExtracted;
            free      -= actuallyExtracted;

            // Debug
            LOGGER.debug("[{} @ {}] pulled {} FE from {} (face {}), now {}/{}",
                    getClass().getSimpleName(), worldPosition, actuallyExtracted,
                    neighborPos, dir.getOpposite(), energy.getEnergyStored(), energy.getMaxEnergyStored());

            if (free <= 0) break;
        }
        if (movedTotal > 0) setChangedAndNotify();
        return movedTotal;
    }

    // --- Helpers for children ---
    protected boolean hasPowerForTick() {
        return energy.getEnergyStored() >= energyPerTick;
    }

    protected boolean tryConsumePowerForTick() {
        if (!hasPowerForTick()) return false;
        energy.extractEnergy(energyPerTick, false);
        return true;
    }

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getEnergyCapacity() {
        return energy.getMaxEnergyStored();
    }

    // --- Persistence ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) {
            energy.setEnergy(tag.getInt("Energy"));
        }
    }
}
