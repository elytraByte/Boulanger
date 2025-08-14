package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.DoughDividerBlockEntity;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DoughDividerBlock extends AbstractProcessingBlock {
    public static final MapCodec<DoughDividerBlock> CODEC = simpleCodec(DoughDividerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 8, 16),
            box(2, 8, 2, 14, 12, 14)
    );

    public DoughDividerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // Place so the machine's "front" faces the player (opposite of player look)
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    // Enable rotation/mirroring via structure blocks, /setblock with NBT, etc.
    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public VoxelShape getOcclusionShape(BlockState s, BlockGetter l, BlockPos p) { return Shapes.empty(); }
    @Override public boolean useShapeForLightOcclusion(BlockState s) { return true; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DoughDividerBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MenuProvider provider) player.openMenu(provider, pos);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override public void onPlace(BlockState s, Level lvl, BlockPos pos, BlockState old, boolean moving) {
        if (!lvl.isClientSide) lvl.scheduleTick(pos, this, 1);
    }
    @Override public void tick(BlockState s, ServerLevel lvl, BlockPos pos, RandomSource rand) {
        BlockEntity be = lvl.getBlockEntity(pos);
        if (be instanceof DoughDividerBlockEntity e) e.tick(lvl, pos, s);
    }
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level lvl, BlockState s, BlockEntityType<T> type) {
        return lvl.isClientSide ? null :
                (type == ModBlockEntities.DOUGH_DIVIDER.get()
                        ? (l, p, st, be) -> ((DoughDividerBlockEntity) be).tick(l, p, st)
                        : null);
    }
}
