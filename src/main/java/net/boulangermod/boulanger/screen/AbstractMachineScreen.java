package net.boulangermod.boulanger.screen;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public abstract class AbstractMachineScreen<T extends AbstractMachineMenu> extends AbstractContainerScreen<T> {
    public AbstractMachineScreen(T menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    // Override render, background, labels, tooltips, etc.
}

