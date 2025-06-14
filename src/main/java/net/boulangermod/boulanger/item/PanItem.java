package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.PanTypeComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class PanItem extends Item {

    public PanItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        if (stack.has(ModDataComponentTypes.PAN_TYPE)) {
            PanTypeComponent type = stack.get(ModDataComponentTypes.PAN_TYPE);
            tooltipComponents.add(Component.literal("Pan Type: " + type.id()));
        }
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        // Optional: assign default pan type (e.g., "loaf")
        stack.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent("loaf"));
        return stack;
    }
}
