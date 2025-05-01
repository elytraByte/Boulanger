package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.block.entity.WoodGasifierBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class WoodGasifierBlock extends BaseEntityBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty    LIT     = BlockStateProperties.LIT;

    public WoodGasifierBlock(Properties props) {
        super(props.lightLevel(s -> s.getValue(LIT) ? 13 : 0));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, LIT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState st, Rotation rot) {
        return st.setValue(FACING, rot.rotate(st.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState st, Mirror mirror) {
        return st.rotate(mirror.getRotation(st.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState st) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState st) {
        return new WoodGasifierBlockEntity(pos, st);
    }

    @Override
    @Nullable
    public MenuProvider getMenuProvider(BlockState st, Level lvl, BlockPos pos) {
        return lvl.getBlockEntity(pos) instanceof WoodGasifierBlockEntity be
                ? be
                : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level lvl, BlockState st, BlockEntityType<T> type
    ) {
        // **only** return a ticker on the server side
        if (lvl.isClientSide) return null;

        return createTickerHelper(
                type,
                ModBlockEntities.WOOD_GASIFIER_BE.get(),
                WoodGasifierBlockEntity::ticker
        );
    }

    @Override
    public void onRemove(BlockState old, Level world, BlockPos pos,
                         BlockState fresh, boolean moved) {
        if (old.getBlock() != fresh.getBlock()) {
            super.onRemove(old, world, pos, fresh, moved);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState st,
                                               Level lvl,
                                               BlockPos pos,
                                               Player player,
                                               BlockHitResult hit) {
        if (!lvl.isClientSide && player instanceof ServerPlayer server) {
            server.openMenu(
                    st.getMenuProvider(lvl, pos),
                    buf -> buf.writeBlockPos(pos)
            );
        }
        return InteractionResult.sidedSuccess(lvl.isClientSide);
    }
}
