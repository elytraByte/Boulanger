// EnergyCableBlockEntity.java
package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.energy.ModEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class EnergyCableBlockEntity extends BlockEntity {
    private static final int CAP = 10_000;
    private static final int TICK_TRANSFER = 500;

    private final ModEnergyStorage buffer = new ModEnergyStorage(CAP, CAP, CAP) {
        @Override protected void onEnergyChanged() {
            setChanged();
        }
    };

    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CABLE_BE.get(), pos, state);
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return buffer;
    }

// EnergyCableBlockEntity.java (replace your tick with this, add helpers)
// constants kept: CAP, TICK_TRANSFER

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos,
                                                    BlockState st, EnergyCableBlockEntity be) {
        if (level.isClientSide()) return;

        // ───────────────────── Pull phase ─────────────────────
        int space = be.buffer.getMaxEnergyStored() - be.buffer.getEnergyStored();
        if (space > 0) {
            int pullBudget = Math.min(space, TICK_TRANSFER);
            for (Direction dir : rotatedDirs(level, pos)) {
                IEnergyStorage neigh = level.getCapability(
                        Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
                if (neigh == null) continue;

                int canExtract = neigh.extractEnergy(pullBudget, true);
                if (canExtract <= 0) continue;

                int accepted = be.buffer.receiveEnergy(canExtract, false);
                if (accepted > 0) {
                    neigh.extractEnergy(accepted, false);
                    pullBudget -= accepted;
                    if (pullBudget <= 0) break;
                }
            }
        }

        // ───────────────────── Push phase ─────────────────────
        int pushBudget = Math.min(be.buffer.getEnergyStored(), TICK_TRANSFER);
        if (pushBudget > 0) {
            // First pass: push to non-cables (consumers/storage)
            pushBudget -= pushOut(level, pos, be, pushBudget, /*allowCables*/ false);

            // Second pass: push any remainder to cables
            if (pushBudget > 0) {
                pushBudget -= pushOut(level, pos, be, pushBudget, /*allowCables*/ true);
            }
        }

        be.setChanged();
    }

    private static int pushOut(Level level, BlockPos pos, EnergyCableBlockEntity be,
                               int budget, boolean allowCables) {
        int movedTotal = 0;
        for (Direction dir : rotatedDirs(level, pos)) {
            if (budget <= 0) break;

            BlockPos np = pos.relative(dir);
            if (!allowCables && isCable(level, np)) continue;

            IEnergyStorage neigh = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK, np, dir.getOpposite());
            if (neigh == null) continue;

            int canReceive = neigh.receiveEnergy(budget, true);
            if (canReceive <= 0) continue;

            int sent = neigh.receiveEnergy(budget, false);
            if (sent > 0) {
                be.buffer.extractEnergy(sent, false);
                budget      -= sent;
                movedTotal  += sent;
            }
        }
        return movedTotal;
    }

    /** Rotate direction order by world time + position so all sides get serviced fairly. */
    private static Direction[] rotatedDirs(Level level, BlockPos pos) {
        Direction[] base = Direction.values();
        Direction[] out  = new Direction[base.length];
        int offset = (int)((level.getGameTime() + pos.asLong()) % base.length);
        for (int i = 0; i < base.length; i++) out[i] = base[(i + offset) % base.length];
        return out;
    }

    private static boolean isCable(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof EnergyCableBlockEntity;
    }

}
