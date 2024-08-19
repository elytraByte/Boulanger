package org.l3e.boulanger.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.SpecialPlantable;
import org.jetbrains.annotations.Nullable;

public class BGenericSeed extends BlockItem implements SpecialPlantable
{
    public BGenericSeed(Block block, Properties properties) {
        super(block, properties);
    }


    @Override
    public boolean canPlacePlantAtPosition(ItemStack itemStack, LevelReader levelReader, BlockPos blockPos, @Nullable Direction direction) {
        return false;
    }

    @Override
    public void spawnPlantAtPosition(ItemStack itemStack, LevelAccessor levelAccessor, BlockPos blockPos, @Nullable Direction direction) {

    }
}
