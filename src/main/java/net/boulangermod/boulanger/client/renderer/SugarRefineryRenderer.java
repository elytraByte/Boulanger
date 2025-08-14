package net.boulangermod.boulanger.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.boulangermod.boulanger.block.entity.SugarRefineryBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class SugarRefineryRenderer implements BlockEntityRenderer<SugarRefineryBlockEntity> {

    // Height where the “arms” hold the item (adjust to taste)
    private static final float Y_ARM = 0.4f;
    private static final float SCALE = 0.50f;

    // Exact offsets that match vanilla brewing-stand pads (in block units)
    private static final float[][] OFFSETS = new float[][]{
            { +0.25f,  0.00f },  // east arm / bottle 0
            { -0.25f, -0.25f },  // north-west arm / bottle 1
            { -0.25f, +0.25f }   // south-west arm / bottle 2
    };

    public SugarRefineryRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(SugarRefineryBlockEntity be, float pt, PoseStack pose, MultiBufferSource buf, int packedLight, int packedOverlay) {
        if (be == null || be.getLevel() == null) return;

        // get stacks in the same order you want them shown
        var inv = be.getInventory();
        ItemStack sugar    = inv.getStackInSlot(SugarRefineryBlockEntity.SUGAR_SLOT);
        ItemStack molasses = inv.getStackInSlot(SugarRefineryBlockEntity.MOLASSES_SLOT);
        ItemStack brown    = inv.getStackInSlot(SugarRefineryBlockEntity.BROWN_SUGAR_SLOT);

        int light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above());

        // east, NW, SW to match vanilla stand
        renderAt(pose, buf, light, sugar,    OFFSETS[0][0], OFFSETS[0][1]);
        renderAt(pose, buf, light, molasses, OFFSETS[1][0], OFFSETS[1][1]);
        renderAt(pose, buf, light, brown,    OFFSETS[2][0], OFFSETS[2][1]);
    }

    private static void renderAt(PoseStack pose, MultiBufferSource buf, int light, ItemStack stack, float dx, float dz) {
        if (stack.isEmpty()) return;

        var ir = Minecraft.getInstance().getItemRenderer();

        pose.pushPose();
        pose.translate(0.5, Y_ARM, 0.5);   // center + correct height
        pose.translate(dx, 0.0, dz);       // exact pad/arm offset

        // face outward based on the offset vector
        float yaw = 180f - (float) Math.toDegrees(Math.atan2(dz, dx));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));

        pose.scale(SCALE, SCALE, SCALE);

        ir.renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                light,
                OverlayTexture.NO_OVERLAY,
                pose,
                buf,
                Minecraft.getInstance().level,
                0
        );
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(SugarRefineryBlockEntity be) { return false; }
}
