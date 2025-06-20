package net.boulangermod.boulanger.multiblock;

import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TestMultiblockSlaveBlockEntity
        extends AbstractMultiblockSlaveEntity {

    public TestMultiblockSlaveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEST_MULTIBLOCK_SLAVE_BE.get(), pos, state);
    }

    // you can add slave‐specific logic here if needed
}