// EnergyStorageBlock.java
package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.EnergyStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class EnergyStorageBlock extends BaseEntityBlock implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public EnergyStorageBlock(Properties props) {
        super(props.lightLevel(s -> s.getValue(LIT) ? 4 : 0));
        // register default state (must be after super)
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LIT, false)
        );
    }

    // remove this if you're not using a data-driven codec
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    // <-- FIXED signature here:
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyStorageBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return null; // no ticking needed
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state,
                                               Level world,
                                               BlockPos pos,
                                               Player player,
                                               BlockHitResult hit) {
        if (!world.isClientSide) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof EnergyStorageBlockEntity battery) {
                int stored = battery.getEnergyStorage(null).getEnergyStored();
                player.sendSystemMessage(
                        Component.literal("Stored energy: " + stored + " RF")
                );
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

}
