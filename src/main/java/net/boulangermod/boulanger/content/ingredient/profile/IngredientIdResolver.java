package net.boulangermod.boulanger.content.ingredient.profile;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface IngredientIdResolver {
    @Nullable ResourceLocation resolve(ItemStack stack);
}
