package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.energy.ModEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public abstract class AbstractPoweredBlockEntity extends AbstractProcessingBlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Per-side IO mode. */
    public enum IOMode { DISABLED, INPUT, OUTPUT, BOTH }

    /** Energy store used by machines that keep an internal battery. */
    protected final ModEnergyStorage energy;

    /** Cost a machine wants to burn per tick when running. */
    protected final int energyPerTick;

    /** Per-side capability wrappers respecting IOMode. */
    private final IEnergyStorage[] sideCaps = new IEnergyStorage[6];

    /** Per-side IO config; defaults to INPUT (typical machine sink). */
    private final IOMode[] sideModes = new IOMode[6];

    /** Optional auto IO knobs (machines can call serverEnergyTick() in their tick). */
    protected boolean autoPullEnabled = true;
    protected boolean autoPushEnabled = false;
    protected int autoPullPerTick;
    protected int autoPushPerTick;

    // ─────────────────────────── ctor ───────────────────────────
    /**
     * Back-compat constructor: sink-style machine (all faces INPUT), auto-pull on, auto-push off.
     */
    protected AbstractPoweredBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state,
            int slotCount, int energyCapacity, int maxReceive, int energyPerTick
    ) {
        this(type, pos, state, slotCount,
                energyCapacity, maxReceive, /*maxExtract*/ Math.max(1, energyPerTick), // allow internal drain
                energyPerTick,
                IOMode.INPUT, true, false,
                /*autoPullPerTick*/ Math.max(0, maxReceive), /*autoPushPerTick*/ 0);
    }

    /**
     * Full-control constructor.
     */
    protected AbstractPoweredBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int slotCount,
            int energyCapacity,
            int maxReceive,
            int maxExtract,
            int energyPerTick,
            IOMode defaultSideMode,
            boolean autoPullEnabled,
            boolean autoPushEnabled,
            int autoPullPerTick,
            int autoPushPerTick
    ) {
        super(type, pos, state, slotCount);
        this.energyPerTick = energyPerTick;

        // Initialize modes & side caps
        for (Direction d : Direction.values()) {
            sideModes[d.ordinal()] = defaultSideMode;
        }

        this.energy = new ModEnergyStorage(energyCapacity, maxReceive, maxExtract) {
            @Override protected void onEnergyChanged() {
                setChangedAndNotify();
            }
        };

        for (Direction d : Direction.values()) {
            sideCaps[d.ordinal()] = new PortEnergy(this, d);
        }

        this.autoPullEnabled = autoPullEnabled;
        this.autoPushEnabled = autoPushEnabled;
        this.autoPullPerTick = Math.max(0, autoPullPerTick);
        this.autoPushPerTick = Math.max(0, autoPushPerTick);
    }

    // ─────────────────────────── Capability exposure ───────────────────────────
    /** Expose to ModCapabilities. */
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (side == null) return energy;
        return sideCaps[side.ordinal()];
    }

    public IOMode getSideMode(Direction side) {
        return sideModes[side.ordinal()];
    }

    /** Mutators you can call from a block ctor / wrench UI. */
    public void setSideMode(Direction side, IOMode mode) {
        sideModes[side.ordinal()] = mode;
        setChangedAndNotify();
    }

    public void cycleSideMode(Direction side) {
        IOMode cur = sideModes[side.ordinal()];
        IOMode next = switch (cur) {
            case DISABLED -> IOMode.INPUT;
            case INPUT    -> IOMode.OUTPUT;
            case OUTPUT   -> IOMode.BOTH;
            case BOTH     -> IOMode.DISABLED;
        };
        setSideMode(side, next);
    }

    // ─────────────────────────── Common helpers ───────────────────────────
    /** Machines can call this at the start of their server tick. */
    protected void serverEnergyTick() {
        if (level == null || level.isClientSide) return;

        if (autoPullEnabled) {
            pullEnergyFromNeighbors(autoPullPerTick);
        }
        if (autoPushEnabled) {
            pushEnergyToNeighbors(autoPushPerTick);
        }
    }

    /** Pull up to maxPullPerTick in a fair order; respects neighbor OUTPUT/BOTH but not our side mode (since *we* are pulling). */
    protected int pullEnergyFromNeighbors(int maxPullPerTick) {
        if (level == null || level.isClientSide || maxPullPerTick <= 0) return 0;

        int movedTotal = 0;
        int free = energy.getMaxEnergyStored() - energy.getEnergyStored();
        if (free <= 0) return 0;

        for (Direction dir : rotatedDirs(level, worldPosition)) {
            if (movedTotal >= maxPullPerTick || free <= 0) break;

            var neighborPos = worldPosition.relative(dir);
            IEnergyStorage src = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, dir.getOpposite());
            if (src == null) continue;

            // Only pull from neighbors that can EXTRACT on their face to us.
            if (!src.canExtract()) continue;

            int want = Math.min(Math.min(maxPullPerTick - movedTotal, free), energy.getMaxReceive());
            if (want <= 0) break;

            int canExtract = src.extractEnergy(want, true);
            if (canExtract <= 0) continue;

            int received = energy.receiveEnergy(canExtract, false);
            if (received <= 0) continue;

            int actuallyExtracted = src.extractEnergy(received, false);

            movedTotal += actuallyExtracted;
            free      -= actuallyExtracted;

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[{} @ {}] pulled {} FE from {} (face {}), now {}/{}",
                        getClass().getSimpleName(), worldPosition, actuallyExtracted,
                        neighborPos, dir.getOpposite(), energy.getEnergyStored(), energy.getMaxEnergyStored());
            }
        }
        if (movedTotal > 0) setChangedAndNotify();
        return movedTotal;
    }

    /** Push out up to maxPushPerTick to neighbors that can RECEIVE; obeys our side modes (OUTPUT/BOTH). */
    protected int pushEnergyToNeighbors(int maxPushPerTick) {
        if (level == null || level.isClientSide || maxPushPerTick <= 0) return 0;

        int movedTotal = 0;
        int available = Math.min(maxPushPerTick, energy.getEnergyStored());

        for (Direction dir : rotatedDirs(level, worldPosition)) {
            if (available <= 0) break;

            // Only push via sides configured as OUTPUT/BOTH
            IOMode mode = sideModes[dir.ordinal()];
            if (!(mode == IOMode.OUTPUT || mode == IOMode.BOTH)) continue;

            var neighborPos = worldPosition.relative(dir);
            IEnergyStorage sink = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, dir.getOpposite());
            if (sink == null || !sink.canReceive()) continue;

            int step = Math.min(available, Math.max(1, energy.getMaxExtract()));
            int canReceive = sink.receiveEnergy(step, true);
            if (canReceive <= 0) continue;

            int extracted = energy.extractEnergy(canReceive, false);
            if (extracted <= 0) continue;

            int accepted = sink.receiveEnergy(extracted, false);
            if (accepted < extracted) {
                // Put back any excess we couldn't insert (should be rare with simulate check).
                energy.receiveEnergy(extracted - accepted, false);
            }

            movedTotal += accepted;
            available  -= accepted;
        }
        if (movedTotal > 0) setChangedAndNotify();
        return movedTotal;
    }

    /** True if enough energy to perform one work tick. */
    protected boolean hasPowerForTick() {
        return energyPerTick <= 0 || energy.getEnergyStored() >= energyPerTick;
    }

    /** Try to pay the per-tick cost. */
    protected boolean tryConsumePowerForTick() {
        if (!hasPowerForTick()) return false;
        if (energyPerTick > 0) energy.extractEnergy(energyPerTick, false);
        return true;
    }

    public int getEnergyStored()   { return energy.getEnergyStored(); }
    public int getEnergyCapacity() { return energy.getMaxEnergyStored(); }

    protected void setChangedAndNotify() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ─────────────────────────── Save / load ───────────────────────────
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("IOModes", packIOModes());
        tag.putBoolean("AutoPull", autoPullEnabled);
        tag.putBoolean("AutoPush", autoPushEnabled);
        tag.putInt("AutoPullPT", autoPullPerTick);
        tag.putInt("AutoPushPT", autoPushPerTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) energy.setEnergy(tag.getInt("Energy"));
        if (tag.contains("IOModes")) unpackIOModes(tag.getInt("IOModes"));
        autoPullEnabled = tag.getBoolean("AutoPull");
        autoPushEnabled = tag.getBoolean("AutoPush");
        autoPullPerTick = Math.max(0, tag.getInt("AutoPullPT"));
        autoPushPerTick = Math.max(0, tag.getInt("AutoPushPT"));
    }

    /** Pack 6 sides * 2 bits into an int. */
    private int packIOModes() {
        int val = 0;
        for (Direction d : Direction.values()) {
            int twoBits = switch (sideModes[d.ordinal()]) {
                case DISABLED -> 0;
                case INPUT    -> 1;
                case OUTPUT   -> 2;
                case BOTH     -> 3;
            };
            val |= (twoBits & 0b11) << (d.ordinal() * 2);
        }
        return val;
    }

    private void unpackIOModes(int packed) {
        for (Direction d : Direction.values()) {
            int twoBits = (packed >> (d.ordinal() * 2)) & 0b11;
            sideModes[d.ordinal()] = switch (twoBits) {
                case 0 -> IOMode.DISABLED;
                case 1 -> IOMode.INPUT;
                case 2 -> IOMode.OUTPUT;
                default -> IOMode.BOTH;
            };
        }
    }

    // ─────────────────────────── Utils ───────────────────────────
    protected static Direction[] rotatedDirs(Level level, BlockPos pos) {
        Direction[] base = Direction.values(); // len=6
        Direction[] out  = new Direction[base.length];
        int off = Math.floorMod(level.getGameTime() + pos.asLong(), base.length);
        for (int i = 0; i < base.length; i++) {
            out[i] = base[Math.floorMod(i + off, base.length)];
        }
        return out;
    }

    /** Per-side wrapper that obeys the configured IOMode. */
    private static final class PortEnergy implements IEnergyStorage {
        private final AbstractPoweredBlockEntity be;
        private final Direction side;

        PortEnergy(AbstractPoweredBlockEntity be, Direction side) {
            this.be = be;
            this.side = side;
        }

        private IOMode mode() { return be.sideModes[side.ordinal()]; }

        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            IOMode m = mode();
            if (!(m == IOMode.INPUT || m == IOMode.BOTH)) return 0;
            if (maxReceive <= 0) return 0;
            return be.energy.receiveEnergy(maxReceive, simulate);
        }

        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            IOMode m = mode();
            if (!(m == IOMode.OUTPUT || m == IOMode.BOTH)) return 0;
            if (maxExtract <= 0) return 0;
            return be.energy.extractEnergy(maxExtract, simulate);
        }

        @Override public int getEnergyStored()     { return be.energy.getEnergyStored(); }
        @Override public int getMaxEnergyStored()  { return be.energy.getMaxEnergyStored(); }
        @Override public boolean canExtract()      { IOMode m = mode(); return m == IOMode.OUTPUT || m == IOMode.BOTH; }
        @Override public boolean canReceive()      { IOMode m = mode(); return m == IOMode.INPUT  || m == IOMode.BOTH; }
    }
}
