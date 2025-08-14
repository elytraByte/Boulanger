package net.boulangermod.boulanger.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.boulangermod.boulanger.Boulanger;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SugarRefineryScreen extends AbstractContainerScreen<SugarRefineryMenu> {
    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/sugar_refinery.png");
    private static final ResourceLocation LIT_FLAME =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/lit_progress.png");             // 14x14
    private static final ResourceLocation VERTICAL_PROGRESS =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/sugar_refinery_progress.png");  // 9x28

    // sprite sizes
    private static final int FIRE_W = 14, FIRE_H = 14;
    private static final int ARROW_W = 9,  ARROW_H = 28;

    // anchors you requested
    private static final int FIRE_X = 26, FIRE_Y = 51;
    private static final int ARROW_X = 96, ARROW_Y = 16;

    public SugarRefineryScreen(SugarRefineryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX     = (this.imageWidth - this.font.width(this.title)) / 2;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        // 1) background
        gfx.blit(BG, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // 2) overlays
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // -- flame: shrink downward as fuel depletes --
        int lit = this.menu.getBurnProgressScaled(FIRE_H); // 0..14
        if (lit > 0) {
            int cropTop = FIRE_H - lit; // crop from top
            // IMPORTANT: use the blit overload with texWidth/texHeight for small textures
            gfx.blit(
                    LIT_FLAME,
                    this.leftPos + FIRE_X,
                    this.topPos  + FIRE_Y + cropTop,
                    0,                 // blitZ
                    0, cropTop,        // u, v
                    FIRE_W, lit,       // draw width/height
                    FIRE_W, FIRE_H     // texture size (prevents sampling from BG)
            );
        }

        // -- vertical craft bar: fill downward --
        int cook = this.menu.getCookProgressScaled(ARROW_H); // 0..28
        if (cook > 0) {
            gfx.blit(
                    VERTICAL_PROGRESS,
                    this.leftPos + ARROW_X,
                    this.topPos  + ARROW_Y,
                    0,                 // blitZ
                    0, 0,              // u, v
                    ARROW_W, cook,     // draw width/height
                    ARROW_W, ARROW_H   // texture size
            );
        }

        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        gfx.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gfx, mouseX, mouseY, partialTicks);
        super.render(gfx, mouseX, mouseY, partialTicks);
        this.renderTooltip(gfx, mouseX, mouseY);
    }
}
