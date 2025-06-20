package net.boulangermod.boulanger.multiblock;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class TestMultiblockSlaveBlock extends BaseEntityBlock {

    public static final MapCodec<TestMultiblockSlaveBlock> CODEC =
            simpleCodec(TestMultiblockSlaveBlock::new);

    public TestMultiblockSlaveBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    // satisfy the abstract contract
    @Override
    public MapCodec<TestMultiblockSlaveBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestMultiblockSlaveBlockEntity(pos, state);
    }
}