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

    // Outline/collision shape (adjust to match your model)
    private static final VoxelShape SHAPE = Shapes.or(
            // base ring
            net.minecraft.world.level.block.Block.box(4, 0, 4, 12, 2, 12),
            // body/frame
            net.minecraft.world.level.block.Block.box(3, 2, 3, 13, 10, 13),
            // top stone/wheel
            net.minecraft.world.level.block.Block.box(5, 10, 5, 11, 13, 11)
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

    // Make occlusion empty so we don't cull neighbor faces like a full cube
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    // Optional: also match physical collision to the smaller shape
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // Optional: use our shape for light occlusion to avoid dark artifacts
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
                    player.openMenu(provider, pos); // send BlockPos to client
                }
            } else {
                throw new IllegalStateException("StoneMillBlockEntity missing at " + pos);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
