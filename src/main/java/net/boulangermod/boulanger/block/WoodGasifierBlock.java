package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.block.entity.WoodGasifierBlockEntity;
import net.boulangermod.boulanger.multiblock.AbstractMultiblockSlaveEntity;
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
import org.jetbrains.annotations.Nullable;

public class WoodGasifierBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty    LIT     = BlockStateProperties.LIT;
    public static final BooleanProperty HIDDEN = BooleanProperty.create("hidden");

    public WoodGasifierBlock(Properties props) {
        super(props.lightLevel(state -> state.getValue(LIT) ? 13 : 0));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false)
                .setValue(HIDDEN, false)
        );
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected MapCodec<? extends BaseEntityBlock> codec() {
        // raw cast lets the compiler accept it:
        return (MapCodec) CODEC;
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, LIT, HIDDEN);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(HIDDEN)
                ? RenderShape.INVISIBLE
                : RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite());
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
    protected ItemInteractionResult useItemOn(ItemStack stack,
                                              BlockState state,
                                              Level level,
                                              BlockPos pos,
                                              Player player,
                                              InteractionHand hand,
                                              BlockHitResult hit) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        var be = level.getBlockEntity(pos);
        if (!(be instanceof WoodGasifierBlockEntity gasifier)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (stack.getItem() == Items.BREAD && !gasifier.isFormed()) {
            boolean formed = gasifier.tryFormOrDismantle();
            if (player instanceof ServerPlayer server) {
                server.sendSystemMessage(
                        Component.literal(formed ? "Multiblock formed!" : "Failed to form multiblock")
                );
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state,
                                               Level level,
                                               BlockPos pos,
                                               Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResult.SUCCESS;
        }
        var be = level.getBlockEntity(pos);
        if (!(be instanceof WoodGasifierBlockEntity gasifier) || !gasifier.isFormed()) {
            return InteractionResult.PASS;
        }
        boolean valid = gasifier.getSlavePositions().stream().allMatch(sp -> {
            var slaveBe = level.getBlockEntity(sp);
            return slaveBe instanceof AbstractMultiblockSlaveEntity s
                    && pos.equals(s.getMasterPos());
        });
        if (!valid) {
            gasifier.tryFormOrDismantle();
            server.sendSystemMessage(Component.literal("Multiblock invalid — dismantled."));
            return InteractionResult.CONSUME;
        }
        server.openMenu(gasifier, buf -> buf.writeBlockPos(pos));
        return InteractionResult.CONSUME;
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
                WoodGasifierBlockEntity::ticker
        );
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (oldState.getBlock() != newState.getBlock()) {
            super.onRemove(oldState, level, pos, newState, moved);
        }
    }
}
