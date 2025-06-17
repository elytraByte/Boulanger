package net.boulangermod.boulanger.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.network.BoulangerNetwork;
import net.boulangermod.boulanger.network.MeasureItemData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MilligramScaleScreen extends AbstractContainerScreen<MilligramScaleMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/scale.png");

    private EditBox weightInput;

    public MilligramScaleScreen(MilligramScaleMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth  = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // center labels exactly as block-scale
        this.titleLabelX     = (this.imageWidth - this.font.width(this.title)) / 2;
        this.inventoryLabelY = this.imageHeight - 94;

        int tfW = 70, tfH = 20;
        int btnW = 50, btnH = 20;
        int textFieldX = this.leftPos + this.imageWidth - tfW - 8;
        int textFieldY = this.topPos + 20;

        // “mg” input box
        this.weightInput = new EditBox(
                this.font, textFieldX, textFieldY, tfW, tfH,
                Component.literal("mg")
        );
        this.weightInput.setMaxLength(8);
        this.weightInput.setValue("");
        addRenderableWidget(this.weightInput);

        // “Measure” button
        int buttonX = textFieldX + (tfW - btnW) / 2;
        int buttonY = textFieldY + tfH + 6;
        addRenderableWidget(Button.builder(Component.literal("Measure"), btn -> {
                    String text = weightInput.getValue();
                    try {
                        int mg = Integer.parseInt(text);
                        MeasureItemData.sendFromClient(menu.containerId, mg);
                    } catch (NumberFormatException e) {
                        this.minecraft.player.sendSystemMessage(
                                Component.literal("Invalid weight: “" + text + "”")
                        );
                    }
                })
                .pos(buttonX, buttonY)
                .size(btnW, btnH)
                .build());
    }

    /** Match your other screen: translucent backdrop + GUI texture */
    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        this.renderTransparentBackground(gui);
        this.renderBg(gui, partialTicks, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        gui.blit(GUI_TEXTURE,
                this.leftPos, this.topPos,
                0, 0,
                this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        // uses your 4-arg override above
        this.renderBackground(gui, mouseX, mouseY, partialTicks);
        super.render(gui, mouseX, mouseY, partialTicks);
        this.renderTooltip(gui, mouseX, mouseY);
        this.weightInput.render(gui, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title,
                this.titleLabelX, this.titleLabelY,
                0x404040, false);
        gui.drawString(this.font, this.playerInventoryTitle,
                8, this.inventoryLabelY,
                0x404040, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int mods) {
        if (this.weightInput.keyPressed(keyCode, scanCode, mods)) return true;
        return super.keyPressed(keyCode, scanCode, mods);
    }

    @Override
    public boolean charTyped(char codePoint, int mods) {
        if (this.weightInput.charTyped(codePoint, mods)) return true;
        return super.charTyped(codePoint, mods);
    }
}
