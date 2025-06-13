package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.screen.AbstractMachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractMachineBlockEntity extends BlockEntity implements MenuProvider, Container {
    protected final NonNullList<ItemStack> items;

    public AbstractMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state);
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    // Override for ticking, I/O, etc.
    public void tick() {}

    @Override
    public abstract AbstractMachineMenu createMenu(int id, Inventory inv, Player player);
}

