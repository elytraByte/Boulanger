package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.block.entity.WoodGasFlareBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class WoodGasFlareBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public static final MapCodec<WoodGasFlareBlock> CODEC = simpleCodec(WoodGasFlareBlock::new);

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

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

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof WoodGasFlareBlockEntity flare) {
                WoodGasFlareBlockEntity.tick(lvl, pos, st, flare);
            }
        };
    }

    // --- helpers ---
    private static VoxelShape mirrorY(VoxelShape s) {
        VoxelShape out = Shapes.empty();
        for (AABB a : s.toAabbs()) {
            out = Shapes.or(out, Shapes.create(
                    a.minX, 1.0 - a.maxY, a.minZ,
                    a.maxX, 1.0 - a.minY, a.maxZ
            ));
        }
        return out.optimize();
    }

    // 6×6 cross-section (centered), 8 px deep, always anchored to the ATTACH face (= opposite of FACING)
    private static final VoxelShape SHAPE_UP    = box(5, 0, 5, 11, 8, 11);    // pipe below → rod from bottom up
    private static final VoxelShape SHAPE_DOWN  = box(5, 8, 5, 11,16, 11);    // pipe above → rod from top down  (your “good” one)

    // Horizontals anchored to the pipe side (opposite FACING)
    private static final VoxelShape SHAPE_NORTH = box(5, 5, 8,  11,11,16);    // FACING=NORTH → pipe SOUTH (z=16)
    private static final VoxelShape SHAPE_SOUTH = box(5, 5, 0,  11,11, 8);    // FACING=SOUTH → pipe NORTH (z=0)
    private static final VoxelShape SHAPE_EAST  = box(0,  5, 5,   8,11,11);   // FACING=EAST  → pipe WEST  (x=0)
    private static final VoxelShape SHAPE_WEST  = box(8,  5, 5,  16,11,11);   // FACING=WEST  → pipe EAST  (x=16)

    private static VoxelShape shapeFor(Direction f) {
        return switch (f) {
            case UP    -> SHAPE_UP;
            case DOWN  -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST  -> SHAPE_EAST;
            case WEST  -> SHAPE_WEST;
        };
    }

    // Use the same oriented shape for all queries
    @Override public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c)         { return shapeFor(s.getValue(FACING)); }
    @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c){ return shapeFor(s.getValue(FACING)); }
    @Override public VoxelShape getOcclusionShape(BlockState s, BlockGetter g, BlockPos p)                    { return shapeFor(s.getValue(FACING)); }
    @Override public VoxelShape getVisualShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c)   { return shapeFor(s.getValue(FACING)); }


    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // Placement: face points AWAY from the pipe (pipe sits behind)
    @Nullable
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace(); // the side we clicked on the pipe
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();

        BlockState trial = defaultBlockState().setValue(FACING, face).setValue(LIT, false);
        // Only place if there's a pipe behind the flare (opposite its facing)
        return canSurvive(trial, level, pos) ? trial : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        Direction attachDir = state.getValue(FACING).getOpposite(); // block behind the flare
        BlockPos behind = pos.relative(attachDir);
        BlockState neighbor = world.getBlockState(behind);

        // ✅ allow either a pipe OR the passthrough block
        return neighbor.is(net.boulangermod.boulanger.block.ModBlocks.WOODGAS_PIPE.get())
                || neighbor.is(net.boulangermod.boulanger.block.ModBlocks.FEED_THROUGH_BLOCK.get());


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


    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WoodGasFlareBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!state.getValue(WoodGasFlareBlock.LIT)) return;

        // Oriented shape → bounds per current FACING
        VoxelShape shape = state.getShape(level, pos);
        AABB box = shape.isEmpty() ? new AABB(0,0,0,1,1,1) : shape.bounds();

        // Center + half-extent in each axis (in block coords)
        final double cx0 = pos.getX() + (box.minX + box.maxX) * 0.5;
        final double cy0 = pos.getY() + (box.minY + box.maxY) * 0.5;
        final double cz0 = pos.getZ() + (box.minZ + box.maxZ) * 0.5;

        final double hx = (box.maxX - box.minX) * 0.5;
        final double hy = (box.maxY - box.minY) * 0.5;
        final double hz = (box.maxZ - box.minZ) * 0.5;

        // Offset IN from the outward face by 3px (and we removed the previous -1px lowering)
        final double off = 5.0 / 16.0;   // distance inward from the face
        final double pad = 1.0 / 16.0;   // keep away from edges

        double cx = cx0, cy = cy0, cz = cz0;

        Direction f = state.getValue(WoodGasFlareBlock.FACING);
        switch (f) {
            case UP ->    cy = cy0 + (hy - off);
            case DOWN ->  cy = cy0 - (hy - off);
            case SOUTH -> cz = cz0 + (hz - off);
            case NORTH -> cz = cz0 - (hz - off);
            case EAST ->  cx = cx0 + (hx - off);
            case WEST ->  cx = cx0 - (hx - off);
        }

        // Clamp within the oriented box so we never escape on rotations
        cx = Mth.clamp(cx, pos.getX() + box.minX + pad, pos.getX() + box.maxX - pad);
        cy = Mth.clamp(cy, pos.getY() + box.minY + pad, pos.getY() + box.maxY - pad);
        cz = Mth.clamp(cz, pos.getZ() + box.minZ + pad, pos.getZ() + box.maxZ - pad);

        // Flame (small upward drift looks fine even when sideways)
        level.addParticle(ParticleTypes.FLAME, cx, cy, cz, 0.0, 0.01, 0.0);

        if (rand.nextFloat() < 0.20f)
            level.addParticle(ParticleTypes.SMALL_FLAME, cx, cy + 0.02, cz, 0.0, 0.015, 0.0);

        if (rand.nextFloat() < 0.35f)
            level.addParticle(ParticleTypes.SMOKE, cx, cy + 0.04, cz, 0.0, 0.02, 0.0);

        if (rand.nextFloat() < 0.04f)
            level.playLocalSound(cx, cy, cz, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.35f, 1.0f, false);
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
