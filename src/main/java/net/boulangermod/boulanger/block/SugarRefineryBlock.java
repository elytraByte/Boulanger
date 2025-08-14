package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.block.entity.SugarRefineryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SugarRefineryBlock extends AbstractProcessingBlock {
    public static final MapCodec<SugarRefineryBlock> CODEC = simpleCodec(SugarRefineryBlock::new);

    public SugarRefineryBlock(Properties props) {
        super(props);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        // Approx brewing-stand shape: thin base + center rod
        // base: 4..12 x 0..2 x 4..12  |  rod: 7..9 x 0..14 x 7..9
        return Shapes.or(
                Block.box(4, 0, 4, 12, 2, 12),
                Block.box(7, 0, 7, 9, 14, 9)
        );
    }

    // Make light-occlusion match the visible shape (prevents dark-culling)
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SugarRefineryBlockEntity(pos, state);
    }

    // Brewing-stand style idle smoke
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(10) == 0) {
            double x = pos.getX() + 0.5D;
            double y = pos.getY() + 0.7D;
            double z = pos.getZ() + 0.5D;
            level.addParticle(ParticleTypes.WHITE_SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    // Server ticker already provided by AbstractProcessingBlock (Tickable)
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.SUGAR_REFINERY_BE.get(),
                (lvl, pos, st, be) -> ((SugarRefineryBlockEntity) be).tick(lvl, pos, st));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MenuProvider provider) {
                player.openMenu(provider, pos);   // ✅ send BlockPos to the client
            } else {
                throw new IllegalStateException("MenuProvider is missing for block at: " + pos);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}