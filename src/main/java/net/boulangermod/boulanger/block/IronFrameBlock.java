package net.boulangermod.boulanger.block;

import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

public class IronFrameBlock extends Block {
    public static final int MAX_STAGE = 7; // 0..7
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, MAX_STAGE);

    public IronFrameBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F, 6.0F)
                .sound(SoundType.METAL)
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        int current = state.getValue(STAGE);
        if (current >= MAX_STAGE || stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        int next = current + 1;
        Item required = requiredItemFor(next); // next-stage requirement

        if (!stack.is(required)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(STAGE, next), Block.UPDATE_ALL);
            if (!player.isCreative()) stack.shrink(1);
        }
        return ItemInteractionResult.SUCCESS; // we handled it
    }

    private Item requiredItemFor(int nextStage) {
        // Stage 1: PCB ; Stages 2..7: Diorite Plate
        if (nextStage == 1) {
            return ModItems.PCB.get();
        } else { // 2..7
            return ModItems.DIORITE_PLATE.get(); // or Items.DIORITE if you prefer vanilla for now
        }
    }
}