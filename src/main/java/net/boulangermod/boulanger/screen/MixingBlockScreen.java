package net.boulangermod.boulanger.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.network.StartMixingPacket;
import net.boulangermod.boulanger.util.IngredientStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.awt.*;
import java.util.List;

public class MixingBlockScreen extends AbstractContainerScreen<MixingBlockMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/mixer.png");

    public MixingBlockScreen(MixingBlockMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 72;
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;

        // GUI origin
        int x0 = (this.width - this.imageWidth) / 2;
        int y0 = (this.height - this.imageHeight) / 2;

        // button size
        int btnW = 40;
        int btnH = 18;
        // center X within the GUI
        int btnX = x0 + (this.imageWidth / 2) - (btnW / 2);
        // desired Y offset (tweak as needed)
        int btnY = y0 + 60;

        Button mixButton = Button.builder(Component.literal("Mix"), btn -> {
                    StartMixingPacket.sendFromClient(menu.getBlockEntity().getBlockPos());
                })
                .bounds(btnX, btnY, btnW, btnH)
                .build();

        this.addRenderableWidget(mixButton);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        // origin of mixer GUI
        int x0 = (this.width  - this.imageWidth)  / 2;
        int y0 = (this.height - this.imageHeight) / 2;

        // 1) draw your 1024×1024 texture at 1/4 scale
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x0, y0, 0);
        guiGraphics.pose().scale(0.25f, 0.25f, 1.0f);
        guiGraphics.blit(GUI_TEXTURE, 0, 0, 0, 0, 1024, 1024, 1024, 1024);
        guiGraphics.pose().popPose();

        // 2) panel metrics
        int count      = menu.getIngredientCount();
        int rows       = (count + 1) / 2;       // two columns
        int iconSize   = 16;
        int vertSpace  = 18;                    // same vertical step you had
        int colSpace   = 8;                     // extra horizontal gap
        int padding    = 4;
        int panelWidth = padding * 2 + iconSize * 2 + colSpace;

        // panel covers full GUI height
        int panelX1 = x0;
        int panelX0 = panelX1 - panelWidth;
        int panelY0 = y0;
        int panelY1 = y0 + this.imageHeight;

        // 3) draw solid background (C6C6C6)
        guiGraphics.fill(panelX0, panelY0, panelX1, panelY1, 0xFFC6C6C6);

        // 4) render each icon + count
        for (int i = 0; i < count; i++) {
            IngredientStack ingr = menu.getIngredient(i);
            ItemStack stack      = ingr.getBowlStack().copy();

            int col = i % 2;
            int row = i / 2;

            int iconX = panelX0 + padding + col * (iconSize + colSpace);
            int iconY = panelY0 + padding + row * vertSpace;

            guiGraphics.renderItem(stack, iconX, iconY);
            guiGraphics.renderItemDecorations(
                    this.font,
                    stack,
                    iconX,
                    iconY,
                    ingr.getGrams() + "g"
            );
        }

        // (…any other progress bars, etc.…)
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        // draw background dimming
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        // draw GUI and slots
        super.render(guiGraphics, mouseX, mouseY, delta);

        // --- custom ingredient‐tooltip logic ---
        int x0 = (this.width  - this.imageWidth)  / 2;
        int y0 = (this.height - this.imageHeight) / 2;
        int iconSize   = 16;
        int vertSpace  = 18;
        int colSpace   = 8;
        int padding    = 4;
        int panelWidth = padding * 2 + iconSize * 2 + colSpace;
        int panelX0    = x0 - panelWidth;

        for (int i = 0; i < menu.getIngredientCount(); i++) {
            int col = i % 2;
            int row = i / 2;

            int iconX = panelX0 + padding + col * (iconSize + colSpace);
            int iconY = y0 + padding + row * vertSpace;
            // check if mouse is over this icon
            if (mouseX >= iconX && mouseX < iconX + iconSize
                    && mouseY >= iconY && mouseY < iconY + iconSize) {
                IngredientStack ingr = menu.getIngredient(i);
                ItemStack stack      = ingr.getBowlStack();
                int grams            = ingr.getGrams();

                List<Component> tip = List.of(
                        stack.getHoverName(),
                        Component.literal(grams + " g")
                );
                guiGraphics.renderComponentTooltip(this.font, tip, mouseX, mouseY);

                break;  // only one tooltip at a time
            }
        }

        // then default slot/tooltips
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 72, 0x404040, false);
    }

}

