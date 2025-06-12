package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class WoodGasPipeBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    // 4 000 mB capacity, transfer callback
    private final FluidTank tank = new FluidTank(4_000, this::onTankChanged);

    public WoodGasPipeBlockEntity(BlockPos pos, BlockState st) {
        super(ModBlockEntities.WOOD_GAS_PIPE_BE.get(), pos, st);
    }

    /** Called whenever the tank’s contents change; return true to allow the change. */
    private boolean onTankChanged(FluidStack ignored) {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return true;
    }

    public FluidTank getTank() {
        return tank;
    }

    /** Ticking logic with neighbor debug, skipping gasifier to prevent backflow */
    public static <T extends BlockEntity> void tickServer(
            Level world, BlockPos pos, BlockState st, WoodGasPipeBlockEntity be
    ) {
        // debug neighbor scan
        LOGGER.debug("[Pipe] neighbors at {}:", pos);
        for (Direction d : Direction.values()) {
            BlockPos np = pos.relative(d);
            IFluidHandler ngh = world.getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    np,
                    d.getOpposite()
            );
            LOGGER.debug("  {} → handler? {}", np, ngh != null);
        }

        int remaining = Math.min(200, be.tank.getFluidAmount());

        // Phase 1: push into external machines
        for (Direction d : Direction.values()) {
            if (remaining <= 0) break;
            BlockPos nbrPos = pos.relative(d);
            // skip pipes and gasifier
            if (world.getBlockEntity(nbrPos) instanceof WoodGasPipeBlockEntity ||
                    world.getBlockEntity(nbrPos) instanceof WoodGasifierBlockEntity) {
                continue;
            }

            IFluidHandler target = world.getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    nbrPos,
                    d.getOpposite()
            );
            if (target == null) continue;

            int toDrain = Math.min(remaining, be.tank.getFluidAmount());
            int filled = target.fill(
                    be.tank.drain(toDrain, IFluidHandler.FluidAction.SIMULATE),
                    IFluidHandler.FluidAction.EXECUTE
            );
            if (filled > 0) {
                be.tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                remaining -= filled;
                LOGGER.debug("[Pipe] pushed {} mB to machine at {}", filled, nbrPos);
            }
        }

        // Phase 2: forward through adjacent pipes, avoid backflow
        for (Direction d : Direction.values()) {
            if (remaining <= 0) break;
            BlockPos nbrPos = pos.relative(d);
            BlockEntity nbrBE = world.getBlockEntity(nbrPos);
            if (!(nbrBE instanceof WoodGasPipeBlockEntity)) continue;

            WoodGasPipeBlockEntity nbrPipe = (WoodGasPipeBlockEntity) nbrBE;
            // only forward if this pipe has more fluid than neighbor
            if (nbrPipe.tank.getFluidAmount() >= be.tank.getFluidAmount()) continue;

            IFluidHandler target = world.getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    nbrPos,
                    d.getOpposite()
            );
            if (target == null) continue;

            int toDrain = Math.min(remaining, be.tank.getFluidAmount());
            int filled = target.fill(
                    be.tank.drain(toDrain, IFluidHandler.FluidAction.SIMULATE),
                    IFluidHandler.FluidAction.EXECUTE
            );
            if (filled > 0) {
                be.tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                remaining -= filled;
                LOGGER.debug("[Pipe] forwarded {} mB to pipe at {}", filled, nbrPos);
            }
        }

        // Final state
        LOGGER.debug("[Pipe] tick @ {}: tank={}/{}mB", pos, be.tank.getFluidAmount(), be.tank.getCapacity());
    }

    // ─── CLIENT–SERVER SYNCHRONIZATION ─────────────────────────────────

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Fluid", tank.writeToNBT(registries, new CompoundTag()));
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("Fluid"));
    }
}