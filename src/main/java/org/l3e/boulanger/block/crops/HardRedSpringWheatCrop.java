package org.l3e.boulanger.block.crops;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.l3e.boulanger.item.ModItems;

public class HardRedSpringWheatCrop extends CropBlock {
    public static final int MAX_AGE = 7;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);

    public HardRedSpringWheatCrop(Properties properties) {
        super(properties);
    }

//TODO can we pass in a generic WheatSeed ItemStack with the WHEAT_TYPE ModDataComponentTypes?
//    @Override
//    protected ItemLike getBaseSeedId() {
//        return ModItems.HARD_RED_SPRING_WHEAT_SEEDS;
//    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(AGE);
    }
}
