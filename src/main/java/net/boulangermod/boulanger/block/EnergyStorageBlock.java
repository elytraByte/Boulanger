package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.EnergyStorageBlockEntity;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

public class EnergyStorageBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public EnergyStorageBlock(Properties props) {
        super(props.lightLevel(s -> s.getValue(LIT) ? 4 : 0));
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    // If you’re not using data-driven codecs, this can return null (or implement a simple codec).
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return null; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyStorageBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.ENERGY_STORAGE_BE.get(), EnergyStorageBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof EnergyStorageBlockEntity battery) {
                int stored = battery.getEnergyStorage(null).getEnergyStored();
                int cap    = battery.getEnergyStorage(null).getMaxEnergyStored();
                player.sendSystemMessage(Component.literal("Stored energy: " + stored + " / " + cap + " FE"));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
