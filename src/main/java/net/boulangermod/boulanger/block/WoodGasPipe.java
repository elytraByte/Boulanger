package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.block.entity.WoodGasPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class WoodGasPipe extends BaseEntityBlock implements EntityBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;
    public static final BooleanProperty UP    = BlockStateProperties.UP;
    public static final BooleanProperty DOWN  = BlockStateProperties.DOWN;

    private static final int PARTICLE_MIN_MB = 10;

    // ---- slim voxel shapes (center nub + 6 arms), precomputed for 64 states ----
// ---- slim voxel shapes (center nub + 6 arms), precomputed for 64 states ----
// tweak these two to change thickness
    private static final int A = 6;   // inner min (0..16)
    private static final int B = 10;  // inner max (0..16)

    // shift hitbox DOWN by 1px to match the model
    private static final int Y_OFF = 0;

    private static VoxelShape boxY(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int y0 = Math.max(0, minY + Y_OFF);
        int y1 = Math.min(16, maxY + Y_OFF);
        return Block.box(minX, y0, minZ, maxX, y1, maxZ);
    }

    private static final VoxelShape CORE  = boxY(A, A, A, B, B, B);
    private static final VoxelShape ARM_N = boxY(A, A, 0,  B, B, A);
    private static final VoxelShape ARM_S = boxY(A, A, B,  B, B, 16);
    private static final VoxelShape ARM_W = boxY(0,  A, A, A, B, B);
    private static final VoxelShape ARM_E = boxY(B,  A, A, 16, B, B);
    private static final VoxelShape ARM_U = boxY(A,  B, A,  B, 16, B);
    private static final VoxelShape ARM_D = boxY(A,  0, A,  B, A,  B);

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

    public WoodGasPipe(Properties props) {
        super(props
                .noOcclusion()
                .randomTicks()
        );
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST,  false)
                .setValue(SOUTH, false).setValue(WEST,  false)
                .setValue(UP,    false).setValue(DOWN,  false)
        );
    }

    // Tell lighting to use our (non-full) shape instead of a cube
    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    // Let skylight pass and don't block light—kills the “cube shadow”
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    // Selection / outline
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[mask(state)];
    }

    // Collision (you can return SHAPES[...] or Shapes.empty() if you want ghost pipes)
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[mask(state)];
    }

    // Light occlusion/culling shape (non-full shape prevents dark halos on the ground)
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPES[mask(state)];
    }

    // Hide the internal faces where two pipes connect on that side (removes z-fighting lines)
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        if (adjacentState.getBlock() instanceof WoodGasPipe) {
            boolean a = state.getValue(prop(side));
            boolean b = adjacentState.getValue(prop(side.getOpposite()));
            if (a && b) return true;
        }
        return super.skipRendering(state, adjacentState, side);
    }

    private static BooleanProperty prop(Direction d) {
        return switch (d) {
            case NORTH -> NORTH;
            case EAST  -> EAST;
            case SOUTH -> SOUTH;
            case WEST  -> WEST;
            case UP    -> UP;
            case DOWN  -> DOWN;
        };
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null; // keep as-is if you’re not using map codecs for this block
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    // ----- CONNECTIVITY (updated) -----

    // checks a specific direction and neighbor face
    private static boolean connects(LevelAccessor level, BlockPos pos, Direction dir) {
        BlockPos adjPos = pos.relative(dir);
        BlockState adj  = level.getBlockState(adjPos);

        // 1) Connect to other pipes
        if (adj.getBlock() instanceof WoodGasPipe) return true;

        // 2) Connect to any block exposing a fluid handler on the face toward this pipe
        BlockEntity be = level.getBlockEntity(adjPos);
        if (level instanceof Level lvl) {
            var cap = lvl.getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    adjPos, adj, be, dir.getOpposite()
            );
            if (cap != null) return true;
        }

        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level w = ctx.getLevel();
        BlockPos p = ctx.getClickedPos();
        return defaultBlockState()
                .setValue(NORTH, connects(w, p, Direction.NORTH))
                .setValue(EAST,  connects(w, p, Direction.EAST))
                .setValue(SOUTH, connects(w, p, Direction.SOUTH))
                .setValue(WEST,  connects(w, p, Direction.WEST))
                .setValue(UP,    connects(w, p, Direction.UP))
                .setValue(DOWN,  connects(w, p, Direction.DOWN));
    }

    @Override
    public BlockState updateShape(BlockState s, Direction dir, BlockState neighbor,
                                  LevelAccessor w, BlockPos pos, BlockPos neighborPos) {
        return s.setValue(prop(dir), connects(w, pos, dir));
    }

    @Override
    public RenderShape getRenderShape(BlockState st) {
        return RenderShape.MODEL;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof WoodGasPipeBlockEntity pipe)) return;

        int amount = pipe.getTank().getFluidAmount();
        if (amount < PARTICLE_MIN_MB) return; // only render smoke if we have at least 10 mB

        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        double a = random.nextDouble() * Math.PI * 2;
        double vx = Math.cos(a) * 0.05, vz = Math.sin(a) * 0.05;
        double vy = 0.02 + random.nextDouble() * 0.02;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, vx, vy, vz);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState st, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.WOOD_GAS_PIPE_BE.get(), WoodGasPipeBlockEntity::tickServer);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState st) {
        return new WoodGasPipeBlockEntity(pos, st);
    }

    // tiny helper so we can declare boxes with ints
    private static VoxelShape box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return Block.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    // Let item-uses (even when holding something) fall through to the block's default interaction
    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // Right-click interaction that doesn't depend on the held item
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof WoodGasPipeBlockEntity pipe) {
            int amt = pipe.getTank().getFluidAmount();
            int cap = pipe.getTank().getCapacity();
            FluidStack stack = pipe.getTank().getFluid();

            Component fluidName = stack.isEmpty() ? Component.literal("empty") : stack.getHoverName();
            Component msg = Component.literal("Pipe: ")
                    .append(Component.literal(Integer.toString(amt)))
                    .append(Component.literal(" / "))
                    .append(Component.literal(Integer.toString(cap)))
                    .append(Component.literal(" mB "))
                    .append(fluidName.copy());

            player.displayClientMessage(msg, false);
        }

        return InteractionResult.CONSUME;
    }
}
