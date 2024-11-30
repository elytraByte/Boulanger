package org.l3e.boulanger.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.l3e.boulanger.util.ModTags;

public class MB_Slave extends Block {

    public MB_Slave(Properties properties) {
        super(properties);
    }

    // Check if the structure remains valid when this block is broken
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);

        if (!isMoving && !state.is(newState.getBlock())) {
            // Notify the master block that the structure has been broken
            if (level.getBlockState(pos).is(ModTags.Blocks.MB_MASTER)) {
                BlockPos masterPos = findMaster(level, pos);
                if (masterPos != null) {
                    // Deactivate the master block (optional logic)
                    level.removeBlock(masterPos, false);
                }
            }
        }
    }

    // Optional helper to find the master block
    @Nullable
    private BlockPos findMaster(Level level, BlockPos slavePos) {
        for (BlockPos offset : BlockPos.betweenClosed(slavePos.offset(-1, -1, -1), slavePos.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(offset);
            if (state.is(ModTags.Blocks.MB_MASTER)) {
                return offset.immutable();
            }
        }
        return null;
    }

//    @Override
    public boolean canBeReplaced(BlockState state, BlockGetter level, BlockPos pos) {
        return false; // Prevents the slave block from being replaced by placement actions
    }
}
