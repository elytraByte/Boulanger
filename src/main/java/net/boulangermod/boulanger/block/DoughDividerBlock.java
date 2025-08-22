package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.DoughDividerBlockEntity;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DoughDividerBlock extends SimpleProcessingBlock {
    public static final MapCodec<DoughDividerBlock> CODEC = simpleCodec(DoughDividerBlock::new);

    public DoughDividerBlock(Properties properties) {
        super(properties, DoughDividerBlockEntity::new);
    }

    @Override public boolean useShapeForLightOcclusion(BlockState s) { return true; }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
}
