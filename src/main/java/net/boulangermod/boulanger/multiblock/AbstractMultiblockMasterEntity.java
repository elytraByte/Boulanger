package net.boulangermod.boulanger.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class for multiblock controller block entities.
 */
public abstract class AbstractMultiblockMasterEntity extends BlockEntity implements IMultiblockMaster {
    private final Set<BlockPos> slaves = new HashSet<>();

    protected AbstractMultiblockMasterEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addSlave(BlockPos pos) {
        if (slaves.add(pos)) {
            setChanged();
        }
    }

    @Override
    public void removeSlave(BlockPos pos) {
        if (slaves.remove(pos)) {
            setChanged();
        }
    }

    @Override
    public Set<BlockPos> getSlavePositions() {
        return Collections.unmodifiableSet(slaves);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ListTag list = new ListTag();
        for (BlockPos p : slaves) {
            CompoundTag pt = new CompoundTag();
            pt.putInt("x", p.getX());
            pt.putInt("y", p.getY());
            pt.putInt("z", p.getZ());
            list.add(pt);
        }
        tag.put("Slaves", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        slaves.clear();
        ListTag list = tag.getList("Slaves", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag pt = list.getCompound(i);
            slaves.add(new BlockPos(pt.getInt("x"), pt.getInt("y"), pt.getInt("z")));
        }
    }
}