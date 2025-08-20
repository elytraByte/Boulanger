// EnergyStorageBlockEntity.java
package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.ModAttachments;
import net.boulangermod.boulanger.block.ModBlocks; // <-- implement this
import net.boulangermod.boulanger.energy.ModEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class EnergyStorageBlockEntity extends BlockEntity {
    // Tune these
    private static final int CAPACITY     = 200_000;
    private static final int IO_PER_TICK  = 800;     // both in & out per tick
    private static final double EQ_EPS    = 0.03;    // 3% fullness delta required to equalize

    private final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, IO_PER_TICK, IO_PER_TICK) {
        @Override protected void onEnergyChanged() { setChanged(); }
    };

    public EnergyStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_STORAGE_BE.get(), pos, state);
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        // Input & output on ALL sides
        return energy;
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState st, EnergyStorageBlockEntity be) {
        if (level.isClientSide()) return;

        // 1) PULL: top up from any neighbors (up to IO_PER_TICK total)
        int pullBudget = Math.min(be.energy.getMaxEnergyStored() - be.energy.getEnergyStored(), IO_PER_TICK);
        if (pullBudget > 0) {
            for (Direction dir : rotatedDirs(level, pos)) {
                IEnergyStorage n = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
                if (n == null) continue;
                // simulate neighbor extract
                int can = n.extractEnergy(pullBudget, true);
                if (can <= 0) continue;
                int accepted = be.energy.receiveEnergy(can, false);
                if (accepted > 0) {
                    n.extractEnergy(accepted, false);
                    pullBudget -= accepted;
                    if (pullBudget <= 0) break;
                }
            }
        }

        // 2) PUSH: send out (up to IO_PER_TICK total)
        int pushBudget = Math.min(be.energy.getEnergyStored(), IO_PER_TICK);
        if (pushBudget > 0) {
            // 2a) Prefer non-batteries (consumers, machines, cables)
            pushBudget -= pushOut(level, pos, be, pushBudget, /*batteriesOk*/ false);

            // 2b) Equalize with other batteries if we’re “fuller”
            if (pushBudget > 0) {
                pushBudget -= equalizeWithBatteries(level, pos, be, pushBudget);
            }
        }
    }

    private static int pushOut(Level level, BlockPos pos, EnergyStorageBlockEntity be, int budget, boolean batteriesOk) {
        int moved = 0;
        for (Direction dir : rotatedDirs(level, pos)) {
            if (budget <= 0) break;
            IEnergyStorage n = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
            if (n == null) continue;
            if (!batteriesOk && isBattery(level, pos.relative(dir))) continue;  // skip batteries in this pass
            int canReceive = n.receiveEnergy(budget, true);
            if (canReceive <= 0) continue;
            int sent = n.receiveEnergy(budget, false);
            if (sent > 0) {
                be.energy.extractEnergy(sent, false);
                budget -= sent;
                moved  += sent;
            }
        }
        return moved;
    }

    private static int equalizeWithBatteries(Level level, BlockPos pos, EnergyStorageBlockEntity be, int budget) {
        int moved = 0;
        double myFill = fill(be.energy);
        for (Direction dir : rotatedDirs(level, pos)) {
            if (budget <= 0) break;
            BlockEntity other = level.getBlockEntity(pos.relative(dir));
            if (!(other instanceof EnergyStorageBlockEntity ob)) continue;

            IEnergyStorage n = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
            if (n == null) continue;

            double otherFill = fill(ob.energy);
            // Only send if we're meaningfully "fuller"
            if (myFill <= otherFill + EQ_EPS) continue;

            int canReceive = n.receiveEnergy(budget, true);
            if (canReceive <= 0) continue;
            int sent = n.receiveEnergy(budget, false);
            if (sent > 0) {
                be.energy.extractEnergy(sent, false);
                budget -= sent;
                moved  += sent;
                myFill = fill(be.energy); // update for next comparisons
            }
        }
        return moved;
    }

    private static boolean isBattery(Level level, BlockPos p) {
        return level.getBlockEntity(p) instanceof EnergyStorageBlockEntity;
    }

    private static double fill(IEnergyStorage es) {
        int max = es.getMaxEnergyStored();
        return max == 0 ? 0.0 : (double) es.getEnergyStored() / (double) max;
    }

    private static Direction[] rotatedDirs(Level level, BlockPos pos) {
        Direction[] base = Direction.values();
        Direction[] out  = new Direction[base.length];
        int off = (int)((level.getGameTime() + pos.asLong()) % base.length);
        for (int i = 0; i < base.length; i++) out[i] = base[(i + off) % base.length];
        return out;
    }
}

