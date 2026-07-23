//pneumatic ducting deprecated for the time being
//package net.boulangermod.boulanger.block.pneumatic;
//
//import com.mojang.serialization.MapCodec;
//import net.boulangermod.boulanger.pneumatic.blockentity.AirTankBlockEntity;
//import net.boulangermod.boulanger.pneumatic.network.AirNetworkManager;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.world.item.context.BlockPlaceContext;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.BaseEntityBlock;
//import net.minecraft.world.level.block.Block;
//import net.minecraft.world.level.block.Mirror;
//import net.minecraft.world.level.block.RenderShape;
//import net.minecraft.world.level.block.Rotation;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.world.level.block.state.StateDefinition;
//import net.minecraft.world.level.block.state.properties.BlockStateProperties;
//import net.minecraft.world.level.block.state.properties.DirectionProperty;
//import org.jetbrains.annotations.Nullable;
//
//public class AirTankBlock extends BaseEntityBlock {
//    public static final MapCodec<AirTankBlock> CODEC = simpleCodec(AirTankBlock::new);
//
//    // “Port face” direction (the side that can connect to ducts / endpoints)
//    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
//
//    public AirTankBlock(Properties props) {
//        super(props);
//        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
//    }
//
//    @Override
//    protected MapCodec<? extends BaseEntityBlock> codec() {
//        return CODEC;
//    }
//
//    @Override
//    public RenderShape getRenderShape(BlockState state) {
//        return RenderShape.MODEL;
//    }
//
//    @Override
//    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
//        return new AirTankBlockEntity(pos, state);
//    }
//
//    @Override
//    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
//        builder.add(FACING);
//    }
//
//    @Override
//    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
//        // Furnace-style: front faces the player
//        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
//    }
//
//    @Override
//    public BlockState rotate(BlockState state, Rotation rot) {
//        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
//    }
//
//    @Override
//    public BlockState mirror(BlockState state, Mirror mirror) {
//        return rotate(state, mirror.getRotation(state.getValue(FACING)));
//    }
//
//    @Override
//    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
//        super.onPlace(state, level, pos, oldState, movedByPiston);
//        notifyTopology(level, pos);
//    }
//
//    @Override
//    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
//        if (state.getBlock() != newState.getBlock()) {
//            notifyTopology(level, pos);
//        }
//        super.onRemove(state, level, pos, newState, movedByPiston);
//    }
//
//    private static void notifyTopology(Level level, BlockPos pos) {
//        if (level.isClientSide) return;
//
//        // If BE exists, let it enqueue (keeps pattern consistent with ducts)
//        BlockEntity be = level.getBlockEntity(pos);
//        if (be instanceof AirTankBlockEntity tank) {
//            tank.requestAirNetworkRebuild();
//            return;
//        }
//
//        // Fallback (rare timing cases)
//        AirNetworkManager.get(level).enqueueTopologyChange(pos);
//    }
//}
