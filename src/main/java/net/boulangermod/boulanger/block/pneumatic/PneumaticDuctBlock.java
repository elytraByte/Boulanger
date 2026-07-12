package net.boulangermod.boulanger.block.pneumatic;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.ModTags;
import net.boulangermod.boulanger.pneumatic.api.IAirEndpoint;
import net.boulangermod.boulanger.pneumatic.blockentity.PneumaticDuctBlockEntity;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.boulangermod.boulanger.pneumatic.network.AirNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class PneumaticDuctBlock extends BaseEntityBlock {

    public static final MapCodec<PneumaticDuctBlock> CODEC = simpleCodec(PneumaticDuctBlock::new);

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public static final EnumProperty<DuctSide> NORTH = EnumProperty.create("north", DuctSide.class);
    public static final EnumProperty<DuctSide> SOUTH = EnumProperty.create("south", DuctSide.class);
    public static final EnumProperty<DuctSide> EAST  = EnumProperty.create("east",  DuctSide.class);
    public static final EnumProperty<DuctSide> WEST  = EnumProperty.create("west",  DuctSide.class);
    public static final EnumProperty<DuctSide> UP    = EnumProperty.create("up",    DuctSide.class);
    public static final EnumProperty<DuctSide> DOWN  = EnumProperty.create("down",  DuctSide.class);

    // True center core (matches a 4x4x4 “core” at the center)
    private static final VoxelShape CORE_SHAPE = box(6, 6, 6, 10, 10, 10);

    // Arms (4x4 cross-section) extending from the core to each face
    private static final VoxelShape ARM_NORTH = box(6, 6, 0, 10, 10, 6);
    private static final VoxelShape ARM_SOUTH = box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape ARM_EAST  = box(10, 6, 6, 16, 10, 10);
    private static final VoxelShape ARM_WEST  = box(0, 6, 6, 6, 10, 10);
    private static final VoxelShape ARM_UP    = box(6, 10, 6, 10, 16, 10);
    private static final VoxelShape ARM_DOWN  = box(6, 0, 6, 10, 6, 10);

    // Convenience: your “horizontal straight” pipe is just west + core + east
    private static final VoxelShape HORIZONTAL_SHAPE = Shapes.or(CORE_SHAPE, ARM_WEST, ARM_EAST);

    private static final VoxelShape[] SHAPES_BY_MASK = new VoxelShape[64];

    static {
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape s = CORE_SHAPE;

            if ((mask & (1 << 0)) != 0) s = Shapes.or(s, ARM_NORTH);
            if ((mask & (1 << 1)) != 0) s = Shapes.or(s, ARM_SOUTH);
            if ((mask & (1 << 2)) != 0) s = Shapes.or(s, ARM_EAST);
            if ((mask & (1 << 3)) != 0) s = Shapes.or(s, ARM_WEST);
            if ((mask & (1 << 4)) != 0) s = Shapes.or(s, ARM_UP);
            if ((mask & (1 << 5)) != 0) s = Shapes.or(s, ARM_DOWN);

            SHAPES_BY_MASK[mask] = s; // (Optional) .optimize()
        }
    }

    private static int shapeMask(BlockState state) {
        int m = 0;
        if (state.getValue(NORTH) != DuctSide.CLOSED) m |= 1 << 0;
        if (state.getValue(SOUTH) != DuctSide.CLOSED) m |= 1 << 1;
        if (state.getValue(EAST)  != DuctSide.CLOSED) m |= 1 << 2;
        if (state.getValue(WEST)  != DuctSide.CLOSED) m |= 1 << 3;
        if (state.getValue(UP)    != DuctSide.CLOSED) m |= 1 << 4;
        if (state.getValue(DOWN)  != DuctSide.CLOSED) m |= 1 << 5;
        return m;
    }

    public PneumaticDuctBlock(Properties properties) {
        super(properties);

        // Default axis X => open E/W, closed others
        BlockState base = this.stateDefinition.any()
                .setValue(AXIS, Direction.Axis.X)
                .setValue(EAST,  DuctSide.OPEN)
                .setValue(WEST,  DuctSide.OPEN)
                .setValue(NORTH, DuctSide.CLOSED)
                .setValue(SOUTH, DuctSide.CLOSED)
                .setValue(UP,    DuctSide.CLOSED)
                .setValue(DOWN,  DuctSide.CLOSED);

        this.registerDefaultState(base);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    private static VoxelShape shapeFor(BlockState state) {
        return SHAPES_BY_MASK[shapeMask(state)];
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AXIS, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection(); // player facing
        Direction.Axis axis = (facing.getAxis() == Direction.Axis.Z) ? Direction.Axis.X : Direction.Axis.Z;

        BlockState s = defaultBlockState().setValue(AXIS, axis);

        // Reset faces to defaults for that axis
        for (Direction d : Direction.values()) {
            s = s.setValue(propFor(d), defaultSideFor(s, d));
        }
        return s;
    }

    public static boolean isDuct(BlockState state) {
        return state.getBlock() instanceof PneumaticDuctBlock;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction changedSide, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {

        // Recompute ALL sides whenever any neighbor changes.
        // This prevents "stale OPEN ends" from sticking around and creating unwanted T-shapes.
        BlockState out = state;

        for (Direction d : Direction.values()) {
            EnumProperty<DuctSide> prop = propFor(d);
            DuctSide cur = out.getValue(prop);

            // Flanged faces are locked by the player.
            if (cur == DuctSide.FLANGED) continue;

            DuctSide next = computeSideState(out, level, pos, d, pos.relative(d));
            if (next != cur) {
                out = out.setValue(prop, next);
            }
        }

        // If we have 2+ real connections, close any remaining OPEN faces so elbows don't
        // become accidental T-junctions.
        out = normalizeExtraOpens(out);

        if (out != state && level instanceof Level lvl && !lvl.isClientSide) {
            notifyTopology(lvl, pos);
        }

        return out;
    }

    private static BlockState normalizeExtraOpens(BlockState s) {
        int connected = 0;
        for (Direction d : Direction.values()) {
            if (s.getValue(propFor(d)) == DuctSide.CONNECTED) connected++;
        }

        if (connected >= 2) {
            for (Direction d : Direction.values()) {
                EnumProperty<DuctSide> p = propFor(d);
                if (s.getValue(p) == DuctSide.OPEN) {
                    s = s.setValue(p, DuctSide.CLOSED);
                }
            }
        }
        return s;
    }

    private static DuctSide computeSideState(BlockState self, LevelAccessor level, BlockPos pos,
                                             Direction side, BlockPos neighborPos) {

        // If neighbor is connectable (duct or endpoint) AND neighbor isn't blocking its opposite side, we connect.
        if (isConnectableNeighbor(level, neighborPos)) {
            if (isNeighborBlockingOpposite(level, neighborPos, side.getOpposite())) {
                return defaultSideFor(self, side);
            }
            return DuctSide.CONNECTED;
        }

        // Otherwise revert to default for this face (open on axis ends, closed elsewhere).
        return defaultSideFor(self, side);
    }

    private static boolean isConnectableNeighbor(LevelAccessor level, BlockPos neighborPos) {
        BlockState ns = level.getBlockState(neighborPos);
        Block nb = ns.getBlock();

        // Duct-to-duct (includes ValveDuctBlock / OneWayValveDuctBlock because they extend PneumaticDuctBlock)
        if (nb instanceof PneumaticDuctBlock) return true;

        // Data-driven endpoint detection (client-safe; no BE required)
        if (ns.is(ModTags.Blocks.PNEUMATIC_CONNECTABLE)) return true;

        // Fallback: BE-based endpoints (useful for future endpoints you forget to tag, or dev/test blocks)
        if (level instanceof Level lvl) {
            BlockEntity be = lvl.getBlockEntity(neighborPos);
            return be instanceof IAirEndpoint;
        }

        return false;
    }

    private static boolean isNeighborBlockingOpposite(LevelAccessor level, BlockPos neighborPos, Direction neighborFace) {
        BlockState ns = level.getBlockState(neighborPos);
        if (!(ns.getBlock() instanceof PneumaticDuctBlock)) return false;

        DuctSide side = ns.getValue(propFor(neighborFace));
        return side == DuctSide.FLANGED;
    }

    private static DuctSide defaultSideFor(BlockState state, Direction side) {
        // vertical faces start closed
        if (side.getAxis().isVertical()) return DuctSide.CLOSED;

        Direction.Axis axis = state.getValue(AXIS);
        return (side.getAxis() == axis) ? DuctSide.OPEN : DuctSide.CLOSED;
    }

    public static EnumProperty<DuctSide> propFor(Direction d) {
        return switch (d) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST  -> EAST;
            case WEST  -> WEST;
            case UP    -> UP;
            case DOWN  -> DOWN;
        };
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Client-only ticker to spawn leak particles; server doesn't need per-tick work here.
        return level.isClientSide
                ? createTickerHelper(type, ModBlockEntities.PNEUMATIC_DUCT_BE.get(), PneumaticDuctBlockEntity::clientTick)
                : null;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PneumaticDuctBlockEntity(pos, state);
    }

    // Optional: remove flange by sneak + empty hand
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (!player.isShiftKeyDown()) return InteractionResult.PASS;

        Direction face = hit.getDirection();
        EnumProperty<DuctSide> prop = propFor(face);
        DuctSide cur = state.getValue(prop);

        if (cur != DuctSide.FLANGED) return InteractionResult.PASS;

        // pop it off: revert to default for that face and drop item
        BlockState out = state.setValue(prop, defaultSideFor(state, face));
        level.setBlock(pos, out, Block.UPDATE_CLIENTS);

        // Drop the flange item back to the player/world.
        // (Assumes the registered item id is "boulanger:blind_flange".)
        var flangeItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("boulanger", "blind_flange"));
        if (flangeItem != net.minecraft.world.item.Items.AIR) {
            popResource(level, pos, new ItemStack(flangeItem));
        }

        notifyTopology(level, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        notifyTopology(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) notifyTopology(level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void notifyTopology(Level level, BlockPos pos) {
        if (level.isClientSide) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PneumaticDuctBlockEntity duct) {
            duct.notifyAirTopologyChanged();
        } else {
            // fallback if BE isn't present yet
            AirNetworkManager.get(level).enqueueTopologyChange(pos);
        }
    }
}
