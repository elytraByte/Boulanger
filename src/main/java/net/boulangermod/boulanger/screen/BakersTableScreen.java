package net.boulangermod.boulanger.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.boulangermod.boulanger.Boulanger;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BakersTableScreen extends AbstractContainerScreen<BakersTableMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/bakers_table.png");

    public BakersTableScreen(BakersTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics); // darkens the world behind the GUI
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY); // tooltips for items
    }

    private void renderBackground(GuiGraphics graphics) {

    }


    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, title, 8, 6, 0x404040, false); // top left
        graphics.drawString(this.font, playerInventoryTitle, 8, 72, 0x404040, false); // above player inventory
    }
}
