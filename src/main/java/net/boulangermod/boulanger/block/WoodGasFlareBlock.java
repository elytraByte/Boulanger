package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.block.entity.WoodGasFlareBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class WoodGasFlareBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty   LIT    = BlockStateProperties.LIT;

    // ✅ Use the class ctor; props come from registration
    public static final MapCodec<WoodGasFlareBlock> CODEC = simpleCodec(WoodGasFlareBlock::new);
    @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public WoodGasFlareBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // ✅ set defaults (engine does this too)
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.UP)
                        .setValue(LIT, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, LIT);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        // ✅ Mirror engine pattern: server-only, and type EXACTLY matches your BE type
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.WOODGAS_FLARE_BE.get(),
                WoodGasFlareBlockEntity::tick);
    }

    // Compact rod-ish shape that rotates with FACING
    private static final VoxelShape SHAPE_UP    = box(6, 0, 6, 10, 12, 10);
    private static final VoxelShape SHAPE_DOWN  = box(6, 4, 6, 10, 16, 10);
    private static final VoxelShape SHAPE_NORTH = box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape SHAPE_SOUTH = box(6, 6, 0, 10, 10, 6);
    private static final VoxelShape SHAPE_WEST  = box(10, 6, 6, 16, 10, 10);
    private static final VoxelShape SHAPE_EAST  = box(0, 6, 6, 6, 10, 10);


    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // Placement: face points AWAY from the pipe (pipe sits behind)
    @Nullable
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace(); // the side we clicked on the pipe
        BlockPos pos   = ctx.getClickedPos();
        Level level    = ctx.getLevel();

        BlockState trial = defaultBlockState().setValue(FACING, face).setValue(LIT, false);
        // Only place if there's a pipe behind the flare (opposite its facing)
        return canSurvive(trial, level, pos) ? trial : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        Direction attachDir = state.getValue(FACING).getOpposite(); // where the pipe must be
        BlockPos behind     = pos.relative(attachDir);
        BlockState neighbor = world.getBlockState(behind);
        // Require the neighbor to be your woodgas pipe block
        return neighbor.is(net.boulangermod.boulanger.block.ModBlocks.WOODGAS_PIPE.get());
    }

    @Override
    public BlockState updateShape(BlockState state,
                                  Direction fromDir,
                                  BlockState fromState,
                                  LevelAccessor level,
                                  BlockPos pos,
                                  BlockPos fromPos) {
        // If the block it's attached to changes and is no longer a pipe, break the flare.
        if (fromDir == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, fromDir, fromState, level, pos, fromPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case UP    -> SHAPE_UP;
            case DOWN  -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST  -> SHAPE_WEST;
            case EAST  -> SHAPE_EAST;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WoodGasFlareBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!state.getValue(LIT)) return;

        // dead-center of the block (your nub’s “top” is the center)
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        // steady flame
        level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, cx, cy, cz, 0.0, 0.005, 0.0);

        // soft smoke puff sometimes
        if (rand.nextFloat() < 0.45f) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, cx, cy, cz, 0.0, 0.01, 0.0);
        }

        // occasional extra flicker + crackle
        if (rand.nextFloat() < 0.15f) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, cx, cy, cz, 0.0, 0.01, 0.0);
        }
        if (rand.nextFloat() < 0.04f) {
            level.playLocalSound(cx, cy, cz,
                    net.minecraft.sounds.SoundEvents.CAMPFIRE_CRACKLE,
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    0.35f, 1.0f, false);
        }
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        Direction attach = state.getValue(FACING).getOpposite();
        BlockPos pipePos = pos.relative(attach);

        if (!level.isClientSide) {
            BlockState pipeState = level.getBlockState(pipePos);
            // tell the pipe “your neighbor (this flare) changed”
            level.neighborChanged(pipePos, state.getBlock(), pos);
            // and force a visual/state re-eval (model swap for connections)
            level.sendBlockUpdated(pipePos, pipeState, pipeState, 3);
        }
    }

}
