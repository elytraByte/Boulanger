package org.l3e.Boulanger.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.time.format.TextStyle;
import java.util.List;

public class SafRedItem extends Item {

    public SafRedItem(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> components, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, level, components, tooltipFlag);

        itemStack.setHoverName(Component.translatable("tooltip.boulanger.safreditem.name").withStyle(style -> style.withColor(ChatFormatting.LIGHT_PURPLE).withItalic(false)));

//        components.add(Component.translatable("Saf Instant: Red").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        //components.add(Component.translatable(ChatFormatting.ITALIC + "reliable and consistent"));
       // components.add(Component.translatable(ChatFormatting.ITALIC + "results from batch to batch"));
    }


}

