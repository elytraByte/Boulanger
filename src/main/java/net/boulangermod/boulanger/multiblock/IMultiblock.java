package net.boulangermod.boulanger.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Basic multiblock contract inspired by Immersive Engineering.
 * Implementations handle structure creation, validation and rendering.
 */
public interface IMultiblock {

    /**
     * @return unique identifier for this multiblock.
     */
    String getUniqueName();

    /**
     * Size of the multiblock structure. Used for placement helpers.
     */
    BlockPos getSize();

    /**
     * Check if the provided block state can trigger structure creation.
     */
    boolean isBlockTrigger(BlockState state);

    /**
     * Attempt to assemble the multiblock at the given position and facing.
     * Returns true if construction was successful.
     */
    boolean create(Level level, BlockPos pos, Direction facing, Player player);

    /**
     * Break down the structure starting from the provided master position.
     */
    void disassemble(Level level, BlockPos masterPos);

    /**
     * Render the fully formed structure in a manual or preview UI.
     */
    void renderFormedStructure(PoseStack pose, Level level);

    /**
     * Optional scale for manual rendering.
     */
    default float getManualScale() { return 1.0F; }
}