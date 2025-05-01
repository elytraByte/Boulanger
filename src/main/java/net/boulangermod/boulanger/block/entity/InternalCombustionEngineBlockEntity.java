package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.energy.ModEnergyStorage;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.block.entity.InternalCombustionEngineBlockEntity;
import net.boulangermod.boulanger.block.entity.WoodGasifierBlockEntity;
import net.boulangermod.boulanger.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.slf4j.Logger;

import javax.annotation.Nullable;

public class InternalCombustionEngineBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int WOOD_GAS_PER_TICK = 1;
    private static final int RF_PER_CYCLE      = 1000;
    private static final int BURN_INTERVAL     = 1;

    // 8,000 mB wood-gas buffer with validator and change callback
    private final FluidTank tank = new FluidTank(8_000) {
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

    // capacity=100_000, maxReceive=RF_PER_CYCLE, maxExtract=RF_PER_CYCLE
    private final ModEnergyStorage energy =
            new ModEnergyStorage(100_000, RF_PER_CYCLE, RF_PER_CYCLE) {
                @Override protected void onEnergyChanged() {
                    setChanged();
                    LOGGER.debug("[ICE] Energy changed: {}/{} RF",
                            getEnergyStored(), getMaxEnergyStored());
                }
            };


    private int burnCooldown = 0;

    public InternalCombustionEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INTERNAL_COMBUSTION_ENGINE_BE.get(), pos, state);
    }

    /** NeoForge will call this when pipes ask for IFluidHandler.BLOCK */
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        LOGGER.debug("[ICE] getFluidHandler(side={}) → {} mB in tank",
                side, tank.getFluidAmount());
        return tank;
    }

    /** NeoForge will call this when neighbors ask for IEnergyStorage.BLOCK */
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        LOGGER.debug("[ICE] getEnergyStorage(side={}) → {}/{} RF",
                side, energy.getEnergyStored(), energy.getMaxEnergyStored());
        return energy;
    }

    public static void tick(Level level, BlockPos pos, BlockState state,
                            InternalCombustionEngineBlockEntity be) {
        if (level.isClientSide) return;
        be.serverTick();
    }

    private void serverTick() {
        LOGGER.debug("[ICE] serverTick start: {} mB wood-gas, cooldown={}",
                tank.getFluidAmount(), burnCooldown);

        if (burnCooldown > 0) {
            burnCooldown--;
        } else if (tank.getFluidAmount() >= WOOD_GAS_PER_TICK) {
            // consume gas and generate RF
            tank.drain(WOOD_GAS_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
            energy.receiveEnergy(RF_PER_CYCLE, false);
            burnCooldown = BURN_INTERVAL;
            LOGGER.info("[ICE] Consumed {} mB wood-gas → produced {} RF; cooldown reset to {}",
                    WOOD_GAS_PER_TICK, RF_PER_CYCLE, burnCooldown);
        } else {
            LOGGER.debug("[ICE] Not enough wood-gas: have {} mB, need {} mB",
                    tank.getFluidAmount(), WOOD_GAS_PER_TICK);
        }

        pushEnergyToNeighbors();
    }

    private void pushEnergyToNeighbors() {
        for (Direction dir : Direction.values()) {
            BlockPos neighbour = worldPosition.relative(dir);
            LOGGER.debug("[ICE] Pushing energy to {} side of {}",
                    dir.getOpposite(), neighbour);

            IEnergyStorage target = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    neighbour,
                    dir.getOpposite()
            );
            if (target != null) {
                int available = energy.extractEnergy(energy.getEnergyStored(), true);
                int sent      = target.receiveEnergy(available, false);
                energy.extractEnergy(sent, false);
                LOGGER.info("[ICE] Pushed {} RF to {} of {}; buffer now {}/{} RF",
                        sent, dir.getOpposite(), neighbour,
                        energy.getEnergyStored(), energy.getMaxEnergyStored());
            } else {
                LOGGER.debug("[ICE] No energy capability on {} side of {}",
                        dir.getOpposite(), neighbour);
            }
        }
    }
}
