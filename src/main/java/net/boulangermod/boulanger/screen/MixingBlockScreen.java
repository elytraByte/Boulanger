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

import java.util.List;

public class MixingBlockScreen extends AbstractContainerScreen<MixingBlockMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/mixer.png");

    // sidebar constants
    private static final int COLUMNS   = 3;
    private static final int ICON_SZ   = 16;
    private static final int VERT_SP   = 18;
    private static final int COL_SP    = 12;
    private static final int PADDING   = 4;

    public MixingBlockScreen(MixingBlockMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 72;
        this.titleLabelX     = (this.imageWidth - this.font.width(this.title)) / 2;

        int x0 = (this.width  - this.imageWidth)  / 2;
        int y0 = (this.height - this.imageHeight) / 2;
        int btnW = 40, btnH = 18;

        Button mixBtn = Button.builder(Component.literal("Mix"), b ->
                        StartMixingPacket.sendFromClient(menu.getBlockEntity().getBlockPos())
                )
                .bounds(x0 + this.imageWidth/2 - btnW/2, y0 + 60, btnW, btnH)
                .build();

        this.addRenderableWidget(mixBtn);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        // 1) draw main GUI scaled 1/4
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f,1f,1f,1f);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x0 = (this.width  - this.imageWidth)  / 2;
        int y0 = (this.height - this.imageHeight) / 2;

        g.pose().pushPose();
        g.pose().translate(x0, y0, 0);
        g.pose().scale(0.25f, 0.25f, 1f);
        g.blit(GUI_TEXTURE, 0, 0, 0, 0, 1024, 1024, 1024, 1024);
        g.pose().popPose();

        // 2) sidebar panel
        int count  = menu.getIngredientCount();
        int panelW = PADDING*2 + ICON_SZ*COLUMNS + COL_SP*(COLUMNS - 1);
        int px1    = x0;
        int px0    = px1 - panelW;
        int py0    = y0;
        int py1    = y0 + this.imageHeight;
        g.fill(px0, py0, px1, py1, 0xFFC6C6C6);

        // 3) render icons + overlay text
        for (int i = 0; i < count; i++) {
            IngredientStack ingr = menu.getIngredient(i);
            ItemStack       stack = ingr.getBowlStack().copy();
            int             col   = i % COLUMNS;
            int             row   = i / COLUMNS;
            int iconX = px0 + PADDING + col*(ICON_SZ+COL_SP);
            int iconY = py0 + PADDING + row*VERT_SP;

            // draw icon
            g.renderItem(stack, iconX, iconY);
            // overlay grams on top
            g.renderItemDecorations(
                    this.font,
                    stack,
                    iconX,
                    iconY,
                    ingr.getGrams() + "g"
            );
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        // background and GUI + sidebar from renderBg
        this.renderBackground(g, mx, my, dt);
        super.render(g, mx, my, dt);

        // ingredient tooltip
        int x0 = (this.width  - this.imageWidth)  / 2;
        int y0 = (this.height - this.imageHeight) / 2;
        int count  = menu.getIngredientCount();
        int panelW = PADDING*2 + ICON_SZ*COLUMNS + COL_SP*(COLUMNS - 1);
        int px0    = x0 - panelW;

        for (int i = 0; i < count; i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int iconX = px0 + PADDING + col*(ICON_SZ+COL_SP);
            int iconY = y0 + PADDING + row*VERT_SP;

            if (mx >= iconX && mx < iconX + ICON_SZ
                    && my >= iconY && my < iconY + ICON_SZ) {
                IngredientStack ingr = menu.getIngredient(i);
                String      name = ingr.getBowlStack().getHoverName().getString();
                int         grams = ingr.getGrams();

                Component line1 = Component.literal("\u00A7a" + name);
                Component line2 = Component.literal("\u00A7a" + grams + " g");
                g.renderComponentTooltip(this.font, List.of(line1, line2), mx, my);
                break;
            }
        }

        // default slot tooltips
        this.renderTooltip(g, mx, my);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, 72, 0x404040, false);
    }
}
