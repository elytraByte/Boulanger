package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.WoodGasTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

public class WoodGasTankBlock extends SimpleProcessingBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF =
            BlockStateProperties.DOUBLE_BLOCK_HALF;

    public static final MapCodec<WoodGasTankBlock> CODEC =
            simpleCodec(WoodGasTankBlock::new);


    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    private static final VoxelShape SHAPE = Shapes.box(2/16.0, 0, 2/16.0, 14/16.0, 1.0, 14/16.0);

    public WoodGasTankBlock(Properties properties) {
        this(properties, WoodGasTankBlockEntity::new);
    }

    public WoodGasTankBlock(Properties properties, BlockEntityFactory factory) {
        super(properties, factory);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(HALF);
    }

    // ——— tall placement ———
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1) return null;

        BlockState above = level.getBlockState(pos.above());
        if (!above.canBeReplaced(ctx)) return null;

        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // remove the other half first
        DoubleBlockHalf half = state.getValue(HALF);
        BlockPos otherPos = (half == DoubleBlockHalf.LOWER) ? pos.above() : pos.below();
        BlockState other = level.getBlockState(otherPos);
        if (other.getBlock() == this) {
            level.removeBlock(otherPos, false);
        }
        // then delegate to super and return its result (1.21 requires returning BlockState)
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (half == DoubleBlockHalf.LOWER && dir == Direction.UP) {
            if (neighborState.getBlock() != this || neighborState.getValue(HALF) != DoubleBlockHalf.UPPER) {
                level.destroyBlock(pos, true);
            }
        } else if (half == DoubleBlockHalf.UPPER && dir == Direction.DOWN) {
            if (neighborState.getBlock() != this || neighborState.getValue(HALF) != DoubleBlockHalf.LOWER) {
                level.destroyBlock(pos, false);
            }
        }
        return super.updateShape(state, dir, neighborState, level, pos, neighborPos);
    }

    // ——— shapes / rendering ———
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape baseShape(BlockState state) { return SHAPE; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE; // same footprint for both halves; tweak if needed
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        // Let normal item interactions (buckets, tools) happen unless empty-hand or sneaking
        boolean showInfo = stack.isEmpty() || player.isShiftKeyDown();
        if (!showInfo) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (!level.isClientSide()) {
            // The LOWER half stores the fluid; UPPER delegates. Read from LOWER.
            BlockPos basePos = (state.getValue(HALF) == DoubleBlockHalf.UPPER) ? pos.below() : pos;
            var be = level.getBlockEntity(basePos);
            if (be instanceof net.boulangermod.boulanger.block.entity.WoodGasTankBlockEntity tank) {
                int amt = tank.getFluidAmount();
                int cap = tank.getFluidCapacity();
                int pct = (cap > 0) ? (amt * 100) / cap : 0;

                // Simple message; swap to a translatable if you prefer
                net.minecraft.network.chat.Component msg =
                        net.minecraft.network.chat.Component.literal(
                                "Wood gas: " + amt + " / " + cap + " mB (" + pct + "%)"
                        );

                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    sp.sendSystemMessage(msg);
                }
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

}
