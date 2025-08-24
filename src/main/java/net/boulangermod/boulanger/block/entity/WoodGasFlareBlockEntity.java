package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.block.WoodGasFlareBlock;
import net.boulangermod.boulanger.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class WoodGasFlareBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    /* ─────────── tuning ─────────── */
    private static final int CONSUME_MB_TICK = 1;    // steady draw while lit
    private static final int PULL_CHUNK_MB   = 20;   // burst size when topping up
    private static final int MAX_BUFFER_MB   = 200;  // small local buffer

    /* ─────────── state ─────────── */
    private int  buffer;  // mB of wood gas
    private boolean lit;

    public WoodGasFlareBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOODGAS_FLARE_BE.get(), pos, state);
    }

    /* ─────────── Capability (receive-only tank on attach face) ─────────── */

    private final IFluidHandler sidedHandler = new IFluidHandler() {
        @Override public int getTanks() { return 1; }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank != 0 || buffer <= 0) return FluidStack.EMPTY;
            // Represent buffer using still variant
            return new FluidStack(ModFluids.WOOD_GAS_STILL.get(), buffer);
        }

        @Override public int getTankCapacity(int tank) { return MAX_BUFFER_MB; }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && !stack.isEmpty() && isWoodGasFluid(stack.getFluid());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
            int space = MAX_BUFFER_MB - buffer;
            if (space <= 0) return 0;
            int accepted = Math.min(space, resource.getAmount());
            if (action.execute()) {
                buffer += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    /** Expose handler ONLY on the face that touches the pipe (attach face). */
    public @Nullable IFluidHandler getFluidHandler(@Nullable Direction side) {
        Direction attach = getBlockState().getValue(WoodGasFlareBlock.FACING).getOpposite();
        return (side == null || side == attach) ? sidedHandler : null;
    }

    // 1) Helper: accept your fluids OR the tag (covers early-tag race)
    private static boolean isWoodGasFluid(net.minecraft.world.level.material.Fluid f) {
        return f == net.boulangermod.boulanger.fluid.ModFluids.WOOD_GAS_STILL.get()
                || f == net.boulangermod.boulanger.fluid.ModFluids.WOOD_GAS_FLOWING.get()
                || f.is(net.boulangermod.boulanger.util.ModTags.WOOD_GAS);
    }

    /* ─────────── Server tick ─────────── */
    public static void tick(Level level, BlockPos pos, BlockState state, WoodGasFlareBlockEntity be) {

        if (level == null || level.isClientSide) return;

        // Pull wood gas from the pipe *behind* the flare (opposite of nozzle direction)
        Direction nozzle = state.getValue(WoodGasFlareBlock.FACING);
        BlockPos  pipePos = pos.relative(nozzle.getOpposite());
        Direction sideOnPipeFacingFlare = nozzle; // side *of the pipe* that faces this flare

        IFluidHandler neighbor = level.getCapability(Capabilities.FluidHandler.BLOCK, pipePos, sideOnPipeFacingFlare);

        if (neighbor != null && be.buffer <= (MAX_BUFFER_MB - PULL_CHUNK_MB)) {
            // Probe by amount; validate with tag, then drain exact fluid type we probed
            FluidStack sim = neighbor.drain(PULL_CHUNK_MB, IFluidHandler.FluidAction.SIMULATE);
            if (!sim.isEmpty() && isWoodGasFluid(sim.getFluid())) {
                int toDrain = Math.min(PULL_CHUNK_MB, MAX_BUFFER_MB - be.buffer);
                FluidStack drained = neighbor.drain(new FluidStack(sim.getFluid(), toDrain),
                        IFluidHandler.FluidAction.EXECUTE);
                if (!drained.isEmpty()) {
                    be.buffer = Math.min(MAX_BUFFER_MB, be.buffer + drained.getAmount());
                }
            }
        }

        boolean couldBurn = be.buffer >= CONSUME_MB_TICK && be.pipeStillValid(level, state, pos);
        if (couldBurn) {
            be.buffer -= CONSUME_MB_TICK;
        }
        be.setLit(level, state, pos, couldBurn);
    }

    private boolean pipeStillValid(Level level, BlockState state, BlockPos pos) {
        Direction attach = state.getValue(WoodGasFlareBlock.FACING).getOpposite();
        BlockState neighbor = level.getBlockState(pos.relative(attach));
        return neighbor.is(net.boulangermod.boulanger.block.ModBlocks.WOODGAS_PIPE.get())
                || neighbor.is(net.boulangermod.boulanger.block.ModBlocks.FEED_THROUGH_BLOCK.get());
    }

    private void setLit(Level level, BlockState state, BlockPos pos, boolean newLit) {
        if (this.lit == newLit) return;
        this.lit = newLit;
        level.setBlock(pos, state.setValue(WoodGasFlareBlock.LIT, newLit), 3);
        setChanged();
        if (LOGGER.isDebugEnabled()) LOGGER.debug("[Flare] LIT {}", newLit);
    }

    /* ─────────── Persistence ─────────── */

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("buffer", buffer);
        tag.putBoolean("lit", lit);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer = tag.getInt("buffer");
        lit    = tag.getBoolean("lit");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[Flare] Loaded (buffer={}, lit={})", buffer, lit);
        }
    }
}
