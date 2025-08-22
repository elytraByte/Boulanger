package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.EnergyCableBlockEntity;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class EnergyCableBlock extends BaseEntityBlock {

    public static final MapCodec<EnergyCableBlock> CODEC = simpleCodec(EnergyCableBlock::new);

    // Six connection flags (match datagen & models)
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST  = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST  = BooleanProperty.create("west");
    public static final BooleanProperty UP    = BooleanProperty.create("up");
    public static final BooleanProperty DOWN  = BooleanProperty.create("down");

    // ---- slim voxel shapes (center nub + 6 arms), precomputed for 64 states ----
    // tweak these two to match your model thickness (same as WoodGasPipe by default)
    private static final int A = 6;   // inner min (0..16)
    private static final int B = 10;  // inner max (0..16)

    private static final VoxelShape CORE = box(A, A, A, B, B, B);
    private static final VoxelShape ARM_N = box(A, A, 0,  B, B, A);
    private static final VoxelShape ARM_S = box(A, A, B,  B, B, 16);
    private static final VoxelShape ARM_W = box(0,  A, A, A, B, B);
    private static final VoxelShape ARM_E = box(B,  A, A, 16, B, B);
    private static final VoxelShape ARM_U = box(A,  B, A,  B, 16, B);
    private static final VoxelShape ARM_D = box(A,  0, A,  B, A,  B);

    private static final VoxelShape[] SHAPES = new VoxelShape[64];
    static {
        for (int m = 0; m < 64; m++) {
            VoxelShape s = CORE;
            if ((m & 0b000001) != 0) s = Shapes.or(s, ARM_N); // N
            if ((m & 0b000010) != 0) s = Shapes.or(s, ARM_E); // E
            if ((m & 0b000100) != 0) s = Shapes.or(s, ARM_S); // S
            if ((m & 0b001000) != 0) s = Shapes.or(s, ARM_W); // W
            if ((m & 0b010000) != 0) s = Shapes.or(s, ARM_U); // U
            if ((m & 0b100000) != 0) s = Shapes.or(s, ARM_D); // D
            SHAPES[m] = s.optimize();
        }
    }

    private static int mask(BlockState s) {
        int m = 0;
        if (s.getValue(NORTH)) m |= 1;
        if (s.getValue(EAST))  m |= 2;
        if (s.getValue(SOUTH)) m |= 4;
        if (s.getValue(WEST))  m |= 8;
        if (s.getValue(UP))    m |= 16;
        if (s.getValue(DOWN))  m |= 32;
        return m;
    }

    public EnergyCableBlock(Properties props) {
        super(props.noOcclusion()); // thin geometry
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(NORTH, false)
                        .setValue(EAST,  false)
                        .setValue(SOUTH, false)
                        .setValue(WEST,  false)
                        .setValue(UP,    false)
                        .setValue(DOWN,  false)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    // ---------- shapes / lighting ----------
    @Override public boolean useShapeForLightOcclusion(BlockState state) { return true; }
    @Override public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }
    @Override public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 0; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[mask(state)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[mask(state)];
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPES[mask(state)];
    }

    // Hide internal faces when two cables connect (prevents z-fighting seam)
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        if (adjacentState.getBlock() instanceof EnergyCableBlock) {
            boolean a = state.getValue(propFor(side));
            boolean b = adjacentState.getValue(propFor(side.getOpposite()));
            if (a && b) return true;
        }
        return super.skipRendering(state, adjacentState, side);
    }

    /* ---------------- placement / updates ---------------- */

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        return updateConnections(level, pos, this.defaultBlockState());
    }

    @Override
    public BlockState updateShape(BlockState state,
                                  Direction changedSide,
                                  BlockState changedState,
                                  LevelAccessor level,
                                  BlockPos pos,
                                  BlockPos neighborPos) {
        return updateConnections(level, pos, state);
    }

    private BlockState updateConnections(LevelAccessor level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) {
            boolean connect = isConnectable(level, pos.relative(d), d.getOpposite());
            state = state.setValue(propFor(d), connect);
        }
        return state;
    }

    /** Connect to other cables or any face with an Energy capability */
    private boolean isConnectable(LevelAccessor level, BlockPos neighborPos, Direction faceTowardUs) {
        BlockState nb = level.getBlockState(neighborPos);
        if (nb.getBlock() instanceof EnergyCableBlock) return true;

        if (level instanceof Level lvl) {
            IEnergyStorage cap = lvl.getCapability(
                    Capabilities.EnergyStorage.BLOCK, neighborPos, faceTowardUs
            );
            return cap != null && (cap.canReceive() || cap.canExtract());
        }
        return false;
    }

    private static BooleanProperty propFor(Direction d) {
        return switch (d) {
            case NORTH -> NORTH;
            case EAST  -> EAST;
            case SOUTH -> SOUTH;
            case WEST  -> WEST;
            case UP    -> UP;
            case DOWN  -> DOWN;
        };
    }

    /* ---------------- block entity ---------------- */

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                                                                  BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.ENERGY_CABLE_BE.get(), EnergyCableBlockEntity::tick);
    }

    // tiny helper so we can declare boxes with ints
    private static VoxelShape box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return Block.box(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
