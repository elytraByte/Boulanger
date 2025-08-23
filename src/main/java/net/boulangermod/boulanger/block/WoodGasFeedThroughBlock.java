package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.WoodGasFeedThroughBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Wood-gas wall feedthrough: bridges capability across ALL axes.
 * Exposes a handler on every face; each face forwards to the opposite neighbor.
 * No menu/inventory. Full cube for collision/occlusion. Rendered via BER (INVISIBLE here).
 */
public class WoodGasFeedThroughBlock extends BaseEntityBlock implements EntityBlock {

    private static final VoxelShape FULL = Shapes.block();

    public WoodGasFeedThroughBlock(Properties props) {
        super(props);
    }

    // ——— placement/state ———
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState(); // no axis flags needed; all faces are valid
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state; // static shape
    }

    // ——— shapes & rendering ———
    @Override public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return FULL; }
    @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return FULL; }
    @Override public VoxelShape getOcclusionShape(BlockState s, BlockGetter g, BlockPos p) { return FULL; }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; } // BER draws camo

    private static boolean isFullCubeSolid(BlockState state, Level level, BlockPos samplePos) {
        // Must be a normal model, opaque/occluding, and a full cube shape
        if (state.getRenderShape() != RenderShape.MODEL) return false;
        if (!state.canOcclude()) return false;
        if (!state.isSolidRender(level, samplePos)) return false;  // solid visual render
        // Full-cube occlusion shape
        return Block.isShapeFullBlock(state.getOcclusionShape(level, samplePos));
    }

    // Your explicit blacklist (add any others you want)
    private static boolean isBlacklisted(Block b) {
        return b instanceof WoodGasPipe
                || b instanceof EnergyCableBlock
                || b instanceof WoodGasFlareBlock;
    }

    // ——— BE ———
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WoodGasFeedThroughBlockEntity(pos, state);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return null; }

    // ——— interactions: right-click to set/clear camo ———
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof WoodGasFeedThroughBlockEntity feed)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // ✅ Shift-right-click: clear camo regardless of held item
        if (player.isShiftKeyDown()) {
            feed.setCover(null);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // Apply camo only from solid full-cube blocks (and not your small multipart blocks)
        if (stack.getItem() instanceof BlockItem bi) {
            BlockState camo = bi.getBlock().defaultBlockState();
            if (!isBlacklisted(camo.getBlock()) && isFullCubeSolid(camo, level, pos)) {
                feed.setCover(camo);
                return ItemInteractionResult.sidedSuccess(level.isClientSide); // consume; don't place
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

}
