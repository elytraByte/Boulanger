// src/main/java/net/boulangermod/boulanger/screen/DoughDividerScreen.java
package net.boulangermod.boulanger.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.network.SetDividerModePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class DoughDividerScreen extends AbstractContainerScreen<DoughDividerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/dough_divider.png");

    private enum DivideMode { NONE, LOAF, ROLL }

    // Button layout (relative to the screen's left/top; i.e., add leftPos/topPos)
    private static final int BTN_W = 22; // 22×22 to avoid 1px cutoff
    private static final int BTN_H = 22;

    // Draw positions (on the right edge)
    private static final int ROLL_BTN_X = 143;
    private static final int ROLL_BTN_Y = 21;

    private static final int LOAF_BTN_X = 143;
    private static final int LOAF_BTN_Y = 45;

    // Texture UVs for the states
    // Columns: Loaf x=176; Roll x=198
    // Rows: hover(blue)=y=0, pressed(selected)=y=22, default=y=44
    private static final int LOAF_U_HOVER = 176;
    private static final int LOAF_V_HOVER = 0;
    private static final int LOAF_U_PRESSED = 176;
    private static final int LOAF_V_PRESSED = 22;
    private static final int LOAF_U_DEFAULT = 176;
    private static final int LOAF_V_DEFAULT = 44;

    private static final int ROLL_U_HOVER = 198;
    private static final int ROLL_V_HOVER = 0;
    private static final int ROLL_U_PRESSED = 198;
    private static final int ROLL_V_PRESSED = 22;
    private static final int ROLL_U_DEFAULT = 198;
    private static final int ROLL_V_DEFAULT = 44;

    // Neutral by default (both buttons shown normal; BE won’t process until user picks)
    private DivideMode selectedMode = DivideMode.NONE;

    public DoughDividerScreen(DoughDividerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // If your menu exposes BE state, you can reflect it here:
        // var be = menu.getBlockEntity();
        // if (be != null && be.isModeSelected()) {
        //     this.selectedMode = be.isRollMode() ? DivideMode.ROLL : DivideMode.LOAF;
        // } else {
        //     this.selectedMode = DivideMode.NONE;
        // }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // Main background
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // --- Draw our two buttons ---
        final boolean rollHover = isHovering(ROLL_BTN_X, ROLL_BTN_Y, BTN_W, BTN_H, mouseX, mouseY);
        final boolean loafHover = isHovering(LOAF_BTN_X, LOAF_BTN_Y, BTN_W, BTN_H, mouseX, mouseY);

        // Roll button state
        if (rollHover) {
            blitButton(g, ROLL_BTN_X, ROLL_BTN_Y, ROLL_U_HOVER, ROLL_V_HOVER);
        } else if (selectedMode == DivideMode.ROLL) {
            blitButton(g, ROLL_BTN_X, ROLL_BTN_Y, ROLL_U_PRESSED, ROLL_V_PRESSED);
        } else {
            blitButton(g, ROLL_BTN_X, ROLL_BTN_Y, ROLL_U_DEFAULT, ROLL_V_DEFAULT);
        }

        // Loaf button state
        if (loafHover) {
            blitButton(g, LOAF_BTN_X, LOAF_BTN_Y, LOAF_U_HOVER, LOAF_V_HOVER);
        } else if (selectedMode == DivideMode.LOAF) {
            blitButton(g, LOAF_BTN_X, LOAF_BTN_Y, LOAF_U_PRESSED, LOAF_V_PRESSED);
        } else {
            blitButton(g, LOAF_BTN_X, LOAF_BTN_Y, LOAF_U_DEFAULT, LOAF_V_DEFAULT);
        }
    }

    private void blitButton(GuiGraphics g, int xOff, int yOff, int u, int v) {
        g.blit(TEXTURE, leftPos + xOff, topPos + yOff, u, v, BTN_W, BTN_H);
    }

    private boolean isHovering(int relX, int relY, int w, int h, int mouseX, int mouseY) {
        final int x = leftPos + relX;
        final int y = topPos + relY;
        return mouseX >= x && mouseX < (x + w) && mouseY >= y && mouseY < (y + h);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        // Tooltips for the buttons
        if (isHovering(ROLL_BTN_X, ROLL_BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal("Roll"), mouseX, mouseY);
        } else if (isHovering(LOAF_BTN_X, LOAF_BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal("Loaf"), mouseX, mouseY);
        }

        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, imageHeight - 94, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // left click
            if (isHovering(ROLL_BTN_X, ROLL_BTN_Y, BTN_W, BTN_H, (int) mouseX, (int) mouseY)) {
                setSelectedMode(DivideMode.ROLL);
                return true;
            }
            if (isHovering(LOAF_BTN_X, LOAF_BTN_Y, BTN_W, BTN_H, (int) mouseX, (int) mouseY)) {
                setSelectedMode(DivideMode.LOAF);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playClick() {
        // If your mappings require, switch to SoundEvents.UI_BUTTON_CLICK.value()
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void setSelectedMode(DivideMode mode) {
        if (this.selectedMode == mode) return;
        this.selectedMode = mode;

        // click!
        playClick();

        // send to server so BE learns the selection
        PacketDistributor.sendToServer(
                new SetDividerModePacket(
                        menu.getBlockEntity().getBlockPos(),
                        mode == DivideMode.ROLL
                )
        );
    }
}
