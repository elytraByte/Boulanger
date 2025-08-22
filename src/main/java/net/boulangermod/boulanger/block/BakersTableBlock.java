package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.BakersTableBlockEntity;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BakersTableBlock extends SimpleProcessingBlock {
    public static final MapCodec<BakersTableBlock> CODEC = simpleCodec(BakersTableBlock::new);


    public BakersTableBlock(BlockBehaviour.Properties properties) {
        super(properties, BakersTableBlockEntity::new);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
