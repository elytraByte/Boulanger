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

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos,
                                                    BlockState st, EnergyCableBlockEntity be) {
        // each tick, draw up to TICK_TRANSFER RF out of buffer and try pushing to ALL neighbors
        int want = be.buffer.extractEnergy(TICK_TRANSFER, true);
        if (want > 0) {
            for (Direction dir : Direction.values()) {
                IEnergyStorage neigh = level.getCapability(
                        Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
                if (neigh != null) {
                    int sent = neigh.receiveEnergy(want, false);
                    be.buffer.extractEnergy(sent, false);
                    want -= sent;
                    if (want <= 0) break;
                }
            }
        }
    }
}
