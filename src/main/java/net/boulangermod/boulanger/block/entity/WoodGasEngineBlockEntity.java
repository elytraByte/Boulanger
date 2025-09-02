package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.block.WoodGasEngineBlock;
import net.boulangermod.boulanger.energy.ModEnergyStorage;
import net.boulangermod.boulanger.fluid.ModFluids;
import net.boulangermod.boulanger.screen.WoodGasEngineBlockMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class WoodGasEngineBlockEntity extends AbstractProcessingBlockEntity {

    /* ── tuning ─────────────────────────────────────────────────── */
    private static final int TANK_CAP_MB        = 10_000;
    private static final int DRAIN_MB_PER_TICK  = 25;    // gas consumed when running
    private static final int FE_PER_TICK        = 50;     // generation rate
    private static final int START_THRESHOLD_MB = 100;   // start at/above
    private static final int STOP_THRESHOLD_MB  = 50;    // stop at/below
    private static final int PULL_PER_TICK_MB   = 500;   // pull from neighbors per tick

    private static final int FE_CAPACITY        = 1000;
    private static final int FE_MAX_EXTRACT     = 1000; // allow cables to pull generously
    private static final int PER_SIDE_LIMIT     = 1000; // max FE to try per neighbor per tick

    // simple visual cycle for GUI
    private static final int BURN_TOTAL_TICKS   = 200;
    private int burnProgress = 0;
    private int burnTotal    = BURN_TOTAL_TICKS;

    /* ── storage ────────────────────────────────────────────────── */
    private final FluidTank tank = new FluidTank(TANK_CAP_MB) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == ModFluids.WOOD_GAS_STILL.get()
                    || stack.getFluid() == ModFluids.WOOD_GAS_FLOWING.get();
        }
        @Override
        protected void onContentsChanged() {
            setChangedAndNotify();
        }
    };

    // extract-only view (lets neighbors pull FE out; engine never receives FE)
    private final IEnergyStorage outOnlyView = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            return energy.extractEnergy(maxExtract, simulate);
        }
        @Override public int getEnergyStored()    { return energy.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energy.getMaxEnergyStored(); }
        @Override public boolean canExtract()     { return true; }
        @Override public boolean canReceive()     { return false; }
    };

    // maxReceive=0, maxExtract=FE_MAX_EXTRACT so **pullers can extract freely**
    private final ModEnergyStorage energy = new ModEnergyStorage(FE_CAPACITY, 1000, FE_MAX_EXTRACT) {
        @Override protected void onEnergyChanged() {
            setChangedAndNotify();
        }
    };

    public WoodGasEngineBlockEntity(BlockPos pos, BlockState state) {
        // 4th arg = inventory slot count (engine has none)
        super(ModBlockEntities.WOODGAS_ENGINE_BE.get(), pos, state, 0);
    }

    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        // wood-gas input only on the back; unsided queries allowed
        return (side == null || side == getBackSide()) ? tank : null;
    }

    public IEnergyStorage getEnergyForSide(@Nullable Direction side) {
        // no energy output on the back
        if (side != null && side == getBackSide()) return null;
        return outOnlyView;
    }

    /* ── ticking ─────────────────────────────────────────────────── */
    public static void tick(Level level, BlockPos pos, BlockState state, WoodGasEngineBlockEntity be) {
        if (level.isClientSide) return;
        be.serverTick(state);
    }

    private void serverTick(BlockState state) {
        pullFromNeighbors();

        boolean running = state.getValue(WoodGasEngineBlock.LIT);
        int amt = tank.getFluidAmount();

        // hysteresis: decide desired lit state
        boolean shouldRun = running
                ? amt > STOP_THRESHOLD_MB
                : amt >= START_THRESHOLD_MB;

        if (shouldRun != running) {
            running = shouldRun;
            // transition: set blockstate and reset the GUI cycle at start
            level.setBlock(worldPosition, state.setValue(WoodGasEngineBlock.LIT, running), 3);
            if (running) {
                burnProgress = 0;                 // show full flame at cycle start
                burnTotal    = BURN_TOTAL_TICKS;  // ensure sane total
            } else {
                burnProgress = 0;                 // hide instantly (screen gates on isLit)
            }
            setChangedAndNotify();
        }

        if (running) {
            int drained = tank.drain(DRAIN_MB_PER_TICK, IFluidHandler.FluidAction.EXECUTE).getAmount();
            if (drained < DRAIN_MB_PER_TICK) {
                // ran out mid-tick → stop
                level.setBlock(worldPosition, state.setValue(WoodGasEngineBlock.LIT, false), 3);
                burnProgress = 0;
                setChangedAndNotify();
            } else {
                energy.receiveEnergy(FE_PER_TICK, false);
                // loop the progress 0..(total-1)
                burnProgress = (burnProgress + 1) % burnTotal;
            }
        }
        // else: not running; screen won't render lit textures because isLit() is false

        // --- fair push to all output sides (skip back) ---
        int remaining = energy.getEnergyStored();
        if (remaining > 0) {
            Direction back = getBackSide();

            // collect valid output targets first
            Direction[] dirs = Direction.values();
            java.util.ArrayList<Direction> outs = new java.util.ArrayList<>(6);
            for (Direction d : dirs) {
                if (d == back) continue;
                IEnergyStorage t = level.getCapability(
                        Capabilities.EnergyStorage.BLOCK,
                        worldPosition.relative(d),
                        d.getOpposite()
                );
                if (t != null) outs.add(d);
            }

            int targets = outs.size();
            if (targets > 0) {
                // give each side an equal share, capped by PER_SIDE_LIMIT, at least 1 FE
                int per = Math.max(1, Math.min(PER_SIDE_LIMIT, remaining / targets));

                for (Direction d : outs) {
                    if (remaining <= 0) break;

                    IEnergyStorage t = level.getCapability(
                            Capabilities.EnergyStorage.BLOCK,
                            worldPosition.relative(d),
                            d.getOpposite()
                    );
                    if (t == null) continue;

                    int offer = Math.min(per, remaining);
                    int accepted = t.receiveEnergy(offer, false);
                    if (accepted > 0) {
                        energy.extractEnergy(accepted, false);
                        remaining -= accepted;
                    }
                }
            }
        }
    }

    private void pullFromNeighbors() {
        if (level == null) return;
        int room = tank.getCapacity() - tank.getFluidAmount();
        if (room <= 0) return;

        int toPull = Math.min(PULL_PER_TICK_MB, room);
        Direction back = getBackSide();

        IFluidHandler src = level.getCapability(
                Capabilities.FluidHandler.BLOCK,
                worldPosition.relative(back),
                back.getOpposite()
        );
        if (src == null) return;

        FluidStack sim = src.drain(toPull, IFluidHandler.FluidAction.SIMULATE);
        if (sim.isEmpty() || !tank.isFluidValid(sim)) return;

        int fit = Math.min(sim.getAmount(), tank.getCapacity() - tank.getFluidAmount());
        if (fit <= 0) return;

        FluidStack drained = src.drain(fit, IFluidHandler.FluidAction.EXECUTE);
        if (!drained.isEmpty()) tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
    }

    /* ── NBT / sync ──────────────────────────────────────────────── */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs); // base saves inventory (0 slots)
        CompoundTag tankTag = new CompoundTag();
        tank.writeToNBT(regs, tankTag);
        tag.put("ice.tank", tankTag);
        tag.putInt("ice.energy", energy.getEnergyStored());
        tag.putInt("ice.burnProgress", burnProgress);
        tag.putInt("ice.burnTotal",    burnTotal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        if (tag.contains("ice.tank")) tank.readFromNBT(regs, tag.getCompound("ice.tank"));
        energy.setEnergy(tag.getInt("ice.energy"));
        burnProgress = tag.getInt("ice.burnProgress");
        burnTotal    = Math.max(1, tag.getInt("ice.burnTotal"));
        if (burnTotal <= 1) burnTotal = BURN_TOTAL_TICKS;
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        return saveWithoutMetadata(regs);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider regs) {
        super.onDataPacket(net, pkt, regs);
    }

    /* ── MenuProvider (from base) ────────────────────────────────── */
    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.boulanger.woodgas_engine");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new WoodGasEngineBlockMenu(id, inv, this);
    }

    /* ── GUI helpers ─────────────────────────────────────────────── */
    public int getBurnProgress() { return this.burnProgress; }
    public int getBurnTotal()    { return Math.max(this.burnTotal, 1); }
    public int getGasAmount()    { return tank.getFluidAmount(); }
    public int getGasCapacity()  { return Math.max(tank.getCapacity(), 1); }

    /** Authoritative lit state for UI: mirrors blockstate. */
    public boolean isLit() {
        BlockState st = getBlockState();
        return st.hasProperty(WoodGasEngineBlock.LIT) && st.getValue(WoodGasEngineBlock.LIT);
    }

    private Direction getFrontSide() {
        BlockState st = getBlockState();
        return st.hasProperty(WoodGasEngineBlock.FACING)
                ? st.getValue(WoodGasEngineBlock.FACING)
                : Direction.NORTH;
    }

    private Direction getBackSide() {
        return getFrontSide().getOpposite();
    }
}
