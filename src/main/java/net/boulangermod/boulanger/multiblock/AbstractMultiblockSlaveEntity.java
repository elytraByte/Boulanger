package net.boulangermod.boulanger.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for multiblock parts.
 */
public abstract class AbstractMultiblockSlaveEntity extends BlockEntity implements IMultiblockSlave {
    @Nullable
    private BlockPos masterPos;

    protected AbstractMultiblockSlaveEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void setMasterPos(BlockPos pos) {
        this.masterPos = pos;
        setChanged();
    }

    @Override
    @Nullable
    public BlockPos getMasterPos() {
        return masterPos;
    }

    @Override
    @Nullable
    public IMultiblockMaster getMaster() {
        if (masterPos == null || level == null) return null;
        BlockEntity be = level.getBlockEntity(masterPos);
        if (be instanceof IMultiblockMaster master) {
            return master;
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (masterPos != null) {
            tag.putInt("MasterX", masterPos.getX());
            tag.putInt("MasterY", masterPos.getY());
            tag.putInt("MasterZ", masterPos.getZ());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("MasterX")) {
            int x = tag.getInt("MasterX");
            int y = tag.getInt("MasterY");
            int z = tag.getInt("MasterZ");
            masterPos = new BlockPos(x, y, z);
        } else {
            masterPos = null;
        }
    }

    /** Expose the master’s inventory on any slave. */
    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (masterPos != null && level != null) {
            var be = level.getBlockEntity(masterPos);
            if (be instanceof AbstractMultiblockMachineEntity master) {
                return master.getItemHandler(side);
            }
        }
        // fallback to an empty handler
        return new ItemStackHandler(0);
    }

    /** Expose the master’s tank on any slave. */
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        if (masterPos != null && level != null) {
            var be = level.getBlockEntity(masterPos);
            if (be instanceof AbstractMultiblockMachineEntity master) {
                return master.getFluidHandler(side);
            }
        }
        // fallback: zero-capacity tank that still marks the TE dirty on change
        return new FluidTank(0, ignored -> {
            setChanged();
            return true;
        });
    }

}
