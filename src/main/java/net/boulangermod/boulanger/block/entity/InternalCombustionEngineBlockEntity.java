package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.energy.ModEnergyStorage;
import net.boulangermod.boulanger.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
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
import org.slf4j.Logger;
import org.jetbrains.annotations.Nullable;

public class InternalCombustionEngineBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int WOOD_GAS_PER_TICK = 125;
    private static final int RF_PER_CYCLE      = 10;
    private static final int BURN_INTERVAL     = 20;

    // 8,000 mB wood-gas buffer
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

    private final ModEnergyStorage energy = new ModEnergyStorage(100_000, RF_PER_CYCLE, RF_PER_CYCLE) {
        @Override
        protected void onEnergyChanged() {
            setChanged();
            LOGGER.debug("[ICE] Energy changed: {}/{} RF",
                    getEnergyStored(), getMaxEnergyStored());
        }
    };

    private int burnCooldown = 0;

    public InternalCombustionEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INTERNAL_COMBUSTION_ENGINE_BE.get(), pos, state);
    }

    /** Expose fluid‐handler capability */
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return tank;
    }

    /** Expose energy capability */
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return energy;
    }

    public static void tick(Level level, BlockPos pos, BlockState state,
                            InternalCombustionEngineBlockEntity be) {
        if (level.isClientSide) return;
        be.serverTick(state);
    }

    private void serverTick(BlockState state) {
        // toggle LIT property based on whether we have gas
        boolean lit = state.getValue(net.boulangermod.boulanger.block.InternalCombustionEngineBlock.LIT);
        boolean shouldBeLit = tank.getFluidAmount() >= WOOD_GAS_PER_TICK;

        if (lit != shouldBeLit) {
            level.setBlock(worldPosition,
                    state.setValue(
                            net.boulangermod.boulanger.block.InternalCombustionEngineBlock.LIT,
                            shouldBeLit
                    ), 3
            );
        }

        if (burnCooldown > 0) {
            burnCooldown--;
        } else if (tank.getFluidAmount() >= WOOD_GAS_PER_TICK) {
            tank.drain(WOOD_GAS_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
            energy.receiveEnergy(RF_PER_CYCLE, false);
            burnCooldown = BURN_INTERVAL;
            LOGGER.info("[ICE] Consumed {} mB wood-gas → produced {} RF; cooldown reset",
                    WOOD_GAS_PER_TICK, RF_PER_CYCLE);
        }

        // push out energy
        for (Direction dir : Direction.values()) {
            IEnergyStorage target = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    worldPosition.relative(dir),
                    dir.getOpposite()
            );
            if (target != null) {
                int available = energy.extractEnergy(energy.getEnergyStored(), true);
                int sent = target.receiveEnergy(available, false);
                energy.extractEnergy(sent, false);
            }
        }
    }

    // —— NBT persistence & client sync —— //
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // note: provider first, then the tag
        CompoundTag tankTag = new CompoundTag();
        tank.writeToNBT(registries, tankTag);
        tag.put("ice.tank", tankTag);
        tag.putInt("ice.energy", energy.getEnergyStored());
        tag.putInt("ice.burnCooldown", burnCooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // again: provider first
        CompoundTag tankTag = tag.getCompound("ice.tank");
        tank.readFromNBT(registries, tankTag);
        energy.setEnergy(tag.getInt("ice.energy"));
        burnCooldown = tag.getInt("ice.burnCooldown");
    }


    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider prov) {
        return saveWithoutMetadata(prov);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt,
                             net.minecraft.core.HolderLookup.Provider prov) {
        super.onDataPacket(net, pkt, prov);
    }
}
