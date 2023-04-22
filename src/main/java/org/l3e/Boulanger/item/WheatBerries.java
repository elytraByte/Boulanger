package org.l3e.Boulanger.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WheatBerries extends Item {

    public WheatBerries(Item.Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public void appendHoverText(ItemStack p_41421_, @Nullable Level p_41422_, List<Component> p_41423_, TooltipFlag p_41424_) {
        super.appendHoverText(p_41421_, p_41422_, p_41423_, p_41424_);

        ItemStack is = p_41421_;
        CompoundTag nbt = is.getOrCreateTag();

        if (nbt.getString("boulanger:test").equals("separated wheat berries")) {
            p_41423_.add(Component.translatable("tooltip.boulanger.wheat_berries.separated_wheat_berries").withStyle(ChatFormatting.YELLOW));
        }

        if (nbt.getString("boulanger:test").equals("aspirated wheat berries")) {
            p_41423_.add(Component.translatable("tooltip.boulanger.wheat_berries.aspirated_wheat_berries").withStyle(ChatFormatting.YELLOW));
        }

        if (nbt.getString("boulanger:test").equals("destoned wheat berries")) {
            p_41423_.add(Component.translatable("tooltip.boulanger.wheat_berries.destoned_wheat_berries").withStyle(ChatFormatting.YELLOW));
        }
        if (nbt.getString("boulanger:test").equals("disc-separated wheat berries")) {
            p_41423_.add(Component.translatable("tooltip.boulanger.wheat_berries.disc-separeated_wheat_berries").withStyle(ChatFormatting.YELLOW));
        }
    }
}
