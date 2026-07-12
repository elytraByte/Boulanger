package net.boulangermod.boulanger.block.pneumatic;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.pneumatic.blockentity.AirCompressorBlockEntity;
import net.boulangermod.boulanger.pneumatic.network.AirNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public class AirCompressorBlock extends BaseEntityBlock {
    public static final MapCodec<AirCompressorBlock> CODEC = simpleCodec(AirCompressorBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public AirCompressorBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite(); // furnace-style
        boolean powered = ctx.getLevel().hasNeighborSignal(ctx.getClickedPos());
        return defaultBlockState().setValue(FACING, facing).setValue(POWERED, powered);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide) return;

        // Keep POWERED in sync for visuals, but DON'T trigger topology rebuilds on redstone-only changes.
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            // UPDATE_CLIENTS only: avoid neighbor updates (which would spam duct rebuilds when POWERED toggles)
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            state = state.setValue(POWERED, powered);
        }

        // Only rebuild topology if a *pneumatic* neighbor changed (duct placed/removed, etc.)
        if (isPneumaticConnectable(block) || isPneumaticConnectable(level.getBlockState(fromPos).getBlock())) {
            AirNetworkManager.get(level).enqueueTopologyChange(pos);
        }
    }

    private static boolean isPneumaticConnectable(Block b) {
        // compressor only cares about duct/tank adjacency for topology
        return b instanceof PneumaticDuctBlock || b instanceof AirTankBlock;
    }


    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) AirNetworkManager.get(level).enqueueTopologyChange(pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide) {
            AirNetworkManager.get(level).enqueueTopologyChange(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AirCompressorBlockEntity(pos, state);
    }
}
