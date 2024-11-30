package org.l3e.boulanger.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.l3e.boulanger.block.ModBlocks;
import org.l3e.boulanger.util.ModTags;

public class MultiBlockMasterBlockEntity extends BlockEntity {

    private boolean isMultiblockAssembled;

    public MultiBlockMasterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOOD_GASIFIER_BE.get(), pos, state);
    }

    public void checkMultiblock() {
        // Check all blocks in the structure
        if (isStructureValid()) {
            isMultiblockAssembled = true;
        } else {
            isMultiblockAssembled = false;
        }
    }

    private boolean isStructureValid() {
        for (int x = 0; x <= 1; x++) { // Loop over width (2 blocks)
            for (int y = 0; y <= 2; y++) { // Loop over height (3 blocks)
                for (int z = 0; z <= 0; z++) { // Loop over depth (1 block)
                    BlockPos checkPos = worldPosition.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    // Check if the block is a valid master or slave block
                    if (!state.is(ModTags.Blocks.MB_MASTER) && !state.is(ModTags.Blocks.MB_SLAVE)) {
                        return false; // Invalid structure
                    }
                }
            }
        }
        return true; // Valid structure
    }
}