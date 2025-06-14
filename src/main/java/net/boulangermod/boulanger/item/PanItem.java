package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.PanTypeComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import java.util.List;

public class PanItem extends Item {
    private final PanType panType;

    public PanItem(Properties properties, PanType panType) {
        super(properties);
        this.panType = panType;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        ensurePanTypeSet(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        ensurePanTypeSet(stack);
    }

    private void ensurePanTypeSet(ItemStack stack) {
        if (stack.has(ModDataComponentTypes.PAN_TYPE.get())) return;

        // Attempt to resolve from CustomModelData
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA)) {
            int modelIndex = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA).value();
            for (PanType type : PanType.values()) {
                if (type.getModelIndex() == modelIndex) {
                    stack.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent(type.getId()));
                    return;
                }
            }
        }

        // Fallback to default constructor type
        stack.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent(panType.getId()));
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
        stack.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent(panType.getId()));
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(panType.getModelIndex()));
        return stack;
    }

}
