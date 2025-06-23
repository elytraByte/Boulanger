package net.boulangermod.boulanger.multiblock;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.multiblock.AbstractMultiblockSlaveEntity;
import net.boulangermod.boulanger.multiblock.WoodGasifierSlaveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class WoodGasifierSlaveBlock extends BaseEntityBlock {

    public static final MapCodec<WoodGasifierSlaveBlock> CODEC = simpleCodec(WoodGasifierSlaveBlock::new);

    public static final BooleanProperty HIDDEN = BooleanProperty.create("hidden");

    public WoodGasifierSlaveBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // default to visible
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HIDDEN, false)
        );
    }

    @Override
    public MapCodec<WoodGasifierSlaveBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WoodGasifierSlaveBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b) {
        b.add(HIDDEN);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // only draw when not hidden
        return state.getValue(HIDDEN)
                ? RenderShape.INVISIBLE
                : RenderShape.MODEL;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state,
                                            Level level,
                                            BlockPos pos,
                                            Player player,
                                            BlockHitResult hit) {
        // only on the logical server
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 1) Get the slave BE at this position
        BlockEntity te = level.getBlockEntity(pos);
        if (!(te instanceof AbstractMultiblockSlaveEntity slave)) {
            return InteractionResult.PASS;
        }

        // 2) Grab the stored master position
        BlockPos masterPos = slave.getMasterPos();
        if (masterPos == null) {
            player.displayClientMessage(Component.literal("Gasifier incomplete!"), true);
            return InteractionResult.SUCCESS;
        }

        // 3) Fetch the master BE and ensure it implements MenuProvider
        BlockEntity masterBe = level.getBlockEntity(masterPos);
        if (!(masterBe instanceof MenuProvider provider)) {
            return InteractionResult.PASS;
        }

        // 4) Open the master’s menu, writing its BlockPos into the packet
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    provider,
                    buf -> buf.writeBlockPos(masterPos)
            );
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }


}
