package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.IngredientTypeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.world.item.ItemStack;

public class TypedFlourBagItem extends FiftyPoundBagItem {
    private final FlourItemType flourItemType;

    public TypedFlourBagItem(Properties props, FlourItemType type) {
        super(props);
        this.flourItemType = type;
    }


    @Override
    public ItemStack getDefaultInstance() {
        ItemStack bag = new ItemStack(this);
        bag.set(ModDataComponentTypes.FLOUR_TYPE.get(), flourItemType.toFlourType());
        bag.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(MAX_GRAMS));
        bag.set(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), IngredientCategory.FLOUR);
        bag.set(ModDataComponentTypes.INGREDIENT_TYPE.get(), new IngredientTypeComponent(ModItems.FLOUR_ITEM.get()));
        return bag;
    }
}

