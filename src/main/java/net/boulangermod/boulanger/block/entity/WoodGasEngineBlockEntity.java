package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.block.WoodGasEngineBlock;
import net.boulangermod.boulanger.energy.ModEnergyStorage;
import net.boulangermod.boulanger.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class WoodGasEngineBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    // ---- Tuning knobs ----
    // Amount of wood-gas the engine attempts to burn each server tick.
    private static final int DRAIN_MB_PER_TICK   = 25;   // was 25
    // RF generated per tick while running.
    private static final int RF_PER_TICK         = 5;   // was 5 per "cycle"
    // Hysteresis thresholds to avoid on/off flicker.
    private static final int START_THRESHOLD_MB  = 100;  // must reach this to turn on
    private static final int STOP_THRESHOLD_MB   = 50;   // falls below this to turn off
    // Try to "pull" up to this much from neighbors each tick before deciding to run.
    private static final int PULL_PER_TICK_MB    = 250;

    private final FluidTank tank = new FluidTank(1000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == ModFluids.WOOD_GAS_STILL.get()
                    || stack.getFluid() == ModFluids.WOOD_GAS_FLOWING.get();
        }
        @Override
        protected void onContentsChanged() {
            LOGGER.debug("[ICE] onContentsChanged → {} mB wood-gas", getFluidAmount());
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    private final ModEnergyStorage energy = new ModEnergyStorage(1000, RF_PER_TICK, RF_PER_TICK) {
        @Override
        protected void onEnergyChanged() {
            setChanged();
            LOGGER.debug("[ICE] Energy changed: {}/{} RF", getEnergyStored(), getMaxEnergyStored());
        }
    };

    public WoodGasEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOODGAS_ENGINE_BE.get(), pos, state);
    }

    // ---- Cap exposure helpers (your pipeline already queries by block) ----
    public IFluidHandler getFluidHandler(@Nullable Direction side) { return tank; }

    // Extract-only view for neighbors. Internal code still uses `energy`.
    private final IEnergyStorage outOnlyView = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; } // no input
        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            return energy.extractEnergy(maxExtract, simulate);
        }
        @Override public int getEnergyStored()      { return energy.getEnergyStored(); }
        @Override public int getMaxEnergyStored()   { return energy.getMaxEnergyStored(); }
        @Override public boolean canExtract()       { return true; }
        @Override public boolean canReceive()       { return false; }
    };

    private Direction getBackSide() {
        BlockState st = getBlockState();
        Direction facing = Direction.NORTH;
        if (st.hasProperty(WoodGasEngineBlock.FACING)) {
            facing = st.getValue(WoodGasEngineBlock.FACING);
        }
        return facing.getOpposite(); // "back" is opposite the facing/front
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (side == null) return null;                  // no unsided access
        return side == getBackSide() ? outOnlyView : null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WoodGasEngineBlockEntity be) {
        if (level.isClientSide) return;
        be.serverTick(state);
    }

    private void serverTick(BlockState state) {
        // 1) Pull from neighbors first so we evaluate with the freshest supply this tick.
        pullFromNeighbors();

        boolean running = state.getValue(WoodGasEngineBlock.LIT);
        int amt = tank.getFluidAmount();

        // 2) Hysteresis to prevent flapping
        if (!running && amt >= START_THRESHOLD_MB) {
            running = true;
            level.setBlock(worldPosition, state.setValue(WoodGasEngineBlock.LIT, true), 3);
            state = getBlockState(); // refresh local ref
            LOGGER.debug("[ICE] Engine -> RUNNING ({} mB >= start {})", amt, START_THRESHOLD_MB);
        } else if (running && amt <= STOP_THRESHOLD_MB) {
            running = false;
            level.setBlock(worldPosition, state.setValue(WoodGasEngineBlock.LIT, false), 3);
            state = getBlockState();
            LOGGER.debug("[ICE] Engine -> STOPPED ({} mB <= stop {})", amt, STOP_THRESHOLD_MB);
        }

        // 3) If running, burn smoothly each tick
        if (running) {
            int drained = tank.drain(DRAIN_MB_PER_TICK, IFluidHandler.FluidAction.EXECUTE).getAmount();
            if (drained < DRAIN_MB_PER_TICK) {
                // Unexpected starvation: stop and wait for buffer to recover to START_THRESHOLD_MB
                level.setBlock(worldPosition, state.setValue(WoodGasEngineBlock.LIT, false), 3);
                LOGGER.debug("[ICE] Starved: drained {} < {}. Engine stopping.", drained, DRAIN_MB_PER_TICK);
            } else {
                energy.receiveEnergy(RF_PER_TICK, false);
            }
        }

        // 4) Push energy to neighbors
        for (Direction dir : Direction.values()) {
            IEnergyStorage target = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    worldPosition.relative(dir),
                    dir.getOpposite()
            );
            if (target != null) {
                int available = energy.extractEnergy(energy.getEnergyStored(), true);
                if (available > 0) {
                    int sent = target.receiveEnergy(available, false);
                    if (sent > 0) energy.extractEnergy(sent, false);
                }
            }
        }
    }

    /**
     * Pull wood-gas from any neighboring fluid handler, up to PULL_PER_TICK_MB in total.
     * Uses the int-amount drain to accept either still or flowing variants.
     */
    private void pullFromNeighbors() {
        if (level == null) return;
        int room = tank.getCapacity() - tank.getFluidAmount();
        if (room <= 0) return;

        int toPull = Math.min(PULL_PER_TICK_MB, room);
        int pulled = 0;

        for (Direction dir : Direction.values()) {
            if (pulled >= toPull) break;

            IFluidHandler src = level.getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    worldPosition.relative(dir),
                    dir.getOpposite()
            );
            if (src == null) continue;

            int request = Math.min(toPull - pulled, toPull);
            // simulate “any fluid” drain, then filter for our wood-gas
            FluidStack sim = src.drain(request, IFluidHandler.FluidAction.SIMULATE);
            if (sim.isEmpty()) continue;
            if (!tank.isFluidValid(sim)) continue;

            int fit = Math.min(sim.getAmount(), tank.getCapacity() - tank.getFluidAmount());
            if (fit <= 0) break;

            FluidStack drained = src.drain(fit, IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty()) {
                int accepted = tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                pulled += accepted;
            }
        }
    }

    // —— NBT persistence & client sync —— //
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag tankTag = new CompoundTag();
        tank.writeToNBT(registries, tankTag);
        tag.put("ice.tank", tankTag);
        tag.putInt("ice.energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag tankTag = tag.getCompound("ice.tank");
        tank.readFromNBT(registries, tankTag);
        energy.setEnergy(tag.getInt("ice.energy"));
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider prov) {
        return saveWithoutMetadata(prov);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider prov) {
        super.onDataPacket(net, pkt, prov);
    }


}
