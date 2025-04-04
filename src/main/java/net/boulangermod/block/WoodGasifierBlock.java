//package net.boulangermod.block;
//
//import com.mojang.serialization.MapCodec;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.sounds.SoundEvents;
//import net.minecraft.sounds.SoundSource;
//import net.minecraft.world.Containers;
//import net.minecraft.world.InteractionHand;
//import net.minecraft.world.InteractionResult;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.context.BlockPlaceContext;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.*;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.world.level.block.state.StateDefinition;
//import net.minecraft.world.level.block.state.properties.BlockStateProperties;
//import net.minecraft.world.level.block.state.properties.EnumProperty;
//import net.minecraft.world.phys.BlockHitResult;
//import org.jetbrains.annotations.Nullable;
//import net.boulangermod.block.entity.WoodGasifierBlockEntity;
//
//public class WoodGasifierBlock extends BaseEntityBlock {
//    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
//    public static final MapCodec<WoodGasifierBlock> CODEC = simpleCodec(WoodGasifierBlock::new);
//
//    protected WoodGasifierBlock(Properties properties) {
//        super(properties);
//    }
//
//    @Override
//    protected MapCodec<? extends BaseEntityBlock> codec() {
//        return CODEC;
//    }
//    @Override
//    protected RenderShape getRenderShape(BlockState pState) {
//        return RenderShape.MODEL;
//    }
//
//    @Nullable
//    @Override
//    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
//        return new WoodGasifierBlockEntity(blockPos, blockState);
//    }
//
//    @Override
//    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
//        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
//    }
//
//    @Override
//    public BlockState rotate(BlockState pState, Rotation pRotation) {
//        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
//    }
//
//    @Override
//    public BlockState mirror(BlockState pState, Mirror pMirror) {
//        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
//    }
//
//    @Override
//    protected void onRemove(BlockState pState, Level pLevel, BlockPos pPos,
//                            BlockState pNewState, boolean pMovedByPiston) {
//        if(pState.getBlock() != pNewState.getBlock()) {
//            if(pLevel.getBlockEntity(pPos) instanceof WoodGasifierBlockEntity woodGasifierBlockEntity) {
//                Containers.dropContents(pLevel, pPos, woodGasifierBlockEntity);
//                pLevel.updateNeighbourForOutputSignal(pPos, this);
//            }
//        }
//        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
//    }
//
//    @Override
//    protected
//    InteractionResult useItemOn(ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos,
//                                              Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult) {
//        if(pLevel.getBlockEntity(pPos) instanceof WoodGasifierBlockEntity woodGasifierBlockEntity) {
//            if(woodGasifierBlockEntity.isEmpty() && !pStack.isEmpty()) {
//                woodGasifierBlockEntity.setItem(0, pStack);
//                pStack.shrink(1);
//                pLevel.playSound(pPlayer, pPos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1f, 2f);
//            } else if(pStack.isEmpty()) {
//                ItemStack stackOnPedestal = woodGasifierBlockEntity.getItem(0);
//                pPlayer.setItemInHand(InteractionHand.MAIN_HAND, stackOnPedestal);
//                woodGasifierBlockEntity.clearContent();
//                pLevel.playSound(pPlayer, pPos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1f, 1f);
//
//            }
//
//        }
//        return InteractionResult.SUCCESS;
//
//    }
//
//    @Override
//    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
//
//        return InteractionResult.SUCCESS;
//    }
//
//    @Override
//    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
//        builder.add(FACING);
//    }
//
//}

