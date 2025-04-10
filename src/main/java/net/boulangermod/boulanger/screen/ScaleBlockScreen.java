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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ScaleBlockScreen extends AbstractContainerScreen<ScaleBlockMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/scale.png");
    private EditBox weightInput;

    public ScaleBlockScreen(ScaleBlockMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        // 'leftPos' and 'topPos' represent where your GUI background starts on the screen.
        // imageWidth and imageHeight default to 176×166 for most ContainerScreens, but confirm in your code.

        // Reposition labels as needed
        this.inventoryLabelY = 72;
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;

        // Position the text box (EditBox) about 10px from the left edge and 10px from the top.
        int textFieldX = this.leftPos + 10;
        int textFieldY = this.topPos + 20;
        int textFieldWidth = 70;   // narrower than 100 so it fits comfortably
        int textFieldHeight = 20;

        this.weightInput = new EditBox(
                this.font,
                textFieldX,
                textFieldY,
                textFieldWidth,
                textFieldHeight,
                Component.literal("Weight")
        );
        this.weightInput.setMaxLength(9); // up to 5 characters
        this.weightInput.setValue("");    // start empty
        addRenderableWidget(this.weightInput);

        // Build a "Measure" button to the right of the text box, with a small gap.
        int buttonX = textFieldX + textFieldWidth + 5;
        int buttonY = textFieldY;
        int buttonWidth = 50;
        int buttonHeight = 20;

        Button measureButton = Button.builder(Component.literal("Measure"), button -> {
                    // OnPress logic
                    try {
                        int weight = Integer.parseInt(weightInput.getValue());
                        BlockPos pos = this.menu.getBlockEntity().getBlockPos();
                        MeasureData payload = new MeasureData(weight, pos);
                        BoulangerNetwork.sendToServer(payload);
                    } catch (NumberFormatException e) {
                        weightInput.setValue("0");
                    }
                })
                .pos(buttonX, buttonY)
                .size(buttonWidth, buttonHeight)
                .build();

        this.addRenderableWidget(measureButton);
    }


    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.pose().pushPose();
        // Apply a scale transformation if needed (using 0.25 here to match your background's design).
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(0.25f, 0.25f, 1.0f);

        // Render the main GUI background.
        guiGraphics.blit(GUI_TEXTURE, 0, 0, 0.0f, 0.0f, 1024, 1024, 1024, 1024);
        guiGraphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
        // Render the EditBox on top of the GUI.
        this.weightInput.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 72, 0x404040, false);
    }

    // Let the EditBox process key input.
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.weightInput.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // Let the EditBox process character typing.
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.weightInput.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }
}
