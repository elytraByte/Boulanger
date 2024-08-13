package org.l3e.boulanger.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class SaltKosher extends Item {

    public SaltKosher(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        tooltipComponents.add(Component.translatable("item.boulanger.salt_kosher.description").withStyle(ChatFormatting.YELLOW));
        tooltipComponents.add(Component.translatable("item.boulanger.salt_kosher.weight").withStyle(ChatFormatting.YELLOW)); //to do change to durability
    }
}
