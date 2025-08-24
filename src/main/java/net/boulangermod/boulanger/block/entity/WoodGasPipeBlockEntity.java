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

import java.util.ArrayList;
import java.util.Comparator;

public class WoodGasPipeBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int TANK_CAPACITY_MB     = 1000;
    private static final int TRANSFER_PER_TICK_MB = 100;

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
            // Accept anything that matches our custom FluidType (covers still & flowing)
            return stack.getFluid().getFluidType() == ModFluids.WOOD_GAS_TYPE.get();
        }
    };

    public WoodGasPipeBlockEntity(BlockPos pos, BlockState st) {
        super(ModBlockEntities.WOOD_GAS_PIPE_BE.get(), pos, st);
    }

    public FluidTank getTank() { return tank; }

    /**
     * Server tick order changed to:
     * 1) Diffuse to adjacent pipes FIRST (using neighbor tanks directly; always moves >= 1 mB).
     * 2) Then push to non-pipe machines (uses block capability, 4-arg overload).
     *
     * This prevents long chains from “starving” because nearby sinks consume the entire budget.
     * It also avoids capability edge-cases between pipes by talking directly tank→tank.
     */
    public static <T extends BlockEntity> void tickServer(Level world, BlockPos pos, BlockState st, WoodGasPipeBlockEntity be) {
        int have = be.tank.getFluidAmount();
        if (have <= 0) return;

        int budget = Math.min(TRANSFER_PER_TICK_MB, have);

        /* ─────────── Phase 1: Diffusion to adjacent pipes (direct tank access) ─────────── */
        class Nbr {
            final WoodGasPipeBlockEntity pipe;
            final FluidTank target;
            final int amt;
            final Direction dir;
            Nbr(WoodGasPipeBlockEntity p, FluidTank t, int a, Direction d) { pipe = p; target = t; amt = a; dir = d; }
        }

        ArrayList<Nbr> nbrs = new ArrayList<>(6);
        for (Direction d : Direction.values()) {
            BlockPos npos = pos.relative(d);
            BlockEntity nbe = world.getBlockEntity(npos);
            if (!(nbe instanceof WoodGasPipeBlockEntity np)) continue;
            FluidTank theirTank = np.getTank();
            nbrs.add(new Nbr(np, theirTank, theirTank.getFluidAmount(), d));
        }

        if (!nbrs.isEmpty()) {
            // Emptiest first reduces bias in lines
            nbrs.sort(Comparator.comparingInt(n -> n.amt));

            // Ceil-divide for a fair per-neighbor cap; never 0
            int maxPerNbr = Math.max(1, (TRANSFER_PER_TICK_MB + nbrs.size() - 1) / Math.max(1, nbrs.size()));

            int my = be.tank.getFluidAmount();
            for (Nbr n : nbrs) {
                if (budget <= 0) break;

                int their = n.amt;
                int diff = my - their;
                if (diff <= 0) continue;

                // Round UP so a 1 mB difference still moves 1 mB
                int desired = (diff + 1) / 2;

                int stepBudget = Math.min(desired, Math.min(budget, maxPerNbr));
                stepBudget = Math.min(stepBudget, be.tank.getFluidAmount());
                if (stepBudget <= 0) continue;

                // Simulate drain from us
                FluidStack sim = be.tank.drain(stepBudget, IFluidHandler.FluidAction.SIMULATE);
                if (sim.isEmpty()) continue;

                // Execute fill directly into neighbor tank
                int accepted = n.target.fill(sim, IFluidHandler.FluidAction.EXECUTE);
                if (accepted > 0) {
                    be.tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                    budget -= accepted;
                    my     -= accepted;
                }
            }
        }

        /* ─────────── Phase 2: Push remaining into non-pipe machines/sinks ─────────── */
        if (budget > 0 && be.tank.getFluidAmount() > 0) {
            for (Direction d : Direction.values()) {
                if (budget <= 0) break;

                BlockPos npos = pos.relative(d);
                BlockEntity nbe = world.getBlockEntity(npos);
                if (nbe instanceof WoodGasPipeBlockEntity || nbe instanceof WoodGasifierBlockEntity) continue; // skip pipes and source

                BlockState ns = world.getBlockState(npos);
                IFluidHandler target = world.getCapability(
                        Capabilities.FluidHandler.BLOCK, npos, ns, nbe, d.getOpposite()
                );
                if (target == null) continue;

                int toDrain = Math.min(budget, be.tank.getFluidAmount());
                if (toDrain <= 0) break;

                FluidStack sim = be.tank.drain(toDrain, IFluidHandler.FluidAction.SIMULATE);
                if (sim.isEmpty()) continue;

                int accepted = target.fill(sim, IFluidHandler.FluidAction.EXECUTE);
                if (accepted > 0) {
                    be.tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                    budget -= accepted;
                }
            }
        }
    }

    // ───────── client/server sync ─────────
    @Override @Nullable
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
