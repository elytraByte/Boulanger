package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.item.MilligramScaleItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class MilligramScaleScreen
        extends AbstractContainerScreen<MilligramScaleMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    Boulanger.MOD_ID,
                    "textures/gui/scale.png"
            );

    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    private EditBox milligramInput;

    public MilligramScaleScreen(
            MilligramScaleMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);

        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        milligramInput = new EditBox(
                font,
                leftPos + 98,
                topPos + 19,
                68,
                18,
                Component.translatable(
                        "screen.boulanger.milligram_scale.milligrams"
                )
        );

        milligramInput.setMaxLength(4);
        milligramInput.setFilter(
                value -> value.isEmpty()
                        || value.chars()
                        .allMatch(Character::isDigit)
        );

        addRenderableWidget(milligramInput);

        addRenderableWidget(
                Button.builder(
                                Component.translatable(
                                        "screen.boulanger.scale.measure"
                                ),
                                button -> submitMeasurement()
                        )
                        .bounds(
                                leftPos + 107,
                                topPos + 46,
                                50,
                                20
                        )
                        .build()
        );
    }

    private void submitMeasurement() {
        String input = milligramInput.getValue();

        if (input.isBlank()) {
            showError(
                    "Enter a weight from 1 to 5000 mg"
            );
            return;
        }

        final int requestedMilligrams;

        try {
            requestedMilligrams =
                    Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            showError("The entered weight is invalid");
            return;
        }

        if (requestedMilligrams <= 0
                || requestedMilligrams
                > MilligramScaleItem
                .MAX_CAPACITY_MILLIGRAMS) {
            showError(
                    "Milligram scale capacity is 5000 mg"
            );
            return;
        }

        if (minecraft == null
                || minecraft.gameMode == null) {
            return;
        }

        minecraft.gameMode.handleInventoryButtonClick(
                menu.containerId,
                requestedMilligrams
        );
    }

    private void showError(String message) {
        if (minecraft != null
                && minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.literal(message)
                            .withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        graphics.blit(
                GUI_TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        graphics.drawString(
                font,
                title,
                8,
                6,
                0x404040,
                false
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "screen.boulanger.milligram_scale.milligrams"
                ),
                98,
                7,
                0x404040,
                false
        );

        graphics.drawString(
                font,
                playerInventoryTitle,
                8,
                72,
                0x404040,
                false
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        renderTooltip(graphics, mouseX, mouseY);
    }
}