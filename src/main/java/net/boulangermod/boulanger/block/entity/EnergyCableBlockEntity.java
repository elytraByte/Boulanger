// EnergyCableBlockEntity.java
package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.energy.ModEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.slf4j.Logger;

import javax.annotation.Nullable;

public class EnergyCableBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int CAP = 10_000;
    private static final int TICK_TRANSFER = 500;
    /** Push at most this much to one neighbor per visit; improves fairness for small sinks. */
    private static final int PUSH_STEP = 50;

    /** Local buffer. */
    private final ModEnergyStorage buffer = new ModEnergyStorage(CAP, CAP, CAP) {
        @Override protected void onEnergyChanged() { setChanged(); }
    };

    /** Per-side capability wrappers (for on-demand upstream pulls). */
    private final IEnergyStorage[] sideCaps = new IEnergyStorage[6];

    // Tracks last known connection mask so we only log on change
    private int lastCombinedMask = Integer.MIN_VALUE; // force first-tick log
    private int lastCableMask    = 0;
    private int lastDeviceMask   = 0;

    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CABLE_BE.get(), pos, state);
        for (Direction d : Direction.values()) {
            sideCaps[d.ordinal()] = new CableSideEnergy(this, d);
        }
    }

    /** Exposed via capability registration. */
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (side == null) return buffer; // fallback
        return sideCaps[side.ordinal()];
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            LOGGER.info("[EnergyCable] loaded @ {} in {}", worldPosition, level.dimension().location());
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            LOGGER.info("[EnergyCable] removed @ {}", worldPosition);
        }
        super.setRemoved();
    }

    // ───────────────────────────────── tick ─────────────────────────────────
    public static <T extends BlockEntity> void tick(Level level, BlockPos pos,
                                                    BlockState st, EnergyCableBlockEntity be) {
        if (level.isClientSide()) return;

        // Detect connections and log on change
        int cableMask  = 0;
        int deviceMask = 0;
        for (Direction dir : Direction.values()) {
            BlockPos np = pos.relative(dir);
            BlockEntity nbe = level.getBlockEntity(np);
            if (nbe instanceof EnergyCableBlockEntity) {
                cableMask |= (1 << dir.ordinal());
            } else {
                IEnergyStorage neigh = level.getCapability(
                        Capabilities.EnergyStorage.BLOCK, np, dir.getOpposite());
                if (neigh != null) {
                    deviceMask |= (1 << dir.ordinal());
                }
            }
        }
        int combined = cableMask | deviceMask;
        if (combined != be.lastCombinedMask) {
            be.lastCombinedMask = combined;
            be.lastCableMask    = cableMask;
            be.lastDeviceMask   = deviceMask;

            LOGGER.info("[EnergyCable] {} connections @ {} → total={}, cables={}({}), devices={}({})",
                    (combined == 0 ? "no" : "updated"),
                    pos,
                    Integer.bitCount(combined),
                    Integer.bitCount(cableMask),  dirsToString(cableMask),
                    Integer.bitCount(deviceMask), dirsToString(deviceMask));
        }

        // ───────────────────── Pull phase ─────────────────────
        // Pull from engines/output-only faces AND (NEW) recursively from adjacent cables via their side caps.
        int space = be.buffer.getMaxEnergyStored() - be.buffer.getEnergyStored();
        if (space > 0) {
            int pullBudget = Math.min(space, TICK_TRANSFER);
            for (Direction dir : rotatedDirs(level, pos)) {
                if (pullBudget <= 0) break;

                BlockPos np = pos.relative(dir);
                BlockEntity nbe = level.getBlockEntity(np);

                if (nbe instanceof EnergyCableBlockEntity other) {
                    // Ask the neighbor cable to *serve* us via its side cap facing us.
                    // This lets it transitively pull from further upstream cables/engines.
                    int served = other.sideCaps[dir.getOpposite().ordinal()].extractEnergy(pullBudget, false);
                    if (served > 0) {
                        be.buffer.receiveEnergy(served, false);
                        pullBudget -= served;
                    }
                    continue;
                }

                IEnergyStorage src = level.getCapability(
                        Capabilities.EnergyStorage.BLOCK, np, dir.getOpposite());
                if (src == null) continue;

                boolean canReceiveFromUs = src.receiveEnergy(1, true) > 0;
                boolean canExtractToUs   = src.extractEnergy(1, true) > 0;

                // Only treat as a source if it can't receive (i.e., output-only on this face)
                if (canExtractToUs && !canReceiveFromUs) {
                    int canExtract = src.extractEnergy(pullBudget, true);
                    if (canExtract > 0) {
                        int accepted = be.buffer.receiveEnergy(canExtract, false);
                        if (accepted > 0) {
                            src.extractEnergy(accepted, false);
                            pullBudget -= accepted;
                        }
                    }
                }
            }
        }

        // ───────────────────── Push phase (fair, chunked) ─────────────────────
        int pushBudget = Math.min(be.buffer.getEnergyStored(), TICK_TRANSFER);
        if (pushBudget > 0) {
            // First pass: non-cables (consumers/storage)
            pushBudget -= pushOutFair(level, pos, be, pushBudget, /*allowCables*/ false);
            // Second pass: remaining to cables to propagate along the network
            if (pushBudget > 0) {
                pushBudget -= pushOutFair(level, pos, be, pushBudget, /*allowCables*/ true);
            }
        }

        be.setChanged();
    }

    /**
     * Fair, chunked push: cycles neighbors multiple times, sending up to PUSH_STEP each visit.
     * Prevents a single huge sink from starving small ones (like the Stone Mill inbox).
     */
    private static int pushOutFair(Level level, BlockPos pos, EnergyCableBlockEntity be,
                                   int budget, boolean allowCables) {
        int movedTotal = 0;
        Direction[] order = rotatedDirs(level, pos);

        boolean progress;
        do {
            progress = false;
            for (Direction dir : order) {
                if (budget <= 0) break;

                BlockPos np = pos.relative(dir);
                if (!allowCables && isCable(level, np)) continue;

                IEnergyStorage neigh = level.getCapability(
                        Capabilities.EnergyStorage.BLOCK, np, dir.getOpposite());
                if (neigh == null) continue;

                int step = Math.min(PUSH_STEP, budget);
                int canReceive = neigh.receiveEnergy(step, true);
                if (canReceive <= 0) continue;

                int sent = neigh.receiveEnergy(canReceive, false);
                if (sent > 0) {
                    be.buffer.extractEnergy(sent, false);
                    budget      -= sent;
                    movedTotal  += sent;
                    progress = true;
                }
            }
        } while (progress && budget > 0);

        return movedTotal;
    }

    /**
     * Rotate direction order by world time + position so all sides get serviced fairly.
     * Uses floorMod to avoid negative indices (fixes crash when pos.asLong() is negative).
     */
    private static Direction[] rotatedDirs(Level level, BlockPos pos) {
        Direction[] base = Direction.values();
        int len = base.length; // 6
        Direction[] out  = new Direction[len];

        int offset = Math.floorMod(level.getGameTime() + pos.asLong(), len);

        for (int i = 0; i < len; i++) {
            int idx = Math.floorMod(i + offset, len);
            out[i] = base[idx];
        }
        return out;
    }

    private static boolean isCable(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof EnergyCableBlockEntity;
    }

    private static String dirsToString(int mask) {
        if (mask == 0) return "-";
        StringBuilder sb = new StringBuilder();
        for (Direction d : Direction.values()) {
            if ((mask & (1 << d.ordinal())) != 0) {
                switch (d) {
                    case NORTH -> sb.append('N');
                    case EAST  -> sb.append('E');
                    case SOUTH -> sb.append('S');
                    case WEST  -> sb.append('W');
                    case UP    -> sb.append('U');
                    case DOWN  -> sb.append('D');
                }
            }
        }
        return sb.toString();
    }

    // ──────────────────────── Local/Upstream helpers ────────────────────────
    /** Drain this cable’s LOCAL buffer only (no upstream pulling). */
    int drainLocal(int amount) {
        if (amount <= 0) return 0;
        return buffer.extractEnergy(amount, false);
    }

    /** Try to actively pull from upstream (engines & cables), excluding a side. */
    int activePullFromUpstream(int need, @Nullable Direction exclude) {
        if (need <= 0 || level == null) return 0;

        int remaining = Math.min(need, TICK_TRANSFER);
        for (Direction dir : rotatedDirs(level, worldPosition)) {
            if (remaining <= 0) break;
            if (exclude != null && dir == exclude) continue;

            BlockPos np = worldPosition.relative(dir);
            BlockEntity nbe = level.getBlockEntity(np);

            if (nbe instanceof EnergyCableBlockEntity other) {
                // Ask neighbor cable to serve via its port facing us (recursive chain pull)
                int served = other.sideCaps[dir.getOpposite().ordinal()].extractEnergy(remaining, false);
                if (served > 0) {
                    buffer.receiveEnergy(served, false);
                    remaining -= served;
                }
                continue;
            }

            IEnergyStorage src = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK, np, dir.getOpposite());
            if (src == null) continue;

            boolean canReceiveFromUs = src.receiveEnergy(1, true) > 0;
            boolean canExtractToUs   = src.extractEnergy(1, true) > 0;

            if (canExtractToUs && !canReceiveFromUs) {
                int gotSim = src.extractEnergy(remaining, true);
                if (gotSim > 0) {
                    int pulled = src.extractEnergy(gotSim, false);
                    if (pulled > 0) {
                        buffer.receiveEnergy(pulled, false);
                        remaining -= pulled;
                    }
                }
            }
        }
        return need - remaining;
    }

    // ──────────────────────── Per-side capability ────────────────────────
    private static final class CableSideEnergy implements IEnergyStorage {
        private final EnergyCableBlockEntity be;
        private final Direction side;

        CableSideEnergy(EnergyCableBlockEntity be, Direction side) {
            this.be = be;
            this.side = side;
        }

        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) return 0;
            return simulate
                    ? Math.min(maxReceive, be.buffer.getMaxEnergyStored() - be.buffer.getEnergyStored())
                    : be.buffer.receiveEnergy(maxReceive, false);
        }

        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            if (maxExtract <= 0) return 0;

            if (!simulate) {
                // Actively pull from upstream first (excluding the requester side), then serve.
                be.activePullFromUpstream(maxExtract, side);
                return be.buffer.extractEnergy(maxExtract, false);
            } else {
                // Simulate potential supply = local buffer + what we could pull this tick.
                int potential = Math.min(maxExtract, be.buffer.getEnergyStored());

                if (be.level != null && potential < maxExtract) {
                    int remaining = maxExtract - potential;
                    for (Direction dir : rotatedDirs(be.level, be.worldPosition)) {
                        if (remaining <= 0) break;
                        if (dir == side) continue;

                        BlockPos np = be.worldPosition.relative(dir);
                        BlockEntity nbe = be.level.getBlockEntity(np);

                        if (nbe instanceof EnergyCableBlockEntity other) {
                            // Peek what neighbor could serve from its local buffer right now.
                            int buf = other.buffer.getEnergyStored();
                            int take = Math.min(remaining, buf);
                            potential += take;
                            remaining -= take;
                            continue;
                        }

                        IEnergyStorage src = be.level.getCapability(
                                Capabilities.EnergyStorage.BLOCK, np, dir.getOpposite());
                        if (src == null) continue;
                        boolean canReceiveFromUs = src.receiveEnergy(1, true) > 0;
                        boolean canExtractToUs   = src.extractEnergy(1, true) > 0;
                        if (canExtractToUs && !canReceiveFromUs) {
                            int got = src.extractEnergy(remaining, true);
                            if (got > 0) {
                                potential += got;
                                remaining -= got;
                            }
                        }
                    }
                }
                return Math.min(maxExtract, potential);
            }
        }

        @Override public int getEnergyStored() { return be.buffer.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return be.buffer.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }
    }
}
