package net.boulangermod.boulanger.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.boulangermod.boulanger.Boulanger;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class WoodGasifierScreen extends AbstractContainerScreen<WoodGasifierMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/wood_gasifier.png");

    private static final ResourceLocation BUBBLES_TEX =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/bubbles.png");  // 12x29
    private static final ResourceLocation WOODGAS_TEX =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/woodgas.png");  // 7x62

    // Layout
    private static final int BURN_X = 94,  BURN_Y = 21, BURN_W = 14, BURN_H = 50;

    // Woodgas gauge area (exactly 62 px tall): bottom lowered by 1px → 77; top stays 15
    private static final int GAS_X = 156;
    private static final int GAS_Y_BOTTOM = 77;     // was 76
    private static final int GAS_Y_TOP    = 15;     // stays 15
    private static final int GAS_W = 7;
    private static final int GAS_TEX_W = 7, GAS_TEX_H = 62;

    // Bubbles position + texture size
    private static final int BUB_X = 136, BUB_Y = 46, BUB_W = 12, BUB_H = 29;

    public WoodGasifierScreen(WoodGasifierMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 172;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = leftPos, y = topPos;

        // Background
        gfx.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Horizontal burn arrow (kept)
        int maxBurn = menu.getMaxBurnProgress();
        int elapsed = maxBurn > 0 ? menu.getBurnProgress() : 0; // our menu exposes elapsed (0..max)
        int progW   = maxBurn > 0 ? (elapsed * 24 / maxBurn) : 0;
        gfx.blit(TEXTURE, x + 56, y + 37, 176, 0, progW + 1, 16);

        // Vertical burn bar (top → bottom)
        int burnFill = maxBurn > 0 ? (elapsed * BURN_H / maxBurn) : 0;
        if (burnFill > 0) {
            gfx.blit(TEXTURE, x + BURN_X, y + BURN_Y, 176, 16, BURN_W, burnFill);
        }

        // Energy bar (bottom → top)
        int maxEnergy = menu.getMaxEnergyStored();
        int energy    = maxEnergy > 0 ? menu.getEnergyStored() * 50 / maxEnergy : 0;
        gfx.blit(TEXTURE, x + 150, y + 15 + (50 - energy), 176, 16 + (50 - energy), 14, energy);

        // Woodgas level — 1:1 draw from woodgas.png (no scaling), bottom anchored at y=77
        int gasCap = Math.max(menu.getGasCapacity(), 0);
        int gasAmt = Math.min(Math.max(menu.getGasAmount(), 0), gasCap);
        int gasFillTex = (gasCap > 0) ? (gasAmt * GAS_TEX_H / gasCap) : 0; // 0..62 texture pixels

        if (gasFillTex > 0) {
            int srcV  = GAS_TEX_H - gasFillTex;            // read from bottom upward
            int destY = y + (GAS_Y_BOTTOM - gasFillTex);   // draw from bottom upward
            RenderSystem.setShaderTexture(0, WOODGAS_TEX);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            gfx.blit(WOODGAS_TEX,
                    x + GAS_X, destY,
                    0,                 // blitOffset
                    0, srcV,           // u, v
                    GAS_W, gasFillTex, // width, height to draw (1:1)
                    GAS_TEX_W, GAS_TEX_H); // texture size (7x62)
            RenderSystem.disableBlend();
            RenderSystem.setShaderTexture(0, TEXTURE);
        }

        // Bubbles like a furnace flame:
        // Reveal from TOP downward, proportional to *elapsed* (as burnTime counts down).
        if (elapsed > 0 && maxBurn > 0) {
            int bubbleFill = (elapsed * BUB_H) / maxBurn;   // 0..29
            if (bubbleFill > 0) {
                RenderSystem.setShaderTexture(0, BUBBLES_TEX);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                // draw the TOP slice (u=0,v=0) with height=bubbleFill
                gfx.blit(BUBBLES_TEX,
                        x + BUB_X, y + BUB_Y,
                        0,            // blitOffset
                        0, 0,         // u, v start at top of texture
                        BUB_W, bubbleFill,
                        BUB_W, BUB_H);
                RenderSystem.disableBlend();
                RenderSystem.setShaderTexture(0, TEXTURE);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        int titleWidth = this.font.width(this.title);
        int titleX = (this.imageWidth - titleWidth) / 2;
        gfx.drawString(this.font, this.title, titleX, 6, 0x404040, false);

        // Inventory label aligned with slots (slots were shifted down by 1px in the Menu)
        gfx.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY + 6, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gfx, mouseX, mouseY, partialTicks);
        super.render(gfx, mouseX, mouseY, partialTicks);

        // ── Tooltip over the woodgas gauge
        int gx0 = leftPos + GAS_X, gx1 = gx0 + GAS_W;
        int gy0 = topPos + GAS_Y_TOP, gy1 = topPos + GAS_Y_BOTTOM;
        if (mouseX >= gx0 && mouseX < gx1 && mouseY >= gy0 && mouseY < gy1) {
            int cap = Math.max(menu.getGasCapacity(), 0);
            int amt = Math.min(Math.max(menu.getGasAmount(), 0), cap);
            // If you have a lang key for the fluid, use that; otherwise this literal is fine.
            Component tip = Component.literal(amt + "/" + cap + " mB " + "Wood Gas");
            gfx.renderTooltip(this.font, tip, mouseX, mouseY);
        }

        this.renderTooltip(gfx, mouseX, mouseY);
    }
}
