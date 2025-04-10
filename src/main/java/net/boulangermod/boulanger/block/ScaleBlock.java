package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.ScaleBlockEntity;
import net.boulangermod.boulanger.screen.MixingBlockMenu;
import net.boulangermod.boulanger.screen.ScaleBlockMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ScaleBlock extends BaseEntityBlock implements MenuProvider {
    public static final MapCodec<ScaleBlock> CODEC = simpleCodec(ScaleBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ScaleBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ScaleBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider provider) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inventory, plyr) -> new ScaleBlockMenu(id, inventory, entity),
                        Component.translatable("scale.boulanger")
                ), pos); // 🡐 This part ensures extraData contains the block pos
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }


    @Override
    public AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player player) {
        BlockEntity entity = player.level().getBlockEntity(player.blockPosition());
        if (entity instanceof ScaleBlockEntity scale) {
            return scale.createMenu(id, inv, player);
        }
        return null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("scale.boulanger");
    }
}

