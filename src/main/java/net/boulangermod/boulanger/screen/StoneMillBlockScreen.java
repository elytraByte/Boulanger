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
    // Overlay for "powered" lamp (your PNG with the “on” bulb)
    private static final ResourceLocation LIGHT_ON =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/on.png");

    // --- Bulb icon hitbox (relative to GUI top-left). Adjust to match your art ---
    private static final int BULB_X = 152;
    private static final int BULB_Y = 7;
    private static final int BULB_W = 16;
    private static final int BULB_H = 16;
    // ------------------------------------------------------------------------------

    public StoneMillBlockScreen(StoneMillBlockMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 72;
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x0 = (this.width - this.imageWidth) / 2;
        int y0 = (this.height - this.imageHeight) / 2;

        // Draw full 1024x1024 background scaled to GUI size (0.25f => 256×256 draw)
        g.pose().pushPose();
        g.pose().translate(x0, y0, 0);
        g.pose().scale(0.25f, 0.25f, 1.0f);
        g.blit(GUI_TEXTURE, 0, 0, 0, 0, 1024, 1024, 1024, 1024);
        g.pose().popPose();

        // Overlays in GUI pixel-space (no scaling)
        g.pose().pushPose();
        g.pose().translate(x0, y0, 0);

        // Power lamp: background shows "off"; draw "on" overlay if we currently have FE
        if (menu.getBlockEntity().isGridPowered()) {
            g.blit(LIGHT_ON, BULB_X, BULB_Y, 0, 0, 16, 16, 16, 16);
        }

        g.pose().popPose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick); // ✅ 4 args
        super.render(g, mouseX, mouseY, partialTick);

        // Tooltip over the bulb area
        int rx = leftPos + BULB_X;
        int ry = topPos  + BULB_Y;
        if (isMouseIn(mouseX, mouseY, rx, ry, BULB_W, BULB_H)) {
            int cur = menu.getEnergyStored();
            int cap = menu.getEnergyCapacity();
            g.renderTooltip(font, Component.literal(cur + " / " + cap + " FE"), mouseX, mouseY);
        }

        this.renderTooltip(g, mouseX, mouseY);
    }


    private static boolean isMouseIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < (x + w) && my < (y + h);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, 72, 0x404040, false);
    }
}
