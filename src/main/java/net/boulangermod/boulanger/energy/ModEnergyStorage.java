package net.boulangermod.boulanger.energy;

import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * A little wrapper around Neoforge’s EnergyStorage that
 * notifies when its internal energy changes.
 */
public abstract class ModEnergyStorage extends EnergyStorage {
    public ModEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }


    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (extracted > 0) {
            onEnergyChanged();
        }
        return extracted;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (received > 0) {
            onEnergyChanged();
        }
        return received;
    }

    /**
     * Force-set the internal energy (used when loading NBT).
     */
    public void setEnergy(int energy) {
        this.energy = energy;
    }

    /**
     * Called whenever extractEnergy or receiveEnergy actually changes the stored energy.
     * You should call setChanged() + send block updates here.
     */
    protected abstract void onEnergyChanged();

    public int getMaxExtract() {
        return this.maxExtract;
    }

    public int getMaxReceive() {
        return this.maxReceive;
    }
}
