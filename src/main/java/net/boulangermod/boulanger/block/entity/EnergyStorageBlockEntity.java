package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.energy.ModEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class EnergyStorageBlockEntity extends BlockEntity {
    // Tunables
    private static final int CAPACITY    = 200_000;
    private static final int IO_PER_TICK = 800; // pull & push limits

    private final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, IO_PER_TICK, IO_PER_TICK) {
        @Override protected void onEnergyChanged() { setChanged(); }
    };

    /** Sided wrappers: DOWN = input-only; others = output-only. */
    private final IEnergyStorage[] sideCaps = new IEnergyStorage[6];

    public EnergyStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_STORAGE_BE.get(), pos, state);
        for (Direction d : Direction.values()) sideCaps[d.ordinal()] = new Port(this, d);
    }

    /** Expose to ModCapabilities. */
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return (side == null) ? energy : sideCaps[side.ordinal()];
    }

    // ── Tick ────────────────────────────────────────────────────────────
    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState st, EnergyStorageBlockEntity be) {
        if (level.isClientSide()) return;

        // 1) Pull from bottom only
        int free = be.energy.getMaxEnergyStored() - be.energy.getEnergyStored();
        if (free > 0) {
            IEnergyStorage src = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(Direction.DOWN), Direction.UP);
            if (src != null && src.canExtract()) {
                int want = Math.min(Math.min(free, IO_PER_TICK), be.energy.getMaxReceive());
                int can  = src.extractEnergy(want, true);
                if (can > 0) {
                    int accepted = be.energy.receiveEnergy(can, false);
                    if (accepted > 0) src.extractEnergy(accepted, false);
                }
            }
        }

        // 2) Push to top + all four sides (skip DOWN)
        int available = Math.min(be.energy.getEnergyStored(), IO_PER_TICK);
        if (available > 0) {
            for (Direction dir : rotatedDirs(level, pos)) {
                if (dir == Direction.DOWN) continue;
                if (available <= 0) break;

                IEnergyStorage sink = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
                if (sink == null || !sink.canReceive()) continue;

                int step       = Math.min(available, be.energy.getMaxExtract());
                int canReceive = sink.receiveEnergy(step, true);
                if (canReceive <= 0) continue;

                int extracted = be.energy.extractEnergy(canReceive, false);
                if (extracted <= 0) continue;

                int accepted = sink.receiveEnergy(extracted, false);
                if (accepted < extracted) {
                    // Put back any unaccepted (rare due to simulate, but safe)
                    be.energy.receiveEnergy(extracted - accepted, false);
                }
                available -= accepted;
            }
        }

        // Optional: toggle a LIT property on the block if you want a glow when >0 FE
        // if (st.hasProperty(BlockStateProperties.LIT)) {
        //     boolean lit = be.energy.getEnergyStored() > 0;
        //     if (st.getValue(BlockStateProperties.LIT) != lit) {
        //         level.setBlock(pos, st.setValue(BlockStateProperties.LIT, lit), 3);
        //     }
        // }
    }

    // ── Persistence ─────────────────────────────────────────────────────
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) energy.setEnergy(tag.getInt("Energy"));
    }

    // ── Utils ───────────────────────────────────────────────────────────
    private static Direction[] rotatedDirs(Level level, BlockPos pos) {
        Direction[] base = Direction.values();
        Direction[] out  = new Direction[base.length];
        int off = Math.floorMod(level.getGameTime() + pos.asLong(), base.length);
        for (int i = 0; i < base.length; i++) out[i] = base[Math.floorMod(i + off, base.length)];
        return out;
    }

    // Per-side wrapper honoring our I/O rules
    private static final class Port implements IEnergyStorage {
        private final EnergyStorageBlockEntity be; private final Direction side;
        Port(EnergyStorageBlockEntity be, Direction side) { this.be = be; this.side = side; }

        private boolean isInput()  { return side == Direction.DOWN; }
        private boolean isOutput() { return side != Direction.DOWN; }

        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            return isInput() ? be.energy.receiveEnergy(maxReceive, simulate) : 0;
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            return isOutput() ? be.energy.extractEnergy(maxExtract, simulate) : 0;
        }
        @Override public int getEnergyStored()    { return be.energy.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return be.energy.getMaxEnergyStored(); }
        @Override public boolean canExtract()     { return isOutput(); }
        @Override public boolean canReceive()     { return isInput(); }
    }
}
