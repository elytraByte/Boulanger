package net.boulangermod.boulanger.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class EnergyHelper {
    /** Push up to `amount` RF from `fromPos` into the block at `toPos`. */
    public static boolean moveEnergy(Level level, BlockPos fromPos, BlockPos toPos, int amount) {
        IEnergyStorage from = level.getCapability(Capabilities.EnergyStorage.BLOCK, fromPos, null);
        IEnergyStorage to   = level.getCapability(Capabilities.EnergyStorage.BLOCK, toPos,   null);
        if (from == null || to == null) return false;

        int extractable = Math.min(amount, from.extractEnergy(amount, true));
        if (extractable <= 0) return false;

        int received = to.receiveEnergy(extractable, false);
        from.extractEnergy(received, false);
        return received > 0;
    }

    /** Does the block at `pos` have an RF handler? */
    public static boolean hasEnergyStorage(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) != null
                && level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) != null;
    }
}
