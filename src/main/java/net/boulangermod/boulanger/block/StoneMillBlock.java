package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.StoneMillBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class StoneMillBlock extends AbstractProcessingBlock {
    public static final MapCodec<StoneMillBlock> CODEC = simpleCodec(StoneMillBlock::new);

    // 16×10 base + 12×6 top (centered)
    private static final VoxelShape SHAPE = Shapes.or(
            // Base: full footprint, 10 tall
            net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 10, 16),
            // Top cube: 12×12×6, centered on top of the base
            net.minecraft.world.level.block.Block.box(2, 10, 2, 14, 16, 14)
    );

    public StoneMillBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StoneMillBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // Outline / selection shape
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // Avoid culling neighbors like a full cube (model is not a solid cube)
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    // Match physical collision to our custom shape
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // Use our shape for light occlusion to prevent dark artifacts
    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StoneMillBlockEntity mill) {
                // Shift-right-click with redstone = inject 2000 FE (testing)
                if (player.isShiftKeyDown() && stack.is(Items.REDSTONE)) {
                    var es = mill.getEnergyStorage(hit.getDirection());
                    if (es != null) {
                        int got = es.receiveEnergy(2000, false);
                        player.displayClientMessage(
                                Component.literal("Injected " + got + " FE; now " + es.getEnergyStored()),
                                true
                        );
                    }
                    return ItemInteractionResult.SUCCESS;
                }
                // Normal open
                if (be instanceof MenuProvider provider) {
                    player.openMenu(provider, pos);
                }
            } else {
                throw new IllegalStateException("StoneMillBlockEntity missing at " + pos);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
