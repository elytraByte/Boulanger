package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.MixingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MixingBlock extends AbstractProcessingBlock{
    public static final MapCodec<MixingBlock> CODEC = simpleCodec(MixingBlock::new);

    public MixingBlock(Properties properties) {
        super(properties);
    }
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MixingBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
