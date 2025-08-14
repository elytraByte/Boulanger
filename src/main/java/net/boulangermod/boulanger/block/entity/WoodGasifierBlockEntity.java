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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

public class WoodGasifierBlockEntity extends AbstractProcessingBlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DBG = true;
    private static void dbg(String fmt, Object... args) { if (DBG) LOGGER.info("[WoodGasifierBE] " + fmt, args); }

    /* -------------------------- inventory & processing -------------------------- */
    private static final int SPLIT_SLOT = 0;
    private static final int LOG_SLOT   = 1;
    private static final int BURN_TIME_PER_LOG = 300;
    private static final int WOOD_GAS_PER_COOK = 1000;

    /* ------------------------------ multiblock core ----------------------------- */
    /** Actual size (X×Y×Z): 2 × 2 × 1 */
    private static final BlockPos SIZE = new BlockPos(2, 2, 1);

    /** When formed, all parts store the same MIN corner of the 2×2×1 volume. */
    private @Nullable BlockPos anchorPos;

    public boolean isFormed() { return anchorPos != null; }
    public boolean isAnchor() { return anchorPos != null && worldPosition.equals(anchorPos); }
    public BlockPos getMinCorner() { return isFormed() ? anchorPos : worldPosition; }

    /* --------------------------------- runtime --------------------------------- */
    private int burnTime = 0;
    private final FluidTank woodGasTank = new FluidTank(8_000) {
        @Override protected void onContentsChanged() { setChanged(); }
    };


    public WoodGasifierBlockEntity(BlockPos pos, BlockState st) {
        super(ModBlockEntities.WOOD_GASIFIER_BE.get(), pos, st, 4);
    }

    public static int getBurnTimePerLog() { return BURN_TIME_PER_LOG; }

    /* ------------------------ rendering helper (used by BER) ------------------- */

    /** Public so the renderer can ask for the full multiblock bounds if needed. */
    public AABB getFootprintAABBForRender() {
        if (level == null || !isFormed() || anchorPos == null) {
            return new AABB(worldPosition); // 1 block around this BE
        }
        Direction facing = getBlockState().hasProperty(WoodGasifierBlock.FACING)
                ? getBlockState().getValue(WoodGasifierBlock.FACING)
                : Direction.NORTH;

        BlockPos min = anchorPos;
        BlockPos max = anchorPos;
        for (BlockPos p : footprintFromMin(anchorPos, facing)) {
            min = new BlockPos(Math.min(min.getX(), p.getX()),
                    Math.min(min.getY(), p.getY()),
                    Math.min(min.getZ(), p.getZ()));
            max = new BlockPos(Math.max(max.getX(), p.getX()),
                    Math.max(max.getY(), p.getY()),
                    Math.max(max.getZ(), p.getZ()));
        }
        // AABB here expects Vec3; make max-exclusive by offsetting +1.
        Vec3 vMin = Vec3.atLowerCornerOf(min);
        Vec3 vMax = Vec3.atLowerCornerOf(max.offset(1, 1, 1));
        return new AABB(vMin, vMax);
    }

    /* ------------------------------- geometry utils ---------------------------- */

    /** Rotate a local offset (x,z) by the given facing around Y (N = identity). Y passes through. */
    public static BlockPos rotateOffset(BlockPos local, Direction face) {
        int x = local.getX(), y = local.getY(), z = local.getZ();
        return switch (face) {
            case EAST  -> new BlockPos(-z, y,  x);
            case SOUTH -> new BlockPos(-x, y, -z);
            case WEST  -> new BlockPos( z, y, -x);
            default    -> local; // NORTH
        };
    }

    /** All world positions in the 2×2×1 volume, using facing's right/forward basis. */
    public static java.util.List<BlockPos> footprintFromMin(BlockPos min, Direction facing) {
        java.util.ArrayList<BlockPos> list = new java.util.ArrayList<>(SIZE.getX()*SIZE.getY()*SIZE.getZ());
        Direction right = facing.getClockWise();      // "width" axis (2)
        Direction fwd   = facing;                     // "depth" axis (1)
        for (int lx = 0; lx < SIZE.getX(); lx++) {
            for (int ly = 0; ly < SIZE.getY(); ly++) {
                for (int lz = 0; lz < SIZE.getZ(); lz++) {
                    BlockPos p = min.relative(right, lx).relative(fwd, lz).above(ly);
                    list.add(p);
                }
            }
        }
        return list;
    }

    /** Check that every position in the rotated 2×2×1 is a WoodGasifier block. */
    public static boolean matchesFootprint(Level level, BlockPos min, Direction facing) {
        int idx = 0;
        for (BlockPos p : footprintFromMin(min, facing)) {
            BlockState st = level.getBlockState(p);
            if (!(st.getBlock() instanceof WoodGasifierBlock)) {
                dbg("matchesFootprint FAIL at cell#{} pos={} state={}", idx, p, st);
                return false;
            }
            idx++;
        }
        return true;
    }

    /** 3×3×3 probe for nearby gasifier cells (helps when resolving from arbitrary part). */
    private static List<BlockPos> findNearbyGasifiers(Level level, BlockPos origin) {
        java.util.ArrayList<BlockPos> list = new java.util.ArrayList<>();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (level.getBlockState(p).getBlock() instanceof WoodGasifierBlock) list.add(p);
                }
            }
        }
        return list;
    }

    /**
     * From ANY of the 4 parts (2×2×1), resolve the MIN (bottom-left-rear)
     * corner in world coords using that part’s facing.
     */
    /** Resolve the anchor MIN corner from any of the 4 cells, using the same basis. */
    public static BlockPos resolveAnchor(Level level, BlockPos anyPos) {
        BlockState clicked = level.getBlockState(anyPos);
        Direction facing = clicked.hasProperty(WoodGasifierBlock.FACING)
                ? clicked.getValue(WoodGasifierBlock.FACING)
                : Direction.NORTH;

        Direction right = facing.getClockWise();
        Direction fwd   = facing;

        // Try every local (lx,ly,lz) to back out the min corner that would include anyPos
        for (int lx = 0; lx < SIZE.getX(); lx++) {
            for (int ly = 0; ly < SIZE.getY(); ly++) {
                for (int lz = 0; lz < SIZE.getZ(); lz++) {
                    BlockPos min = anyPos.relative(right, -lx).relative(fwd, -lz).below(ly);
                    if (matchesFootprint(level, min, facing)) return min;
                }
            }
        }

        // Fallback: probe 3×3×3 neighbors and repeat
        for (BlockPos q : findNearbyGasifiers(level, anyPos)) {
            for (int lx = 0; lx < SIZE.getX(); lx++) {
                for (int ly = 0; ly < SIZE.getY(); ly++) {
                    for (int lz = 0; lz < SIZE.getZ(); lz++) {
                        BlockPos min = q.relative(right, -lx).relative(fwd, -lz).below(ly);
                        if (matchesFootprint(level, min, facing)) return min;
                    }
                }
            }
        }
        return anyPos; // PASS
    }

    public WoodGasifierBlockEntity getAnchorBE() {
        if (level == null || !isFormed()) return null;
        var be = level.getBlockEntity(anchorPos);
        return be instanceof WoodGasifierBlockEntity g ? g : null;
    }

    public void dismantleMultiblock() {
        if (level != null && isFormed()) setFormedAt(anchorPos, false);
    }


    /* ----------------------------- state propagation --------------------------- */

    private void applyHiddenToFootprint(boolean hidden) {
        if (level == null || anchorPos == null) return;
        Direction facing = getBlockState().hasProperty(WoodGasifierBlock.FACING)
                ? getBlockState().getValue(WoodGasifierBlock.FACING)
                : Direction.NORTH;

        dbg("applyHiddenToFootprint hidden={} min={} facing={}", hidden, anchorPos, facing);
        for (BlockPos p : footprintFromMin(anchorPos, facing)) {
            BlockState st = level.getBlockState(p);
            if (!(st.getBlock() instanceof WoodGasifierBlock)) continue;

            BlockState upd = st;
            if (st.hasProperty(WoodGasifierBlock.HIDDEN)) upd = upd.setValue(WoodGasifierBlock.HIDDEN, hidden);
            if (st.hasProperty(WoodGasifierBlock.FORMED)) upd = upd.setValue(WoodGasifierBlock.FORMED, hidden);

            if (upd != st) {
                level.setBlock(p, upd, 3);
                level.sendBlockUpdated(p, st, upd, 3);
                dbg("  updated HIDDEN/FORMED at {} -> {}", p, upd);
            }
        }
    }

    private void setFormedAt(BlockPos anchor, boolean formed) {
        if (level == null) return;
        Direction facing = getBlockState().hasProperty(WoodGasifierBlock.FACING)
                ? getBlockState().getValue(WoodGasifierBlock.FACING)
                : Direction.NORTH;
        dbg("setFormedAt formed={} anchor={} facing={}", formed, anchor, facing);

        for (BlockPos p : footprintFromMin(anchor, facing)) {
            BlockState st = level.getBlockState(p);
            if (!(st.getBlock() instanceof WoodGasifierBlock)) continue;

            BlockState upd = st;
            if (upd.hasProperty(WoodGasifierBlock.FACING)) upd = upd.setValue(WoodGasifierBlock.FACING, facing);
            if (upd.hasProperty(WoodGasifierBlock.HIDDEN))  upd = upd.setValue(WoodGasifierBlock.HIDDEN,  formed);
            if (upd.hasProperty(WoodGasifierBlock.FORMED))  upd = upd.setValue(WoodGasifierBlock.FORMED,  formed);
            if (upd != st) level.setBlock(p, upd, 3);

            var other = level.getBlockEntity(p);
            if (other instanceof WoodGasifierBlockEntity o) {
                o.anchorPos = formed ? anchor : null;
                o.setChanged();
                level.sendBlockUpdated(p, upd, upd, 3);
                dbg("  wrote anchorPos={} into BE at {}", o.anchorPos, p);
            } else {
                dbg("  no BE at {} (expected WoodGasifierBlockEntity)", p);
            }
        }

        this.anchorPos = formed ? anchor : null;
        setChanged();
        dbg("setFormedAt done; this.anchorPos={}", this.anchorPos);
    }

    /* --------------------------------- toggling -------------------------------- */

    public InteractionResult tryToggleForm(Player player, InteractionHand hand, Direction faceClicked) {
        if (level == null || level.isClientSide) return InteractionResult.SUCCESS;

        dbg("tryToggleForm at {} formed={} anchorPos={} hand={} faceClicked={}",
                worldPosition, isFormed(), anchorPos, hand, faceClicked);

        BlockPos anchor = resolveAnchor(level, worldPosition);
        Direction facing = getBlockState().hasProperty(WoodGasifierBlock.FACING)
                ? getBlockState().getValue(WoodGasifierBlock.FACING)
                : Direction.NORTH;
        dbg("  resolved anchor={} using facing={}", anchor, facing);

        if (!isFormed()) {
            if (!matchesFootprint(level, anchor, facing)) {
                dbg("  footprint mismatch (rot) at anchor={}, PASS", anchor);
                return InteractionResult.PASS;
            }
            setFormedAt(anchor, true);
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("Gasifier FORMED at " + anchor.toShortString()));
            return InteractionResult.CONSUME;
        } else {
            setFormedAt(anchorPos != null ? anchorPos : anchor, false);
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("Gasifier DISMANTLED"));
            return InteractionResult.CONSUME;
        }
    }

    /* --------------------------- processing / ticking --------------------------- */

    @Override
    public BlockEntityType<?> getType() { return ModBlockEntities.WOOD_GASIFIER_BE.get(); }

    public static void tick(Level level, BlockPos pos, BlockState state, WoodGasifierBlockEntity be) {
        if (level.isClientSide || !be.isFormed() || !be.isAnchor()) return;
        if ((level.getGameTime() & 15L) == 0L) dbg("tick @{} (anchor) burnTime={}", pos, be.burnTime);
        be.performCookingTick();
    }

    private void performCookingTick() {
        if (burnTime <= 0 && canCook()) {
            dbg("performCookingTick: starting cook");
            doCook();
            burnTime = BURN_TIME_PER_LOG;
        }
        if (burnTime > 0) burnTime--;
    }

    private boolean canCook() {
        IItemHandler items = getItemHandler();
        ItemStack split = items.getStackInSlot(SPLIT_SLOT);
        ItemStack log   = items.getStackInSlot(LOG_SLOT);
        boolean validLog = log.is(Items.OAK_LOG) || log.is(Items.BIRCH_LOG) || log.is(Items.SPRUCE_LOG);
        boolean ok = validLog && split.getItem() == ModItems.SPLIT_PINE_LOGS.get() && split.getCount() > 0;
        dbg("canCook split={}x{} log={} -> {}", split.getCount(), split.getItem(), log.getItem(), ok);
        return ok;
    }

    private void doCook() {
        IItemHandler items = getItemHandler();
        items.extractItem(SPLIT_SLOT, 1, false);
        items.extractItem(LOG_SLOT,   1, false);
        woodGasTank.fill(new FluidStack(ModFluids.WOOD_GAS_STILL.get(), WOOD_GAS_PER_COOK),
                IFluidHandler.FluidAction.EXECUTE);
        setChanged();
        dbg("doCook: produced {}mb, tank now {}", WOOD_GAS_PER_COOK, woodGasTank.getFluidAmount());
    }

    /* ----------------------------------- NBT ----------------------------------- */

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        if (anchorPos != null) tag.putLong("anchorPos", anchorPos.asLong());
        tag.putInt("burnTime", burnTime);
        tag.put("tank", woodGasTank.writeToNBT(regs, new CompoundTag()));
        dbg("saveAdditional anchorPos={} burnTime={} tank={}mb", anchorPos, burnTime, woodGasTank.getFluidAmount());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        anchorPos = tag.contains("anchorPos") ? BlockPos.of(tag.getLong("anchorPos")) : null;
        burnTime = tag.getInt("burnTime");
        if (tag.contains("tank")) woodGasTank.readFromNBT(regs, tag.getCompound("tank"));
        dbg("loadAdditional anchorPos={} burnTime={} tank={}mb", anchorPos, burnTime, woodGasTank.getFluidAmount());
    }
    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.boulanger.wood_gasifier");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        // server-side path: give the BE directly + the data that should sync to the client
        return new WoodGasifierMenu(id, inv, this, createSyncData());
    }

    private net.minecraft.world.inventory.SimpleContainerData createSyncData() {
        // adjust the size if you sync more ints later
        return new net.minecraft.world.inventory.SimpleContainerData(2) {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> WoodGasifierBlockEntity.this.burnTime;
                    case 1 -> WoodGasifierBlockEntity.this.woodGasTank.getFluidAmount();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> WoodGasifierBlockEntity.this.burnTime = value;
                    case 1 -> {
                        var fs = WoodGasifierBlockEntity.this.woodGasTank.getFluid();
                        WoodGasifierBlockEntity.this.woodGasTank.setFluid(fs.copyWithAmount(value));
                    }
                }
            }

            @Override
            public int getCount() { return 2; }
        };
    }
}
