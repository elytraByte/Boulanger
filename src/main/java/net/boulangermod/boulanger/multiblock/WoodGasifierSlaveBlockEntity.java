package net.boulangermod.boulanger.multiblock;

import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.block.entity.WoodGasifierBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * Simple slave block entity for the Wood Gasifier multiblock.
 */
public class WoodGasifierSlaveBlockEntity extends AbstractMultiblockSlaveEntity {

    public WoodGasifierSlaveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOOD_GASIFIER_SLAVE_BE.get(), pos, state);
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        BlockPos m = getMasterPos();
        if (level != null && m != null) {
            var te = level.getBlockEntity(m);
            if (te instanceof WoodGasifierBlockEntity master) {
                return master.getItemHandler(side);
            }
        }
        // fall back to an empty handler if you like, or super:
        return super.getItemHandler(side);
    }

    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        BlockPos m = getMasterPos();
        if (level != null && m != null) {
            var te = level.getBlockEntity(m);
            if (te instanceof WoodGasifierBlockEntity master) {
                return master.getFluidHandler(side);
            }
        }
        return super.getFluidHandler(side);
    }

}
