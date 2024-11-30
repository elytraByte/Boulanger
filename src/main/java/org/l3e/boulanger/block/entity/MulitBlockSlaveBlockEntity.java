package org.l3e.boulanger.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MulitBlockSlaveBlockEntity extends BlockEntity {
    private BlockPos masterBlockPos;

    public MulitBlockSlaveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MB_SLAVE.get(), pos, state);
    }

    public void setMaster(BlockPos masterPos) {
        this.masterBlockPos = masterPos;
    }

    public BlockPos getMaster() {
        return masterBlockPos;
    }
}

