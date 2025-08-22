package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.WoodGasTankBlock;
import net.boulangermod.boulanger.fluid.ModFluids;
import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import org.jetbrains.annotations.Nullable;

public class WoodGasTankBlockEntity extends AbstractProcessingBlockEntity
        implements AbstractProcessingBlock.Tickable {

    // Tunables
    public static final int CAPACITY_MB    = 64000; // 16 buckets
    public static final int PUSH_PER_TICK  = 250;    // mB/t up from the top

    // Store in LOWER only
    private final FluidTank tank = new FluidTank(CAPACITY_MB, fs ->
            fs.getFluid() == ModFluids.WOOD_GAS_STILL.get() || fs.getFluid() == ModFluids.WOOD_GAS_FLOWING.get()
    ) {
        @Override protected void onContentsChanged() { setChangedAndNotify(); }
    };

    public WoodGasTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOODGAS_TANK_BE.get(), pos, state, 0); // 0-slot inventory
    }

    /* ----------------- helpers ----------------- */

    private boolean isUpper() {
        return getBlockState().getValue(WoodGasTankBlock.HALF)
                == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER;
    }

    /** Controller = LOWER BE. UPPER delegates to LOWER for storage. */
    private @Nullable WoodGasTankBlockEntity controller() {
        if (!isUpper()) return this;
        if (level == null) return null;
        var be = level.getBlockEntity(worldPosition.below());
        return (be instanceof WoodGasTankBlockEntity t) ? t : null;
    }

    /* ----------------- side-gated views ----------------- */

    /** Bottom (LOWER, side=DOWN): fill-only view. */
    public IFluidHandler bottomFillOnly() {
        return new IFluidHandler() {
            private WoodGasTankBlockEntity c() { return controller(); }
            @Override public int getTanks() { return 1; }
            @Override public FluidStack getFluidInTank(int tankIdx) { return (c()==null)? FluidStack.EMPTY : c().tank.getFluid(); }
            @Override public int getTankCapacity(int tankIdx) { return CAPACITY_MB; }
            @Override public boolean isFluidValid(int tankIdx, FluidStack stack) { return !stack.isEmpty() && (stack.getFluid()==ModFluids.WOOD_GAS_STILL.get() || stack.getFluid()==ModFluids.WOOD_GAS_FLOWING.get()); }
            @Override public int fill(FluidStack resource, FluidAction action) { return (c()==null)? 0 : c().tank.fill(resource, action); }
            @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
            @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
        };
    }

    /** Top (UPPER, side=UP): drain-only view. */
    public IFluidHandler topDrainOnly() {
        return new IFluidHandler() {
            private WoodGasTankBlockEntity c() { return controller(); }
            @Override public int getTanks() { return 1; }
            @Override public FluidStack getFluidInTank(int tankIdx) { return (c()==null)? FluidStack.EMPTY : c().tank.getFluid(); }
            @Override public int getTankCapacity(int tankIdx) { return CAPACITY_MB; }
            @Override public boolean isFluidValid(int tankIdx, FluidStack stack) { return false; }
            @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
            @Override public FluidStack drain(FluidStack resource, FluidAction action) { return (c()==null)? FluidStack.EMPTY : c().tank.drain(resource, action); }
            @Override public FluidStack drain(int maxDrain, FluidAction action) { return (c()==null)? FluidStack.EMPTY : c().tank.drain(maxDrain, action); }
        };
    }

    /* ----------------- ticking ----------------- */

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        if (!isUpper()) return; // push only from the top half

        WoodGasTankBlockEntity ctrl = controller();
        if (ctrl == null) return;
        if (ctrl.tank.getFluidAmount() <= 0) return;

        // Push straight up into a neighbor’s DOWN side
        var abovePos = pos.above();
        var dest = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                abovePos, Direction.DOWN);
        if (dest == null) return;

        FluidStack toOffer = ctrl.tank.drain(PUSH_PER_TICK, IFluidHandler.FluidAction.SIMULATE);
        if (toOffer.isEmpty()) return;

        int filled = dest.fill(toOffer, IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            ctrl.tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /* ----------------- NBT (LOWER only) ----------------- */

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        if (!isUpper()) {
            CompoundTag t = new CompoundTag();
            tank.writeToNBT(regs, t);
            tag.put("tank", t);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        if (!isUpper() && tag.contains("tank")) {
            tank.readFromNBT(regs, tag.getCompound("tank"));
        }
    }

    /* ----------------- MenuProvider (not used) ----------------- */

    @Override public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("block.boulanger.woodgas_tank");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        // No UI for the tank; if called by mistake, fail loudly.
        throw new IllegalStateException("WoodGasTank has no menu");
    }

    /* ----------------- exposed for HUD/JEI if needed ----------------- */
    public int getFluidAmount()   { return (controller()==null? 0 : controller().tank.getFluidAmount()); }
    public int getFluidCapacity() { return CAPACITY_MB; }
}
