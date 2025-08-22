package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.WoodGasEngineBlockEntity;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class WoodGasEngineBlock extends SimpleProcessingBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public static final MapCodec<WoodGasEngineBlock> CODEC = simpleCodec(WoodGasEngineBlock::new);
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public WoodGasEngineBlock(Properties props) {
        // emit light when running
        super(props.lightLevel(state -> state.getValue(LIT) ? 13 : 0), WoodGasEngineBlockEntity::new);
        // default: FACING=NORTH (from super) + LIT=false
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(LIT, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); // adds FACING
        builder.add(LIT);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T>
    getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.WOODGAS_ENGINE_BE.get(), WoodGasEngineBlockEntity::tick);
    }
}
