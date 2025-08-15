package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.fluid.ModFluids;
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

    /* ─────────────────────────── config / constants ─────────────────────────── */
    private static final int TANK_CAPACITY_MB        = 1000;
    private static final int TRANSFER_PER_TICK_MB    = 10;

    /* ──────────────────────────────── tank / state ──────────────────────────── */
    /**
     * Pipe tank:
     * - capacity 4 000 mB
     * - server-side onContentsChanged() triggers a block update → client stays in sync
     * - only accepts wood-gas (change isFluidValid() if you want “any fluid”)
     */
    private final FluidTank tank = new FluidTank(TANK_CAPACITY_MB) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == ModFluids.WOOD_GAS_STILL.get();
        }
    };

    public WoodGasPipeBlockEntity(BlockPos pos, BlockState st) {
        super(ModBlockEntities.WOOD_GAS_PIPE_BE.get(), pos, st);
    }

    /** Accessor used by block for particles & debug. */
    public FluidTank getTank() { return tank; }

    /* ──────────────────────────────── ticking logic ──────────────────────────── */
    /**
     * Server tick:
     * 1) push into neighboring machines (non-pipes, non-gasifiers)
     * 2) forward to adjacent pipes (only if neighbor has less fluid to avoid backflow loops)
     * Also logs a quick neighbor capability scan to help with setup.
     */
    public static <T extends BlockEntity> void tickServer(Level world, BlockPos pos, BlockState st, WoodGasPipeBlockEntity be) {
        // Optional: throttle noisy logs
        // if ((world.getGameTime() & 0x1F) == 0) LOGGER.debug("[Pipe] @{} tank={}/{}", pos, be.tank.getFluidAmount(), be.tank.getCapacity());

        if (be.tank.getFluidAmount() <= 0) return;

        int budget = Math.min(TRANSFER_PER_TICK_MB, be.tank.getFluidAmount());

        /* ── Phase 1: push into non-pipe machines/sinks ───────────────────────── */
        for (Direction d : Direction.values()) {
            if (budget <= 0) break;

            BlockPos npos = pos.relative(d);
            BlockEntity nbe = world.getBlockEntity(npos);
            if (nbe instanceof WoodGasPipeBlockEntity || nbe instanceof WoodGasifierBlockEntity) continue; // don't "push" into pipes or source

            IFluidHandler target = world.getCapability(Capabilities.FluidHandler.BLOCK, npos, d.getOpposite());
            if (target == null) continue;

            int toDrain = Math.min(budget, be.tank.getFluidAmount());
            FluidStack sim = be.tank.drain(toDrain, IFluidHandler.FluidAction.SIMULATE);
            if (sim.isEmpty()) continue;

            int accepted = target.fill(sim, IFluidHandler.FluidAction.EXECUTE);
            if (accepted > 0) {
                be.tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                budget -= accepted;
            }
        }

        /* ── Phase 2: diffusion to adjacent pipes (level out) ──────────────────── */
        if (budget <= 0) return;

        int my = be.tank.getFluidAmount();

        // Collect neighbor pipes + their levels
        class Nbr { final WoodGasPipeBlockEntity pipe; final IFluidHandler handler; final int amt; final Direction dir;
            Nbr(WoodGasPipeBlockEntity p, IFluidHandler h, int a, Direction d){ pipe=p; handler=h; amt=a; dir=d; } }

        java.util.ArrayList<Nbr> nbrs = new java.util.ArrayList<>(6);
        for (Direction d : Direction.values()) {
            BlockPos npos = pos.relative(d);
            BlockEntity nbe = world.getBlockEntity(npos);
            if (!(nbe instanceof WoodGasPipeBlockEntity np)) continue;

            IFluidHandler target = world.getCapability(Capabilities.FluidHandler.BLOCK, npos, d.getOpposite());
            if (target == null) continue;

            nbrs.add(new Nbr(np, target, np.getTank().getFluidAmount(), d));
        }
        if (nbrs.isEmpty()) return;

        // Push to emptiest first (reduces bias)
        nbrs.sort(java.util.Comparator.comparingInt(n -> n.amt));

        for (Nbr n : nbrs) {
            if (budget <= 0) break;

            int their = n.amt;
            int diff = my - their;
            if (diff <= 0) continue;               // only push if we have more than the neighbor
            int desired = diff / 2;                // aim to close half the gap
            if (desired <= 0) continue;

            int stepBudget = Math.min(desired, Math.min(budget, TRANSFER_PER_TICK_MB / nbrs.size() + 1));
            stepBudget = Math.min(stepBudget, be.tank.getFluidAmount());
            if (stepBudget <= 0) continue;

            FluidStack sim = be.tank.drain(stepBudget, IFluidHandler.FluidAction.SIMULATE);
            if (sim.isEmpty()) continue;

            int accepted = n.handler.fill(sim, IFluidHandler.FluidAction.EXECUTE);
            if (accepted > 0) {
                be.tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                budget -= accepted;
                my -= accepted;                    // update our local "my" so we don't overshare this tick
            }
        }
    }

    /* ────────────────────────── client–server synchronization ────────────────── */
    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag tag = super.getUpdateTag(regs);
        tag.put("Fluid", tank.writeToNBT(regs, new CompoundTag()));
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider regs) {
        super.handleUpdateTag(tag, regs);
        if (tag.contains("Fluid")) {
            tank.readFromNBT(regs, tag.getCompound("Fluid"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("Fluid", tank.writeToNBT(regs, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        if (tag.contains("Fluid")) {
            tank.readFromNBT(regs, tag.getCompound("Fluid"));
        }
    }
}
