package net.boulangermod.boulanger.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ScaleBlockScreen
        extends AbstractContainerScreen<ScaleBlockMenu> {

    private EditBox milligramInput;

    public ScaleBlockScreen(
            ScaleBlockMenu menu,
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
                        "screen.boulanger.scale.milligrams"
                )
        );

        milligramInput.setMaxLength(10);
        milligramInput.setFilter(
                value -> value.isEmpty()
                        || value.chars().allMatch(Character::isDigit)
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
            showError("Enter a weight in milligrams");
            return;
        }

        final long requestedMilligrams;

        try {
            requestedMilligrams = Long.parseLong(input);
        } catch (NumberFormatException exception) {
            showError("The entered weight is invalid");
            return;
        }

        if (requestedMilligrams <= 0L) {
            showError("Weight must be greater than zero");
            return;
        }

        if (requestedMilligrams > Integer.MAX_VALUE) {
            showError("Requested weight is too large");
            return;
        }

        if (minecraft == null || minecraft.gameMode == null) {
            return;
        }

        minecraft.gameMode.handleInventoryButtonClick(
                menu.containerId,
                (int) requestedMilligrams
        );
    }

    private void showError(String message) {
        if (minecraft != null && minecraft.player != null) {
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
        int left = leftPos;
        int top = topPos;

        graphics.fill(
                left,
                top,
                left + imageWidth,
                top + imageHeight,
                0xFFC6C6C6
        );

        graphics.fill(
                left,
                top,
                left + imageWidth,
                top + 1,
                0xFFFFFFFF
        );

        graphics.fill(
                left,
                top,
                left + 1,
                top + imageHeight,
                0xFFFFFFFF
        );

        graphics.fill(
                left,
                top + imageHeight - 1,
                left + imageWidth,
                top + imageHeight,
                0xFF555555
        );

        graphics.fill(
                left + imageWidth - 1,
                top,
                left + imageWidth,
                top + imageHeight,
                0xFF555555
        );

        drawSlot(graphics, left + 26, top + 17);
        drawSlot(graphics, left + 62, top + 17);
        drawSlot(graphics, left + 26, top + 53);
        drawSlot(graphics, left + 62, top + 53);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(
                        graphics,
                        left + 8 + column * 18,
                        top + 84 + row * 18
                );
            }
        }

        for (int column = 0; column < 9; column++) {
            drawSlot(
                    graphics,
                    left + 8 + column * 18,
                    top + 142
            );
        }
    }

    private static void drawSlot(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(
                x - 1,
                y - 1,
                x + 17,
                y + 17,
                0xFF666666
        );

        graphics.fill(
                x,
                y,
                x + 16,
                y + 16,
                0xFFEEEEEE
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
                        "screen.boulanger.scale.milligrams"
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