package org.l3e.boulanger.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.l3e.boulanger.component.ModDataCompnentTypes;

import java.util.List;

public class WheatSeeds extends BGenericSeed {

    public WheatSeeds(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

       //make hover text indicate which type of wheat the seeds are, using the WheatType data component.

    }
}
