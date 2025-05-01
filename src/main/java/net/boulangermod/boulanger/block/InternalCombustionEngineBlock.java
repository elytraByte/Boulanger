package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.InternalCombustionEngineBlockEntity;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public class InternalCombustionEngineBlock extends BaseEntityBlock implements EntityBlock {
    // 1) Declare the LIT property
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public InternalCombustionEngineBlock(Properties props) {
        // make the block emit light when LIT==true
        super(props.lightLevel(state -> state.getValue(LIT) ? 13 : 0));
        // 2) set default state to LIT=false
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    // 3) add LIT into the blockstate container
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                                                                  BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type,
                ModBlockEntities.INTERNAL_COMBUSTION_ENGINE_BE.get(),
                InternalCombustionEngineBlockEntity::tick
        );
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState st) {
        return new InternalCombustionEngineBlockEntity(pos, st);
    }
}
