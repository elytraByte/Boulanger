package net.boulangermod.boulanger.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.boulangermod.boulanger.Boulanger;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class WoodGasEngineBlockScreen extends AbstractContainerScreen<WoodGasEngineBlockMenu> {

    // ── textures ───────────────────────────────────────────────────────────────
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/woodgas_engine.png");
    private static final ResourceLocation FLAME_TEX =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/lit_progress.png"); // 14×14
    private static final ResourceLocation BUBBLES_TEX =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/bubbles.png");      // 12×29

    // Gauge texture (7×62)
    private static final ResourceLocation WOODGAS_TEX =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/woodgas.png");

    // ── background size ────────────────────────────────────────────────────────
    private static final int BG_W = 176;
    private static final int BG_H = 166;

    // ── flame (unchanged) ─────────────────────────────────────────────────────
    private static final int FLAME_X = 80, FLAME_Y = 51, FLAME_W = 14, FLAME_H = 14;

    // ── bubbles (unchanged) ───────────────────────────────────────────────────
    private static final int BUB_X = 136, BUB_Y = 46, BUB_W = 12, BUB_H = 29;

    // ── woodgas gauge (unchanged) ─────────────────────────────────────────────
    private static final int GAS_X = 156, GAS_Y_TOP = 15, GAS_Y_BOTTOM = 77, GAS_W = 7, GAS_H = 62;

    public WoodGasEngineBlockScreen(WoodGasEngineBlockMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_W;
        this.imageHeight = BG_H;
        this.inventoryLabelY = 10000; // hide vanilla labels
        this.titleLabelY = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        final int x = leftPos, y = topPos;

        // Background
        RenderSystem.setShaderTexture(0, TEXTURE);
        gfx.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // 🔥 Draw lit progress only when the engine is burning
        if (menu.isBurning()) {
            float elapsed = Mth.clamp(menu.getBurnPercent(), 0f, 1f);

            // FLAME (full at start, shrinks TOP→DOWN)
            int flameH = Math.round(FLAME_H * (1f - elapsed));
            if (flameH > 0) {
                int srcV  = FLAME_H - flameH;
                int destY = y + FLAME_Y + srcV;
                RenderSystem.setShaderTexture(0, FLAME_TEX);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                gfx.blit(FLAME_TEX, x + FLAME_X, destY, 0, srcV, FLAME_W, flameH, FLAME_W, FLAME_H);
                RenderSystem.disableBlend();
            }

            // BUBBLES (reveal from TOP downward)
            int bubbleFill = Math.round(elapsed * BUB_H);
            if (bubbleFill > 0) {
                RenderSystem.setShaderTexture(0, BUBBLES_TEX);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                gfx.blit(BUBBLES_TEX, x + BUB_X, y + BUB_Y, 0, 0, BUB_W, bubbleFill, BUB_W, BUB_H);
                RenderSystem.disableBlend();
            }

            // restore main sheet if needed later
            RenderSystem.setShaderTexture(0, TEXTURE);
        }

        // Gauge always draws (independent of burning)
        int cap = Math.max(menu.getGasCapacity(), 0);
        int amt = Math.min(Math.max(menu.getGasAmount(), 0), cap);
        int gasFill = (cap > 0) ? (amt * GAS_H / cap) : 0;
        if (gasFill > 0) {
            int srcV  = GAS_H - gasFill;
            int destY = y + (GAS_Y_BOTTOM - gasFill);
            RenderSystem.setShaderTexture(0, WOODGAS_TEX);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            gfx.blit(WOODGAS_TEX, x + GAS_X, destY, 0, srcV, GAS_W, gasFill, GAS_W, GAS_H);
            RenderSystem.disableBlend();
            RenderSystem.setShaderTexture(0, TEXTURE);
        }
    }

    private void renderGasTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
        int gx0 = leftPos + GAS_X, gx1 = gx0 + GAS_W;
        int gy0 = topPos + GAS_Y_TOP, gy1 = topPos + GAS_Y_BOTTOM;
        if (mouseX >= gx0 && mouseX < gx1 && mouseY >= gy0 && mouseY < gy1) {
            int cap = Math.max(menu.getGasCapacity(), 0);
            int amt = Math.min(Math.max(menu.getGasAmount(), 0), cap);
            gfx.renderTooltip(this.font, Component.literal(amt + "/" + cap + " mB Wood Gas"), mouseX, mouseY);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gfx, mouseX, mouseY, partialTicks);
        super.render(gfx, mouseX, mouseY, partialTicks);
        renderGasTooltip(gfx, mouseX, mouseY);
    }
}
