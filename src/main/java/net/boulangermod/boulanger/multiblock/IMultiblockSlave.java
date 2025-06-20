package net.boulangermod.boulanger.multiblock;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for multiblock child block entities.
 */
public interface IMultiblockSlave {
    /**
     * Sets the controller position for this part.
     */
    void setMasterPos(BlockPos pos);

    /**
     * @return the controller position or null if not assigned.
     */
    @Nullable
    BlockPos getMasterPos();

    /**
     * Convenience method to fetch the controller instance if loaded.
     */
    @Nullable
    IMultiblockMaster getMaster();
}