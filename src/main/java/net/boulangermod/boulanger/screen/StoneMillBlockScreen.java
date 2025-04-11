package net.boulangermod.boulanger.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.boulangermod.boulanger.Boulanger;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class StoneMillBlockScreen extends AbstractContainerScreen<StoneMillBlockMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/stone_mill.png");

    public StoneMillBlockScreen(StoneMillBlockMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }


    @Override
    protected void init () {
        super.init();
        this.inventoryLabelY = 72;
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;

        // center coordinates
        int x0 = (this.width - this.imageWidth) / 2;
        int y0 = (this.height - this.imageHeight) / 2;

    }

    @Override
    protected void renderBg (GuiGraphics guiGraphics,float partialTick, int mouseX, int mouseY){
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x0 = (this.width - this.imageWidth) / 2;
        int y0 = (this.height - this.imageHeight) / 2;

        // draw the full GUI
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x0, y0, 0);
        guiGraphics.pose().scale(0.25f, 0.25f, 1.0f);
        guiGraphics.blit(GUI_TEXTURE, 0, 0, 0, 0, 1024, 1024, 1024, 1024);
        guiGraphics.pose().popPose();

        // now draw the ingredient list on top
        guiGraphics.pose().pushPose();
        // translate into GUI coords (no scale here)
        guiGraphics.pose().translate(x0, y0, 0);

        guiGraphics.pose().popPose();

        // (optionally) draw your progress bar here, too...
    }


    @Override
    public void render (GuiGraphics guiGraphics,int mouseX, int mouseY, float delta){
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels (GuiGraphics guiGraphics,int mouseX, int mouseY){
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 72, 0x404040, false);
    }
}

