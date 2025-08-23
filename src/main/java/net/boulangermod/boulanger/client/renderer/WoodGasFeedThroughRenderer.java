package net.boulangermod.boulanger.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.boulangermod.boulanger.block.entity.WoodGasFeedThroughBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class WoodGasFeedThroughRenderer implements BlockEntityRenderer<WoodGasFeedThroughBlockEntity> {
    private final BlockRenderDispatcher dispatcher;

    public WoodGasFeedThroughRenderer(BlockEntityRendererProvider.Context ctx) {
        this.dispatcher = ctx.getBlockRenderDispatcher();
    }

    @Override
    public void render(WoodGasFeedThroughBlockEntity be,
                       float partialTicks,
                       PoseStack pose,
                       MultiBufferSource buffers,
                       int packedLight,
                       int packedOverlay) {

        if (be.getLevel() == null) return;
        BlockState state = be.getCoverOrFallback();

        pose.pushPose();

        var model = dispatcher.getBlockModel(state);
        var data  = ModelData.EMPTY;

        // Use the model's declared render layers
        long seed = state.getSeed(be.getBlockPos());
        for (RenderType layer : model.getRenderTypes(state, RandomSource.create(seed), data)) {
            VertexConsumer vc = buffers.getBuffer(layer);
            // chunk-style tesselation ⇒ correct AO/skylight/shadows
            dispatcher.getModelRenderer().tesselateBlock(
                    be.getLevel(),
                    model,
                    state,
                    be.getBlockPos(),
                    pose,
                    vc,
                    /* checkSides */ false,
                    RandomSource.create(seed),
                    seed,
                    OverlayTexture.NO_OVERLAY,
                    data,
                    layer
            );
        }

        pose.popPose();
    }
}