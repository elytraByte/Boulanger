package net.boulangermod.boulanger.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.boulangermod.boulanger.Boulanger;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class WoodOvenScreen extends AbstractContainerScreen<WoodOvenMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/furnace.png");
    private static final ResourceLocation FLAME_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/flame.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/arrow_progress.png");

    // Native sizes (no scaling needed since textures are 1:1)
    private static final int FLAME_WIDTH = 56;
    private static final int FLAME_HEIGHT = 56;
    private static final int ARROW_WIDTH = 87;
    private static final int ARROW_HEIGHT = 60;

    // Origin positions in 1024x1024 GUI coordinates (before scaling)
    private static final int FLAME_X_ORIGIN = 224;
    private static final int FLAME_Y_ORIGIN = 144;
    private static final int ARROW_X_ORIGIN = 320;
    private static final int ARROW_Y_ORIGIN = 140;


    public WoodOvenScreen(WoodOvenMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 72;
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.pose().pushPose();

        // Apply 0.25 scale to everything inside
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(0.25f, 0.25f, 1.0f);

        // Main GUI background (1024x1024)
        guiGraphics.blit(GUI_TEXTURE, 0, 0, 0.0f, 0.0f, 1024, 1024, 1024, 1024);

        // 🔥 Flame
        if (this.menu.isLit()) {
            RenderSystem.setShaderTexture(0, FLAME_TEXTURE);
            guiGraphics.blit(FLAME_TEXTURE,
                    FLAME_X_ORIGIN, FLAME_Y_ORIGIN, // inside scaled coords
                    0, 0,
                    FLAME_WIDTH, FLAME_HEIGHT,
                    FLAME_WIDTH, FLAME_HEIGHT);
        }

        // ➡️ Arrow
        if (this.menu.isCrafting()) {
            int progress = this.menu.getCookingProgress(); // 0–ARROW_WIDTH

            RenderSystem.setShaderTexture(0, ARROW_TEXTURE);
            guiGraphics.blit(ARROW_TEXTURE,
                    ARROW_X_ORIGIN, ARROW_Y_ORIGIN,
                    0, 0,
                    progress, ARROW_HEIGHT,
                    ARROW_WIDTH, ARROW_HEIGHT);
        }

        guiGraphics.pose().popPose();
    }






    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 72, 0x404040, false);
    }
}