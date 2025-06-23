package net.boulangermod.boulanger.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.boulangermod.boulanger.block.entity.WoodGasifierBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public class WoodGasifierRenderer implements BlockEntityRenderer<WoodGasifierBlockEntity> {
    private final Minecraft mc = Minecraft.getInstance();
    private final BakedModel model;

    public WoodGasifierRenderer(BlockEntityRendererProvider.Context ctx) {
        // Use the block-model loader to grab your JSON model
        this.model = mc.getBlockRenderer().getBlockModelShaper()
                .getModelManager()
                .getModel(new ModelResourceLocation(
                        ResourceLocation.fromNamespaceAndPath("boulanger", "wood_gasifier_mb"),
                        ""  // or "inventory", depending on your JSON
                ));
    }

    @Override
    public void render(WoodGasifierBlockEntity be, float partialTicks,
                       PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (!be.isFormed()) return;

        // Find the origin corner of the multiblock
        BlockPos origin = be.getMultiblockOrigin();
        BlockPos here   = be.getBlockPos();
        double dx = origin.getX() - here.getX();
        double dy = origin.getY() - here.getY();
        double dz = origin.getZ() - here.getZ();

        pose.pushPose();
        // translate so that each tile renders the same full model at world-origin
        pose.translate(dx, dy, dz);

        VertexConsumer consumer = buffers.getBuffer(RenderType.solid());
        // renderModel parameters: (Pose, VertexConsumer, blockState, model, red,green,blue, light, overlay)
        mc.getBlockRenderer()
                .getModelRenderer()
                .renderModel(pose.last(),
                        buffers.getBuffer(RenderType.cutout()),
                        null,
                        model,
                        1f,1f,1f,
                        packedLight,
                        packedOverlay);

        pose.popPose();
    }
}
