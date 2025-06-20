package net.boulangermod.boulanger.multiblock;

import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

public class TestMultiblockMasterBlockEntity
        extends AbstractMultiblockMasterEntity {

    public TestMultiblockMasterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEST_MULTIBLOCK_MASTER_BE.get(), pos, state);
    }

    /** Try to form or dismantle on demand (e.g. called from the Block’s onUse). */
    public boolean tryFormOrDismantle() {
        if (getSlavePositions().isEmpty()) {
            return formStructure();
        } else {
            dismantleStructure();
            return false;
        }
    }

    private boolean formStructure() {
        BlockPos origin = getBlockPos();
        var world = level;
        // gather expected slave positions in a 2×2×2 cube *around* this origin
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (p.equals(origin)) continue; // skip master itself
                    // check proper block & TE
                    if (!(world.getBlockEntity(p) instanceof AbstractMultiblockSlaveEntity slave)) {
                        return false;
                    }
                }
            }
        }
        // everything’s there, register slaves
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (p.equals(origin)) continue;
                    var te = (AbstractMultiblockSlaveEntity) world.getBlockEntity(p);
                    te.setMasterPos(origin);
                    addSlave(p);
                }
            }
        }
        setChanged();
        return true;
    }

    private void dismantleStructure() {
        var world = level;
        // first, drop the master link on each slave TE
        for (BlockPos slavePos : getSlavePositions()) {
            var be = world.getBlockEntity(slavePos);
            if (be instanceof AbstractMultiblockSlaveEntity slave) {
                slave.setMasterPos(null);
            }
        }
        // now unregister them using the inherited method
        var toRemove = new ArrayList<>(getSlavePositions());
        for (BlockPos slavePos : toRemove) {
            removeSlave(slavePos);
        }
        // mark dirty one last time (removeSlave already calls setChanged())
        setChanged();
    }
}