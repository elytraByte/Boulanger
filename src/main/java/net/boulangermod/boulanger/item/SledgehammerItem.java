package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.block.IronWedgeBlock;
import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SledgehammerItem extends Item {
    public SledgehammerItem(Properties props) {
        super(props.stacksTo(1).durability(1024));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getHand() != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        Level level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.PASS;

        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        ItemStack hammer = ctx.getItemInHand();

        // 1) only on iron wedge block
        BlockState wedgeState = level.getBlockState(pos);
        if (!wedgeState.is(ModBlocks.IRON_WEDGE.get())) {
            return InteractionResult.PASS;
        }

        // 2) must have a log beneath
        BlockPos below = pos.below();
        if (!level.getBlockState(below).is(BlockTags.LOGS)) {
            return InteractionResult.PASS;
        }

        // 3) strip the log
        level.destroyBlock(below, false);

        // 4) spawn split logs
        Block.popResource(level, below, new ItemStack(ModItems.SPLIT_PINE_LOGS.get(), 8));

        // 5) damage the hammer
        hammer.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

        // 6) compute new wedge damage
        int oldDamage = wedgeState.getValue(IronWedgeBlock.DAMAGE);
        int newDamage = oldDamage + 1;

        // 7) remove the wedge block
        level.destroyBlock(pos, false);

        // 8) drop back the wedge item with updated damage
        ItemStack drop = new ItemStack(ModItems.IRON_WEDGE.get());
        drop.setDamageValue(newDamage);
        Block.popResource(level, pos, drop);

        return InteractionResult.CONSUME;
    }

}
