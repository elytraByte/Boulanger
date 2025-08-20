package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.block.WoodGasifierBlock;
import net.boulangermod.boulanger.fluid.ModFluids;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.screen.WoodGasifierMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

public class WoodGasifierBlockEntity extends AbstractProcessingBlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Flip once if your model's visual “front” is 180° off from the blockstate FACING. */
    private static final boolean FLIP_MODEL_FRONT = true;

    /* ────────────────────────── inventory & processing ────────────────────────── */
    private static final int SPLIT_SLOT   = 0;  // split pine logs
    private static final int LOG_SLOT     = 1;  // any vanilla or modded log (ItemTags.LOGS)
    private static final int FILTER_A     = 2;  // filter canister #1
    private static final int FILTER_B     = 3;  // filter canister #2

    private static final int BURN_TIME_PER_LOG = 200;   // In ticks
    private static final int WOOD_GAS_PER_COOK = 3750; // mB

    private static boolean isAnyLog(ItemStack s) { return s.is(net.minecraft.tags.ItemTags.LOGS); }
    private boolean isUsableFilter(ItemStack s) {
        return !s.isEmpty() && (!s.isDamageableItem() || s.getDamageValue() < s.getMaxDamage());
    }
    private boolean haveBothFilters() {
        IItemHandler items = getItemHandler(null);
        return isUsableFilter(items.getStackInSlot(FILTER_A)) && isUsableFilter(items.getStackInSlot(FILTER_B));
    }

    /* ─────────────────────────────── multiblock core ──────────────────────────── */
    /** Footprint size (x×y×z). */
    private static final BlockPos SIZE = new BlockPos(2, 2, 1);

    /** All parts store the same MIN corner (the anchor position). */
    private @Nullable BlockPos anchorPos;

    public boolean isFormed() { return anchorPos != null; }
    public boolean isAnchor() { return anchorPos != null && worldPosition.equals(anchorPos); }
    public BlockPos getMinCorner() { return isFormed() ? anchorPos : worldPosition; }

    /* ────────────────────────────────── runtime ───────────────────────────────── */
    /** Counts DOWN while a single log “burns”. */
    private int burnTime = 0;

    /** Private tank; we expose a drain-only view on the port face. */
    private final FluidTank woodGasTank = new FluidTank(8_000) {
        @Override protected void onContentsChanged() { setChanged(); }
    };

    /** Drain-only view used by capability exposure on the port face. */
    private final IFluidHandler portDrainView = new IFluidHandler() {
        @Override public int getTanks() { return woodGasTank.getTanks(); }
        @Override public FluidStack getFluidInTank(int tank) { return woodGasTank.getFluidInTank(tank); }
        @Override public int getTankCapacity(int tank) { return woodGasTank.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return woodGasTank.isFluidValid(tank, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return 0; } // output-only port
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return woodGasTank.drain(resource, action); }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return woodGasTank.drain(maxDrain, action); }
    };

    /** Data synced to the Screen via the Menu.
     *  0=burnElapsed, 1=energyStored, 2=maxEnergy, 3=gasAmount, 4=gasCapacity */
    private final ContainerData syncData = new ContainerData() {
        @Override public int get(int idx) {
            return switch (idx) {
                case 0 -> getBurnElapsed();         // elapsed ticks 0..BURN_TIME_PER_LOG
                case 1 -> 0;                        // energy stored (gasifier has none)
                case 2 -> 0;                        // max energy
                case 3 -> woodGasTank.getFluidAmount();
                case 4 -> woodGasTank.getCapacity();
                default -> 0;
            };
        }
        @Override public void set(int idx, int val) {
            // Client copy only; keep harmless. If someone writes burnElapsed, invert back to remaining.
            if (idx == 0) {
                int clamped = Math.max(0, Math.min(BURN_TIME_PER_LOG, val));
                burnTime = Math.max(0, BURN_TIME_PER_LOG - clamped);
            } else if (idx == 3) {
                // allow client copy to reflect value locally without changing type
                if (!woodGasTank.getFluid().isEmpty()) {
                    woodGasTank.setFluid(woodGasTank.getFluid().copyWithAmount(val));
                }
            }
        }
        @Override public int getCount() { return 5; }
    };

    public WoodGasifierBlockEntity(BlockPos pos, BlockState st) {
        super(ModBlockEntities.WOOD_GASIFIER_BE.get(), pos, st, 4);
    }

    public static int getBurnTimePerLog() { return BURN_TIME_PER_LOG; }

    /** Burn elapsed = how much of the current log we’ve consumed (0..BURN_TIME_PER_LOG). */
    private int getBurnElapsed() {
        if (burnTime <= 0) return 0;
        return Math.max(0, Math.min(BURN_TIME_PER_LOG, BURN_TIME_PER_LOG - burnTime));
    }

    /* ─────────────────────── rendering helper (used by BER) ───────────────────── */
    public AABB getFootprintAABBForRender() {
        if (level == null || !isFormed() || anchorPos == null) return new AABB(worldPosition);
        Direction facing = getBlockState().hasProperty(WoodGasifierBlock.FACING)
                ? getBlockState().getValue(WoodGasifierBlock.FACING) : Direction.NORTH;

        BlockPos min = anchorPos, max = anchorPos;
        for (BlockPos p : footprintFromMin(anchorPos, facing)) {
            min = new BlockPos(Math.min(min.getX(), p.getX()), Math.min(min.getY(), p.getY()), Math.min(min.getZ(), p.getZ()));
            max = new BlockPos(Math.max(max.getX(), p.getX()), Math.max(max.getY(), p.getY()), Math.max(max.getZ(), p.getZ()));
        }
        return new AABB(Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max.offset(1, 1, 1)));
    }

    /* ───────────────────────────── geometry utilities ─────────────────────────── */
    public static java.util.List<BlockPos> footprintFromMin(BlockPos min, Direction facing) {
        java.util.ArrayList<BlockPos> list = new java.util.ArrayList<>(SIZE.getX() * SIZE.getY() * SIZE.getZ());
        Direction right = facing.getClockWise();
        Direction fwd   = facing;
        for (int lx = 0; lx < SIZE.getX(); lx++) {
            for (int ly = 0; ly < SIZE.getY(); ly++) {
                for (int lz = 0; lz < SIZE.getZ(); lz++) {
                    list.add(min.relative(right, lx).relative(fwd, lz).above(ly));
                }
            }
        }
        return list;
    }

    public static boolean matchesFootprint(Level level, BlockPos min, Direction facing) {
        int idx = 0;
        for (BlockPos p : footprintFromMin(min, facing)) {
            BlockState st = level.getBlockState(p);
            if (!(st.getBlock() instanceof WoodGasifierBlock)) {
                return false;
            }
            idx++;
        }
        return true;
    }


    /** True if the given world position lies inside this gasifier’s current footprint. */
    public boolean isInFootprint(BlockPos test) {
        if (!isFormed() || anchorPos == null) return false;
        Direction facing = getBlockState().hasProperty(WoodGasifierBlock.FACING)
                ? getBlockState().getValue(WoodGasifierBlock.FACING) : Direction.NORTH;
        for (BlockPos p : footprintFromMin(anchorPos, facing)) if (p.equals(test)) return true;
        return false;
    }

    /** Log the external port placement to make pipe placement trivial. */
    private void debugPortLayout() {
        BlockPos anchor = (anchorPos != null) ? anchorPos : worldPosition;
        Direction facing = getBlockState().getValue(WoodGasifierBlock.FACING);
        Direction port   = getPortSide();

        BlockPos portCell = anchor; BlockPos external = null;
        for (BlockPos cell : footprintFromMin(anchor, facing)) {
            BlockPos n = cell.relative(port);
            if (!isInFootprint(n)) { portCell = cell; external = n; break; }
        }
        if (external == null) external = anchor.relative(port);
    }

    private static List<BlockPos> findNearbyGasifiers(Level level, BlockPos origin) {
        java.util.ArrayList<BlockPos> list = new java.util.ArrayList<>();
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (level.getBlockState(p).getBlock() instanceof WoodGasifierBlock) list.add(p);
                }
        return list;
    }

    public static BlockPos resolveAnchor(Level level, BlockPos anyPos) {
        BlockState clicked = level.getBlockState(anyPos);
        Direction facing = clicked.hasProperty(WoodGasifierBlock.FACING)
                ? clicked.getValue(WoodGasifierBlock.FACING) : Direction.NORTH;
        Direction right = facing.getClockWise(), fwd = facing;

        for (int lx = 0; lx < SIZE.getX(); lx++)
            for (int ly = 0; ly < SIZE.getY(); ly++)
                for (int lz = 0; lz < SIZE.getZ(); lz++) {
                    BlockPos min = anyPos.relative(right, -lx).relative(fwd, -lz).below(ly);
                    if (matchesFootprint(level, min, facing)) return min;
                }

        for (BlockPos q : findNearbyGasifiers(level, anyPos))
            for (int lx = 0; lx < SIZE.getX(); lx++)
                for (int ly = 0; ly < SIZE.getY(); ly++)
                    for (int lz = 0; lz < SIZE.getZ(); lz++) {
                        BlockPos min = q.relative(right, -lx).relative(fwd, -lz).below(ly);
                        if (matchesFootprint(level, min, facing)) return min;
                    }
        return anyPos;
    }

    public WoodGasifierBlockEntity getAnchorBE() {
        if (level == null || !isFormed()) return null;
        var be = level.getBlockEntity(anchorPos);
        return be instanceof WoodGasifierBlockEntity g ? g : null;
    }

    public void dismantleMultiblock() {
        if (level != null && isFormed()) setFormedAt(anchorPos, false);
    }

    /* ───────────────────────────── orientation helpers ────────────────────────── */
    /** Front in model space (already applying the global flip). */
    public Direction getFrontFacing() {
        Direction front = getBlockState().getValue(WoodGasifierBlock.FACING);
        return FLIP_MODEL_FRONT ? front.getOpposite() : front;
    }

    /** Port = left of front (after optional flip). */
    public Direction getPortSide() {
        Direction port = getFrontFacing().getClockWise();
        LogUtils.getLogger().info("[Gasifier] facing={} (flip={}) -> port={}",
                getBlockState().getValue(WoodGasifierBlock.FACING), FLIP_MODEL_FRONT, port);
        return port;
    }

    /** Drain-only fluid handler exposed on the port face. */
    public IFluidHandler getPortFluidHandler() { return portDrainView; }

    /** External neighbor next to the *port face*. Chooses a cell whose port side is outside. */
    private BlockPos getPortNeighborPos() {
        BlockPos anchor = (anchorPos != null) ? anchorPos : worldPosition;
        Direction facing = getBlockState().getValue(WoodGasifierBlock.FACING);
        Direction port   = getPortSide();

        for (BlockPos cell : footprintFromMin(anchor, facing)) {
            BlockPos neighbor = cell.relative(port);
            if (!isInFootprint(neighbor)) return neighbor; // outside → valid pipe position
        }
        // Fallback (shouldn’t happen): anchor’s port neighbor
        return anchor.relative(port);
    }

    private void setFormedAt(BlockPos anchor, boolean forming) {
        if (level == null) return;

        Direction facing = getBlockState().hasProperty(WoodGasifierBlock.FACING)
                ? getBlockState().getValue(WoodGasifierBlock.FACING) : Direction.NORTH;

        var cells = footprintFromMin(anchor, facing);

        // If your model renders one block to the RIGHT of the anchor,
        // make THAT cell the visible one:
        Direction right = facing.getClockWise();
        BlockPos visibleCell = anchor.relative(right); // ← try this first
        // If it ends up on the wrong side, switch to counterclockwise:
        // BlockPos visibleCell = anchor.relative(right.getOpposite());

        for (BlockPos p : cells) {
            BlockState st = level.getBlockState(p);
            if (!(st.getBlock() instanceof WoodGasifierBlock)) continue;

            BlockState upd = st;
            if (upd.hasProperty(WoodGasifierBlock.FACING)) upd = upd.setValue(WoodGasifierBlock.FACING, facing);

            if (forming) {
                boolean isVisible = p.equals(visibleCell);
                // Only the visible cell shows the big model
                if (upd.hasProperty(WoodGasifierBlock.FORMED)) upd = upd.setValue(WoodGasifierBlock.FORMED, isVisible);
                if (upd.hasProperty(WoodGasifierBlock.HIDDEN)) upd = upd.setValue(WoodGasifierBlock.HIDDEN, !isVisible);
            } else {
                if (upd.hasProperty(WoodGasifierBlock.FORMED)) upd = upd.setValue(WoodGasifierBlock.FORMED, false);
                if (upd.hasProperty(WoodGasifierBlock.HIDDEN)) upd = upd.setValue(WoodGasifierBlock.HIDDEN, false);
            }

            if (upd != st) level.setBlock(p, upd, 3);

            var other = level.getBlockEntity(p);
            if (other instanceof WoodGasifierBlockEntity o) {
                o.anchorPos = forming ? anchor : null; // keep logical anchor unchanged
                o.setChanged();
                level.sendBlockUpdated(p, upd, upd, 3);
            }
            level.invalidateCapabilities(p);
        }

        this.anchorPos = forming ? anchor : null;
        setChanged();
    }

    public net.minecraft.world.InteractionResult tryToggleForm(
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand,
            @org.jetbrains.annotations.Nullable net.minecraft.core.Direction faceClicked
    ) {
        if (level == null || level.isClientSide) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        // Resolve the true anchor for the current structure
        BlockPos anchor = resolveAnchor(level, worldPosition);
        Direction facing = getBlockState().hasProperty(WoodGasifierBlock.FACING)
                ? getBlockState().getValue(WoodGasifierBlock.FACING)
                : Direction.NORTH;



        if (!isFormed()) {
            if (!matchesFootprint(level, anchor, facing)) {

                return net.minecraft.world.InteractionResult.PASS;
            }
            setFormedAt(anchor, true);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Gasifier FORMED at " + anchor.toShortString()));
            }
            return net.minecraft.world.InteractionResult.CONSUME;
        } else {
            setFormedAt(anchorPos != null ? anchorPos : anchor, false);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("Gasifier DISMANTLED"));
            }
            return net.minecraft.world.InteractionResult.CONSUME;
        }
    }

    /* ───────────────────────────── processing / ticking ───────────────────────── */
    @Override
    public BlockEntityType<?> getType() { return ModBlockEntities.WOOD_GASIFIER_BE.get(); }

    /** Server tick — gated to ANCHOR ONLY. */
    public static void tick(Level level, BlockPos pos, BlockState state, WoodGasifierBlockEntity be) {
        if (level.isClientSide) return;
        if (!be.isFormed() || !be.isAnchor()) return;
        be.performCookingTick();
    }

    private void performCookingTick() {
        if (burnTime <= 0 && canCook()) {
            doCook();
            burnTime = BURN_TIME_PER_LOG;
        }
        if (burnTime > 0) burnTime--;

        // Push gas toward the external port neighbor.
        pushFluidOut();

        updateLitFlag();
    }

    private boolean canCook() {
        IItemHandler items = getItemHandler(null);
        ItemStack split = items.getStackInSlot(SPLIT_SLOT);
        ItemStack log   = items.getStackInSlot(LOG_SLOT);

        boolean okSplit   = !split.isEmpty() && split.getItem() == ModItems.SPLIT_PINE_LOGS.get();
        boolean okLog     = isAnyLog(log);
        boolean okFilters = haveBothFilters();

        if (!(okSplit && okLog && okFilters)) {
            return false;
        }

        FluidStack toMake = new FluidStack(ModFluids.WOOD_GAS_STILL.get(), WOOD_GAS_PER_COOK);
        int canFill = woodGasTank.fill(toMake, IFluidHandler.FluidAction.SIMULATE);
        boolean fits = (canFill == WOOD_GAS_PER_COOK);

        return fits;
    }

    private void doCook() {
        IItemHandler items = getItemHandler(null);

        items.extractItem(SPLIT_SLOT, 1, false);
        items.extractItem(LOG_SLOT,   1, false);

        damageOrConsumeFilter(FILTER_A, 1);
        damageOrConsumeFilter(FILTER_B, 1);

        woodGasTank.fill(new FluidStack(ModFluids.WOOD_GAS_STILL.get(), WOOD_GAS_PER_COOK),
                IFluidHandler.FluidAction.EXECUTE);

        setChanged();
    }

    private void damageOrConsumeFilter(int slot, int dmg) {
        IItemHandler items = getItemHandler(null);
        ItemStack stack = items.extractItem(slot, 1, false);
        if (stack.isEmpty()) return;

        if (stack.isDamageableItem()) {
            int newDamage = stack.getDamageValue() + dmg;
            if (newDamage < stack.getMaxDamage()) {
                stack.setDamageValue(newDamage);
                items.insertItem(slot, stack, false);
            }
        }
        // else: nondurable → consumed
    }

    private void pushFluidOut() {
        if (level == null || woodGasTank.getFluidAmount() <= 0) return;

        Direction port = getPortSide();
        BlockPos outPos = getPortNeighborPos();              // external neighbor
        Direction neighborFace = port.getOpposite();

        IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, outPos, neighborFace);
        int toSend = Math.min(200, woodGasTank.getFluidAmount());
        int moved  = target.fill(woodGasTank.drain(toSend, IFluidHandler.FluidAction.SIMULATE),
                IFluidHandler.FluidAction.EXECUTE);
        if (moved > 0) woodGasTank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
    }

    /* ─────────────────────────────────── NBT ──────────────────────────────────── */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        if (anchorPos != null) tag.putLong("anchorPos", anchorPos.asLong());
        tag.putInt("burnTime", burnTime);
        tag.put("tank", woodGasTank.writeToNBT(regs, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        anchorPos = tag.contains("anchorPos") ? BlockPos.of(tag.getLong("anchorPos")) : null;
        burnTime  = tag.getInt("burnTime");
        if (tag.contains("tank")) woodGasTank.readFromNBT(regs, tag.getCompound("tank"));
    }

    /* ─────────────────────────────── UI / menus ──────────────────────────────── */
    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.boulanger.wood_gasifier");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        // Bridge our ContainerData to the Menu’s SimpleContainerData type
        return new WoodGasifierMenu(id, inv, this, new SimpleContainerData(5) {
            @Override public int get(int i) { return syncData.get(i); }
            @Override public void set(int i, int v) { syncData.set(i, v); }
            @Override public int getCount() { return syncData.getCount(); }
        });
    }

    private void updateLitFlag() {
        if (level == null || !isAnchor()) return;
        boolean lit = (burnTime > 0) || (woodGasTank.getFluidAmount() > 0);

        BlockState st = level.getBlockState(worldPosition);
        if (st.hasProperty(WoodGasifierBlock.LIT) && st.getValue(WoodGasifierBlock.LIT) != lit) {
            level.setBlock(worldPosition, st.setValue(WoodGasifierBlock.LIT, lit), 3);
        }
    }
}
