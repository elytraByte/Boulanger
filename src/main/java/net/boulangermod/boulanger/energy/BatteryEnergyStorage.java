package net.boulangermod.boulanger.energy;

import net.boulangermod.boulanger.energy.ModEnergyStorage;

/**
 * A concrete ModEnergyStorage for our battery blocks, supporting multiple tiers.
 */
public class BatteryEnergyStorage extends ModEnergyStorage {
    /**
     * Defines the available battery tiers and their characteristics.
     */
    public enum Tier {
        BASIC(200_000, 1_000),      // 200k RF capacity, 1k RF I/O
        ADVANCED(1_000_000, 5_000); // 1M RF capacity, 5k RF I/O

        private final int capacity;
        private final int maxIo;

        Tier(int capacity, int maxIo) {
            this.capacity = capacity;
            this.maxIo = maxIo;
        }

        /**
         * Maximum energy this tier can store.
         */
        public int getCapacity() {
            return capacity;
        }

        /**
         * Maximum energy this tier can receive or extract per tick.
         */
        public int getMaxIo() {
            return maxIo;
        }
    }

    /**
     * Creates a battery storage of the given tier.
     * @param tier the battery tier to instantiate
     */
    public BatteryEnergyStorage(Tier tier) {
        super(tier.getCapacity(), tier.getMaxIo(), tier.getMaxIo());
    }

    /**
     * Convenience constructor: defaults to BASIC tier.
     */
    public BatteryEnergyStorage() {
        this(Tier.BASIC);
    }

    @Override
    protected void onEnergyChanged() {
        // No-op: Attachment or BE will handle marking dirty and updates.
    }
}