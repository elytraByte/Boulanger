package net.boulangermod.boulanger.multiblock;


import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.Level;

/**
 * Utility for rendering multiblocks in manuals or previews.
 * This is extremely lightweight compared to Immersive Engineering's implementation
 * but provides a starting point for custom render logic.
 */
public class ClientMultiblockRenderer {

    private ClientMultiblockRenderer() {}

    public static void render(IMultiblock multiblock, PoseStack pose, Level level) {
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        pose.pushPose();
        multiblock.renderFormedStructure(pose, level);
        pose.popPose();
        buffer.endBatch(RenderType.solid());
    }
}