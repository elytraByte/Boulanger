package net.boulangermod.boulanger.item;


import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.ItemStack;


import java.util.List;

public class WheatSeedItem extends ItemNameBlockItem {
    public WheatSeedItem(net.minecraft.world.level.block.Block block, Properties props) {
        super(block, props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        // Just call super — our crop block handles reading the seed's component
        return super.useOn(ctx);
    }



    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        // Read the wheat_variety component
        WheatVariety variety = stack.get(ModDataComponentTypes.WHEAT_VARIETY.get());
        if (variety != null) {
            // Add a localized line showing the variety
            tooltipComponents.add(Component.literal("Wheat variety: " + variety.getId().toString()).withStyle(ChatFormatting.YELLOW)
            );
        } else {
            tooltipComponents.add(
                    Component.literal("Unknown variety")
                            .withStyle(ChatFormatting.RED)
            );
        }
    }
}

