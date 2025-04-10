package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WheatVarietyRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IDataComponentHolderExtension;

import java.util.List;

public class WheatSeedItem extends ItemNameBlockItem {
    public WheatSeedItem(net.minecraft.world.level.block.Block block, Properties props) {
        super(block, props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        // 1) Let vanilla place the crop
        InteractionResult result = super.useOn(ctx);
        if (!result.consumesAction()) {
            return result;
        }

        // 2) Find the block entity
        Level world = ctx.getLevel();
        BlockPos cropPos = ctx.getClickedPos().above();
        BlockEntity be = world.getBlockEntity(cropPos);
        if (be == null) {
            return result;
        }

        // 3) Read the variety from the ItemStack
        ItemStack stack = ctx.getItemInHand();
        WheatVariety variety = ((IDataComponentHolderExtension) stack)
                .get(ModDataComponentTypes.WHEAT_VARIETY);
        if (variety == null) {
            return result;
        }

        // 4) Rebuild the block-entity's DataComponentMap with the new value
        DataComponentMap existing = be.components();
        Builder builder = DataComponentMap.builder().addAll(existing);
        builder.set(ModDataComponentTypes.WHEAT_VARIETY.get(), variety);
        DataComponentMap updated = builder.build();

        // 5) Apply and mark dirty
        be.setComponents(updated);
        be.setChanged();

        return result;
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

