package net.boulangermod.boulanger.multiblock;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.Block;

public class TestMultiblockMasterBlock extends BaseEntityBlock {

    // this field gives Minecraft a trivial codec for your block
    // 1) Use Minecraft's helper:
    public static final MapCodec<TestMultiblockMasterBlock> CODEC =
            simpleCodec(TestMultiblockMasterBlock::new);


    public TestMultiblockMasterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // satisfy the abstract contract
    @Override
    public MapCodec<TestMultiblockMasterBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state,
                                               Level level,
                                               BlockPos pos,
                                               Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TestMultiblockMasterBlockEntity master) {
                boolean formed = master.tryFormOrDismantle();
                player.sendSystemMessage(Component.literal(
                        formed ? "Multiblock formed!" : "Multiblock dismantled"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestMultiblockMasterBlockEntity(pos, state);
    }
}