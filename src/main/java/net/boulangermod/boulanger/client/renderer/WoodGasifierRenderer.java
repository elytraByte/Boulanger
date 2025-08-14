package net.boulangermod.boulanger.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.block.WoodGasifierBlock;
import net.boulangermod.boulanger.block.entity.WoodGasifierBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class WoodGasifierRenderer implements BlockEntityRenderer<WoodGasifierBlockEntity> {
    private final BlockRenderDispatcher blocks;

    // If your model JSON is centered (e.g., spans -16..+16 on the width axis),
    // bump this to +1 to shift one block to the RIGHT (relative to facing),
    // so the left edge lands exactly on the leftmost cell.
    private static final int MODEL_RIGHT_OFFSET_BLOCKS = 2;

    // If you need to nudge forward/back (depth) relative to facing, change this.
    // Positive moves TOWARD the facing direction; negative moves back.
    private static final int MODEL_FORWARD_OFFSET_BLOCKS = 0;

    public WoodGasifierRenderer(BlockEntityRendererProvider.Context ctx) {
        this.blocks = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(WoodGasifierBlockEntity be, float pt, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null || !be.isFormed() || !be.isAnchor()) return;

        final BlockPos min = be.getMinCorner();  // bottom-left-rear of the 2×2×1 footprint
        if (min == null) return;

        final BlockPos here    = be.getBlockPos();
        final BlockState state = be.getBlockState();
        final Direction facing = state.getValue(WoodGasifierBlock.FACING);
        final Direction fwd    = facing;                  // +Z of the multiblock “depth”

        // Render the visible “front/frame” block for the formed structure
        final BlockState renderState = ModBlocks.WOOD_GASIFIER.get().defaultBlockState()
                .setValue(WoodGasifierBlock.FACING, facing)
                .setValue(WoodGasifierBlock.FORMED, true)
                .setValue(WoodGasifierBlock.HIDDEN, false);

        pose.pushPose();
        try {
            // 1) Move from the BE location to the multiblock’s MIN (leftmost, rear, bottom) cell.
            pose.translate(min.getX() - here.getX(), min.getY() - here.getY(), min.getZ() - here.getZ());
            // 1) BE -> multiblock min
            pose.translate(min.getX() - here.getX(), min.getY() - here.getY(), min.getZ() - here.getZ());

// 1-block LEFT shift (left = opposite of "right" = facing.getClockWise())
            final Direction right = facing.getClockWise();
            pose.translate(-right.getStepX(), 0, -right.getStepZ());


            // 2) Precisely pin model origin to that leftmost cell.
            //    (No automatic left/back shifts here—this is the anchor.)

            // 3) Optional authoring offsets (keep 0 unless your JSON is centered or needs a depth nudge):
            if (MODEL_RIGHT_OFFSET_BLOCKS != 0 || MODEL_FORWARD_OFFSET_BLOCKS != 0) {
                pose.translate(
                        right.getStepX() * MODEL_RIGHT_OFFSET_BLOCKS + fwd.getStepX() * MODEL_FORWARD_OFFSET_BLOCKS,
                        0,
                        right.getStepZ() * MODEL_RIGHT_OFFSET_BLOCKS + fwd.getStepZ() * MODEL_FORWARD_OFFSET_BLOCKS
                );
            }

            blocks.renderSingleBlock(renderState, pose, buffers, light, overlay);
        } finally {
            pose.popPose();
        }
    }

    @Override public boolean shouldRenderOffScreen(WoodGasifierBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 128; }
    @Override public AABB getRenderBoundingBox(WoodGasifierBlockEntity be) { return be.getFootprintAABBForRender(); }
}
