// EnergyCableBlock.java
package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.EnergyCableBlockEntity;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class EnergyCableBlock extends BaseEntityBlock {
    public EnergyCableBlock(Properties props) {
        super(props.noOcclusion());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState st) {
        return RenderShape.MODEL;
    }
    // in EnergyCableBlock.java
    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return createTickerHelper(
                type,
                ModBlockEntities.ENERGY_CABLE_BE.get(),
                EnergyCableBlockEntity::tick
        );
    }


    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState st) {
        return new EnergyCableBlockEntity(pos, st);
    }
}
