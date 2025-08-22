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
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/lit_progress.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/arrow_progress.png");

    // Native sprite sizes
    private static final int FLAME_WIDTH  = 14;
    private static final int FLAME_HEIGHT = 14;
    private static final int ARROW_WIDTH  = 87;
    private static final int ARROW_HEIGHT = 60;

    // Origins in the 1024×1024 background (pre-scale coordinates)
    private static final int FLAME_X_ORIGIN = 224;
    private static final int FLAME_Y_ORIGIN = 144;
    private static final int ARROW_X_ORIGIN = 320;
    private static final int ARROW_Y_ORIGIN = 140;

    // Background scale (1024 → 256)
    private static final float BG_SCALE = 0.25f;

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
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        final int x = (this.width - this.imageWidth) / 2;
        final int y = (this.height - this.imageHeight) / 2;

        // ---- Background at 0.25 scale ----
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        gui.pose().pushPose();
        gui.pose().translate(x, y, 0);
        gui.pose().scale(BG_SCALE, BG_SCALE, 1.0f);
        gui.blit(GUI_TEXTURE, 0, 0, 0f, 0f, 1024, 1024, 1024, 1024);

        // ➡️ Arrow (cooking progress) inside scaled pose
        if (this.menu.isCrafting()) {
            int progress = this.menu.getCookingProgressScaled(ARROW_WIDTH); // 0..ARROW_WIDTH
            RenderSystem.setShaderTexture(0, ARROW_TEXTURE);
            gui.blit(ARROW_TEXTURE,
                    ARROW_X_ORIGIN, ARROW_Y_ORIGIN,
                    0, 0,
                    progress, ARROW_HEIGHT,
                    ARROW_WIDTH, ARROW_HEIGHT);
        }

        gui.pose().popPose();

        // 🔥 Flame (burn progress) OUTSIDE scaled pose to render at native 14×14
        if (this.menu.isLit()) {
            final int flameX = x + Math.round(FLAME_X_ORIGIN * BG_SCALE);
            final int flameY = y + Math.round(FLAME_Y_ORIGIN * BG_SCALE);

            int litPixels = this.menu.getLitProgressScaled(FLAME_HEIGHT); // 0..14
            if (litPixels > 0) {
                RenderSystem.setShaderTexture(0, FLAME_TEXTURE);
                int visibleH = Math.min(litPixels, FLAME_HEIGHT);
                int yOffset  = FLAME_HEIGHT - visibleH;

                gui.blit(FLAME_TEXTURE,
                        flameX, flameY + yOffset,
                        0, yOffset,
                        FLAME_WIDTH, visibleH,
                        FLAME_WIDTH, FLAME_HEIGHT);
            }
        }
    }


    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        renderBackground(gui, mouseX, mouseY, delta);
        super.render(gui, mouseX, mouseY, delta);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        gui.drawString(this.font, this.playerInventoryTitle, 8, 72, 0x404040, false);
    }
}
