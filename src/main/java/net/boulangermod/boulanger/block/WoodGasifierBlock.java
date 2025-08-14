package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.block.entity.WoodGasifierBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
    public @NotNull VoxelShape getOcclusionShape(@NotNull BlockState state,
                                                 @NotNull BlockGetter level,
                                                 @NotNull BlockPos pos) {
        // When hidden (multiblock formed), don't occlude anything.
        return state.getValue(HIDDEN) ? Shapes.empty()
                : super.getOcclusionShape(state, level, pos);
    }


    @Override
    @SuppressWarnings("unchecked")
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return (MapCodec) CODEC;
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

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                                                                  BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
                type,
                ModBlockEntities.WOOD_GASIFIER_BE.get(),
                WoodGasifierBlockEntity::tick
        );
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
