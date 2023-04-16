package org.l3e.Boulanger.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.l3e.Boulanger.Boulanger;

public class SeparatorScreen extends AbstractContainerScreen<SeparatorMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation((Boulanger.MOD_ID), "textures/gui/separator.png");
    public SeparatorScreen(SeparatorMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(PoseStack stack, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        this.blit(stack, x, y, 0, 0, imageWidth, imageHeight);
        renderProgressArrow(stack, x, y);


    }

    private void renderProgressArrow(PoseStack pPoseStack, int x, int y) {
        if(menu.isCrafting()) {
            blit(pPoseStack, x + 72, y + 33, 176, 0, menu.getScaledProgress(), 23);

        }
    }



    @Override
    public void render(PoseStack poseStack, int i, int i1, float v) {
        renderBackground(poseStack);
        super.render(poseStack, i, i1, v);
        renderTooltip(poseStack,i, i1 );
    }
}
