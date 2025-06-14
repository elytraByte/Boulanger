package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.BakersTableBlockEntity;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.screen.BakersTableMenu;
import net.boulangermod.boulanger.screen.MixingBlockMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class BakersTableBlock extends AbstractProcessingBlock {

    public static final MapCodec<BakersTableBlock> CODEC = simpleCodec(BakersTableBlock::new);

    public BakersTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BakersTableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.BAKERS_TABLE.get(),
                level.isClientSide ? null : BakersTableBlockEntity::tick);
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return (be instanceof MenuProvider provider) ? provider : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider provider) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inventory, plyr) -> new BakersTableMenu(id, inventory, entity),
                        Component.translatable("bakers_table.boulanger")
                ), pos);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
