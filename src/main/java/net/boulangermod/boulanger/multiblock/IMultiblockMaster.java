package net.boulangermod.boulanger.multiblock;

import net.minecraft.core.BlockPos;
import java.util.Set;

/**
 * Interface for multiblock controller block entities.
 */
public interface IMultiblockMaster {
    /**
     * Registers a child part of this multiblock.
     */
    void addSlave(BlockPos pos);

    /**
     * Unregisters a child part.
     */
    void removeSlave(BlockPos pos);

    /**
     * @return immutable set of all registered slave positions.
     */
    Set<BlockPos> getSlavePositions();
}