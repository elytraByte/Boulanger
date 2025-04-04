package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;


public class FlourItem extends Item {
    public FlourItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        FlourType type = stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
        if (type != null) {
            tooltipComponents.add(Component.literal("Type: " + type.type()).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal("Ash: " + type.ash() + "%").withStyle(ChatFormatting.DARK_GRAY));
            tooltipComponents.add(Component.literal("Protein: " + type.protein() + "%").withStyle(ChatFormatting.BLUE));
        } else {
            tooltipComponents.add(Component.literal("No flour data").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            ItemStack stack = player.getItemInHand(hand);
            FlourType type = stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
            if (type != null) {
                System.out.println("Used flour: " + type.type() + " (Protein: " + type.protein() + ", Ash: " + type.ash() + ")");
            } else {
                System.out.println("Used unknown flour");
            }
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public Component getName(ItemStack stack) {
        FlourType type = stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
        if (type != null) {
            return Component.translatable("item.boulanger.flour." + type.type());
        }
        return super.getName(stack); // fallback
    }

}
