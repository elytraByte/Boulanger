package org.l3e.boulanger.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.l3e.boulanger.block.entity.MultiBlockMasterBlockEntity;

public class MultiBlockMasterBlock extends Block {
    public MultiBlockMasterBlock(Properties properties) {
        super(properties);
    }

    public void onNeighborChange(BlockState state, Level level, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(state, level, pos, neighbor);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MultiBlockMasterBlockEntity master) {
            master.checkMultiblock(); // Recheck the multiblock structure
        }

    }
}