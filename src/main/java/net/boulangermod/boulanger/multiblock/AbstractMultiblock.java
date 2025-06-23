package net.boulangermod.boulanger.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractMultiblock implements IMultiblock {

    @Override
    public boolean isBlockTrigger(BlockState state) {
        return false;
    }

    @Override
    public boolean create(Level level, BlockPos pos, Direction facing, Player player) {
        return false;
    }

    @Override
    public void disassemble(Level level, BlockPos masterPos) {
    }

    @Override
    public void renderFormedStructure(com.mojang.blaze3d.vertex.PoseStack pose, Level level) {
    }

    @Override
    public BlockPos getSize() {
        return BlockPos.ZERO;
    }
}
