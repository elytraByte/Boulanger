package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.block.entity.WoodGasifierBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WoodGasifierBlock extends BaseEntityBlock {
    public static final MapCodec<WoodGasifierBlock> CODEC = simpleCodec(WoodGasifierBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty    LIT    = BlockStateProperties.LIT;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty HIDDEN = BooleanProperty.create("hidden");

    public WoodGasifierBlock(Properties props) {
        super(props.noOcclusion().lightLevel(s -> s.getValue(LIT) ? 13 : 0));
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(LIT, false)
                        .setValue(HIDDEN, false)
                        .setValue(FORMED, false)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull VoxelShape getOcclusionShape(@NotNull BlockState state,
                                                 @NotNull BlockGetter level,
                                                 @NotNull BlockPos pos) {
        // When hidden (multiblock formed), don't occlude anything.
        return state.getValue(HIDDEN) ? Shapes.empty()
                : super.getOcclusionShape(state, level, pos);
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, LIT, HIDDEN, FORMED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(HIDDEN) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // player’s look direction → horizontal facing
        Direction playerDir = ctx.getHorizontalDirection();
        // we want the *front* of the block to point at the player, so take the opposite
        Direction front = playerDir.getOpposite();

        return this.defaultBlockState()
                .setValue(FACING, front)
                .setValue(FORMED, false)
                .setValue(HIDDEN, false)
                .setValue(LIT, false);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WoodGasifierBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        BlockPos anchorPos = WoodGasifierBlockEntity.resolveAnchor(level, pos);
        var be = level.getBlockEntity(anchorPos);
        if (!(be instanceof WoodGasifierBlockEntity gasifier)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.getItem() == Items.BREAD) {
            var result = gasifier.tryToggleForm(player, hand, hit.getDirection());
            return result.consumesAction()
                    ? ItemInteractionResult.SUCCESS
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos anchorPos = WoodGasifierBlockEntity.resolveAnchor(level, pos);
        var be = level.getBlockEntity(anchorPos);
        if (be instanceof WoodGasifierBlockEntity gasifier && gasifier.isFormed() && gasifier.isAnchor()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer server) {
                server.openMenu(gasifier, buf -> buf.writeBlockPos(anchorPos)); // send anchor to client
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.WOOD_GASIFIER_BE.get(),
                (lvl, pos, st, be) -> WoodGasifierBlockEntity.tick(lvl, pos, st, (WoodGasifierBlockEntity) be));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        // Only the anchor should handle client effects
        BlockPos anchorPos = WoodGasifierBlockEntity.resolveAnchor(level, pos);
        if (!pos.equals(anchorPos)) return;

        BlockEntity be = level.getBlockEntity(anchorPos);
        if (!(be instanceof WoodGasifierBlockEntity gasifier)) return;
        if (!gasifier.isFormed()) return;

        // Only run when visually "on"
        if (!state.getValue(LIT)) return;

        // Orientation
        final Direction front = gasifier.getFrontFacing();      // model front (respects FLIP)
        final Direction port  = gasifier.getPortSide();         // RIGHT-of-front (new outlet)
        final Direction left  = front.getCounterClockWise();    // still useful for flame placement
        final Direction right = front.getClockWise();

        /* ───────── sounds ───────── */
        double sx = anchorPos.getX() + 0.5, sy = anchorPos.getY() + 0.5, sz = anchorPos.getZ() + 0.5;
        if (random.nextInt(6) == 0) {
            level.playLocalSound(sx, sy, sz, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS,
                    0.5f, 1.0f + (random.nextFloat() - 0.5f) * 0.2f, false);
        }
        if (random.nextInt(24) == 0) {
            level.playLocalSound(sx, sy, sz, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS,
                    0.25f, 1.0f + (random.nextFloat() - 0.5f) * 0.25f, false);
        }

        /* ───────── exhaust SMOKE on the PORT (RIGHT) face ───────── */
        BlockPos external = null;
        Direction facing = state.getValue(FACING);
        for (BlockPos cell : WoodGasifierBlockEntity.footprintFromMin(anchorPos, facing)) {
            BlockPos n = cell.relative(port);
            if (!gasifier.isInFootprint(n)) { external = n; break; }
        }
        if (external == null) external = anchorPos.relative(port);

        final double epsOut = 0.02, yMid = 0.50;
        double bx = external.getX() + 0.5 - port.getStepX() * (0.5 + epsOut);
        double by = anchorPos.getY() + yMid;
        double bz = external.getZ() + 0.5 - port.getStepZ() * (0.5 + epsOut);

        double jx = (random.nextDouble() - 0.5) * 0.10;
        double jz = (random.nextDouble() - 0.5) * 0.10;
        double vx = (random.nextDouble() - 0.5) * 0.02;
        double vz = (random.nextDouble() - 0.5) * 0.02;
        double vy = 0.02 + random.nextDouble() * 0.02;
        level.addParticle(ParticleTypes.SMOKE, bx + jx, by, bz + jz, vx, vy, vz);

        /* ───────── FLAME placement: front grate (shift left, narrower) ───────── */
        {
            BlockPos bottomLeftCell = null;
            int baseY = anchorPos.getY();
            for (BlockPos cell : WoodGasifierBlockEntity.footprintFromMin(anchorPos, facing)) {
                if (cell.getY() != baseY) continue;
                if (!gasifier.isInFootprint(cell.relative(left))) { bottomLeftCell = cell; break; }
            }
            if (bottomLeftCell == null) bottomLeftCell = anchorPos;

            // front width in cells
            int widthCells = 1;
            BlockPos sweep = bottomLeftCell;
            while (gasifier.isInFootprint(sweep.relative(right))) { sweep = sweep.relative(right); widthCells++; }

            // ——— knobs ———
            final double OUT_FROM_FRONT = 0.49;
            final double GRATE_Y        = 0.45;
            final double GRATE_W        = Math.min(0.55 * widthCells, 0.85); // narrower band
            final double GRATE_H        = 0.20;
            final double LEFT_SHIFT     = 1.0; // move flames left on the grate

            // center of front face (bottom row), shifted left
            double cx = bottomLeftCell.getX() + 0.5 + right.getStepX() * (widthCells / 2.0)
                    - right.getStepX() * LEFT_SHIFT;
            double cz = bottomLeftCell.getZ() + 0.5 + right.getStepZ() * (widthCells / 2.0)
                    - right.getStepZ() * LEFT_SHIFT;
            double cy = baseY + GRATE_Y;

            // furnace-style scattered flames in the rectangle
            int flames = 2 + random.nextInt(2);
            for (int i = 0; i < flames; i++) {
                double rx = (random.nextDouble() - 0.5) * GRATE_W;  // left/right inside the narrower grate
                double ry = (random.nextDouble() - 0.5) * GRATE_H;  // up/down

                double px = cx - front.getStepX() * OUT_FROM_FRONT + right.getStepX() * rx;
                double pz = cz - front.getStepZ() * OUT_FROM_FRONT + right.getStepZ() * rx;
                double py = cy + ry;

                level.addParticle(ParticleTypes.FLAME, px, py, pz, 0.0, 0.0, 0.0);
                if (random.nextFloat() < 0.25f) {
                    level.addParticle(ParticleTypes.SMOKE, px, py + 0.02, pz, 0.0, 0.012, 0.0);
                }
            }
        }

    }


    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            var be = level.getBlockEntity(pos);
            if (be instanceof WoodGasifierBlockEntity gas) gas.dismantleMultiblock();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

}
