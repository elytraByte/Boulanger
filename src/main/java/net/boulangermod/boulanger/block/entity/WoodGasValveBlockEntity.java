package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.block.WoodGasValveBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.slf4j.Logger;

import javax.annotation.Nullable;

public class WoodGasValveBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int CAPACITY = 250; // mB
    public static final int MAX_IO = 50;  // mB/t

    private final FluidTank tank = new FluidTank(CAPACITY);

    // NOTE:
    // If your registry field is a DeferredHolder, keep `.get()`.
    // If it's a plain BlockEntityType<...>, drop `.get()` and pass the field directly.
    public WoodGasValveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOODGAS_VALVE_BE.get(), pos, state);
    }

    // Capability exposure (wired via RegisterCapabilitiesEvent)
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        boolean open = getBlockState().getValue(WoodGasValveBlock.OPEN);
        if (!open) {
            // Block all IO when closed
            return new IFluidHandler() {
                @Override
                public int getTanks() {
                    return 1;
                }

                @Override
                public FluidStack getFluidInTank(int tank) {
                    return WoodGasValveBlockEntity.this.tank.getFluid();
                }

                @Override
                public int getTankCapacity(int tank) {
                    return CAPACITY;
                }

                @Override
                public boolean isFluidValid(int tank, FluidStack stack) {
                    return WoodGasValveBlockEntity.this.tank.isFluidValid(stack);
                }

                @Override
                public int fill(FluidStack resource, FluidAction action) {
                    return 0;
                }

                @Override
                public FluidStack drain(FluidStack resource, FluidAction action) {
                    return FluidStack.EMPTY;
                }

                @Override
                public FluidStack drain(int maxDrain, FluidAction action) {
                    return FluidStack.EMPTY;
                }
            };
        }
        return tank;
    }

    // Server tick: equalize along the inline axis when OPEN
    public static void serverTick(Level level, BlockPos pos, BlockState state, WoodGasValveBlockEntity be) {
        if (level.isClientSide) return;
        if (!state.getValue(WoodGasValveBlock.OPEN)) return;

        Direction axis = state.getValue(WoodGasValveBlock.FACING);
        be.pushTo(level, pos, axis);
        be.pushTo(level, pos, axis.getOpposite());
        be.pullFrom(level, pos, axis);
        be.pullFrom(level, pos, axis.getOpposite());
    }

    private void pushTo(Level lvl, BlockPos pos, Direction dir) {
        if (tank.getFluidAmount() <= 0) return;

        BlockPos np = pos.relative(dir);
        BlockState ns = lvl.getBlockState(np);
        BlockEntity nbe = lvl.getBlockEntity(np);

        IFluidHandler neighbor = lvl.getCapability(Capabilities.FluidHandler.BLOCK, np, ns, nbe, dir.getOpposite());
        if (neighbor == null) return;

        FluidStack toSend = tank.getFluid().copy();
        toSend.setAmount(Math.min(MAX_IO, tank.getFluidAmount()));
        int accepted = neighbor.fill(toSend, IFluidHandler.FluidAction.EXECUTE);
        if (accepted > 0) {
            tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            setDirtyAndSync();
        }
    }

    private void pullFrom(Level lvl, BlockPos pos, Direction dir) {
        if (tank.getSpace() <= 0) return;

        BlockPos np = pos.relative(dir);
        BlockState ns = lvl.getBlockState(np);
        BlockEntity nbe = lvl.getBlockEntity(np);

        IFluidHandler neighbor = lvl.getCapability(Capabilities.FluidHandler.BLOCK, np, ns, nbe, dir.getOpposite());
        if (neighbor == null) return;

        FluidStack pulled = neighbor.drain(Math.min(MAX_IO, tank.getSpace()), IFluidHandler.FluidAction.EXECUTE);
        if (!pulled.isEmpty()) {
            tank.fill(pulled, IFluidHandler.FluidAction.EXECUTE);
            setDirtyAndSync();
        }
    }

    // ── Persist & load ────────────────────────────────────────────────────────────
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);

        CompoundTag tankTag = new CompoundTag();
        tank.writeToNBT(regs, tankTag);      // <— pass regs first
        tag.put("Tank", tankTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);

        if (tag.contains("Tank", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            tank.readFromNBT(regs, tag.getCompound("Tank"));  // <— pass regs first
        } else {
            tank.setFluid(FluidStack.EMPTY); // optional: clear if missing
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag tag = super.getUpdateTag(regs);
        CompoundTag tankTag = new CompoundTag();
        tank.writeToNBT(regs, tankTag);
        tag.put("Tank", tankTag);
        return tag;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // 1.21 signature includes regs:
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider regs) {
        handleUpdateTag(pkt.getTag(), regs); // use the variant that accepts regs
    }

    public void setDirtyAndSync() {
        setChanged(); // mark for saving
        if (level instanceof ServerLevel sl) {
            // notify clients + neighbors that the BE’s data changed
            sl.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            sl.getChunkSource().blockChanged(worldPosition);
        }

    }
}
