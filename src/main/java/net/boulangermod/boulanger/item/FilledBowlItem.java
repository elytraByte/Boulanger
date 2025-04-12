package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.IngredientTypeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class FilledBowlItem extends Item {

    public FilledBowlItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        // Call the super method first
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        // Retrieve and add the WeightComponent tooltip.
        WeightComponent weightComp = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS);
        if (weightComp != null) {
            tooltipComponents.add(Component.literal("Weight: " + weightComp.grams() + " g"));
        }

        // Retrieve and add the FlourType tooltip, if available.
        FlourType flourType = stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
        if (flourType != null) {
            tooltipComponents.add(Component.literal("Flour Type: " + flourType.getId()));
        }

        // Retrieve and add the IngredientTypeComponent tooltip.
        IngredientTypeComponent ingredientType = stack.get(ModDataComponentTypes.INGREDIENT_TYPE.get());
        if (ingredientType != null) {
            tooltipComponents.add(Component.literal("Ingredient: " + ingredientType));
        }

        // Retrieve and add the IngredientCategory tooltip.
        IngredientCategory category = stack.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get());
        if (category != null) {
            tooltipComponents.add(Component.literal("Category: " + category.toString()));
        }
    }
}
