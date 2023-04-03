package org.l3e.Boulanger.block;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.l3e.Boulanger.item.ModItems;

public class HardRedSpringWheat extends CropBlock {
    public HardRedSpringWheat(Properties p_52247_) {
        super(p_52247_);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.HARD_RED_SPRING_WHEAT_SEEDS.get();
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return super.getAgeProperty();
    }

    @Override
    protected int getAge(BlockState age) {
        return super.getAge(age);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }
}
