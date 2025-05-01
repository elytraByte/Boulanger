package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.EnergyStorageBlockEntity;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.block.entity.WoodGasPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class WoodGasPipe extends BaseEntityBlock implements EntityBlock {
    public WoodGasPipe(Properties props) {
        super(props.noOcclusion());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }


    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState st) {
        return new WoodGasPipeBlockEntity(pos, st);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state,
                                               Level world,
                                               BlockPos pos,
                                               Player player,
                                               BlockHitResult hit) {
        if (!world.isClientSide) {
            var be = world.getBlockEntity(pos);

            // for your pipe:
            if (be instanceof WoodGasPipeBlockEntity pipe) {
                int mb = pipe.getTank().getFluidAmount();
                player.sendSystemMessage(
                        Component.literal("Pipe contains: " + mb + " mB wood-gas")
                );
                return InteractionResult.SUCCESS;
            }

            // for your battery:
            if (be instanceof EnergyStorageBlockEntity batt) {
                int rf = batt.getEnergyStorage(null).getEnergyStored();
                player.sendSystemMessage(
                        Component.literal("Stored energy: " + rf + " RF")
                );
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState st, BlockEntityType<T> type
    ) {
        if (level.isClientSide) return null;
        return createTickerHelper(
                type,
                ModBlockEntities.WOOD_GAS_PIPE_BE.get(),
                WoodGasPipeBlockEntity::tickServer
        );
    }
}
