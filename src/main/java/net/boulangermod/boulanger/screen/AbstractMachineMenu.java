package net.boulangermod.boulanger.screen;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class AbstractMachineMenu extends AbstractContainerMenu {
    protected final ContainerLevelAccess access;
    protected final BlockEntity blockEntity;

    protected AbstractMachineMenu(MenuType<?> type, int id, Inventory inv, BlockEntity entity) {
        super(type, id);
        this.blockEntity = entity;
        this.access = ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos());
    }
}

