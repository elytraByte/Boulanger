package net.boulangermod.boulanger.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.network.MeasureData;
import net.boulangermod.boulanger.network.BoulangerNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ScaleBlockScreen extends AbstractContainerScreen<ScaleBlockMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/scale.png");

    private EditBox weightInput;

    public ScaleBlockScreen(ScaleBlockMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        // standard 176×166 GUI
        this.imageWidth  = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        // center the title
        this.titleLabelX    = (this.imageWidth - this.font.width(this.title)) / 2;
        this.inventoryLabelY = this.imageHeight - 94;

        int tfW = 70, tfH = 20;
        int btnW = 50, btnH = 20;

        // position widgets in right-hand margin
        int textFieldX = this.leftPos + this.imageWidth - tfW - 8;
        int textFieldY = this.topPos + 20;
        this.weightInput = new EditBox(
                this.font, textFieldX, textFieldY, tfW, tfH,
                Component.literal("Weight")
        );
        this.weightInput.setMaxLength(9);
        this.weightInput.setValue("");
        addRenderableWidget(this.weightInput);

        int buttonX = textFieldX + (tfW - btnW) / 2;
        int buttonY = textFieldY + tfH + 6;
        Button measureButton = Button.builder(Component.literal("Measure"), btn -> {
                    String text = weightInput.getValue();
                    try {
                        int weight = Integer.parseInt(text);
                        BoulangerNetwork.sendToServer(
                                new MeasureData(weight, menu.getBlockEntity().getBlockPos())
                        );
                    } catch (NumberFormatException e) {
                        this.minecraft.player.sendSystemMessage(
                                Component.literal("Invalid weight: “" + text + "”"));
                    }
                })
                .pos(buttonX, buttonY)
                .size(btnW, btnH)
                .build();
        addRenderableWidget(measureButton);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float pt, int mx, int my) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        gui.blit(GUI_TEXTURE, this.leftPos, this.topPos,
                0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float delta) {
        renderBackground(gui, mx, my, delta);
        super.render(gui, mx, my, delta);
        renderTooltip(gui, mx, my);
        this.weightInput.render(gui, mx, my, delta);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mx, int my) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        gui.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
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
