package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.Codec;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiFunction;
import java.util.function.Function;

public class IngredientRequirement {
    private final ResourceLocation itemId;
    private final double amount;

    public IngredientRequirement(ResourceLocation itemId, double amount) {
        this.itemId = itemId;
        this.amount = amount;
    }

    /** The item this requirement refers to (e.g. "minecraft:egg") */
    public ResourceLocation getItemId() {
        return itemId;
    }

    /** The minimum grams (or units) required of that item */
    public double getAmount() {
        return amount;
    }

    /**
     * Returns true if the given stack’s item matches this requirement.
     * (You can extend this to also check a weight data-component if you like.)
     */
    public boolean matches(ItemStack stack) {
        Item required = BuiltInRegistries.ITEM.getOptional(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown item: " + itemId));
        return stack.getItem() == required;
    }

    /**
     * Codec that reads/writes JSON like:
     * {
     *   "item": "boulanger:some_ingredient",
     *   "amount": 50.0
     * }
     */
    public static final MapCodec<IngredientRequirement> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(IngredientRequirement::getItemId),
            Codec.DOUBLE.fieldOf("amount").forGetter(IngredientRequirement::getAmount)
    ).apply(inst, IngredientRequirement::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientRequirement> STREAM_CODEC;

    static {
        StreamCodec<RegistryFriendlyByteBuf, ResourceLocation> idCodec = StreamCodecsCompat.RESOURCE_LOCATION;
        StreamCodec<RegistryFriendlyByteBuf, Double> amountCodec = StreamCodecsCompat.DOUBLE;

        Function<IngredientRequirement, ResourceLocation> getId = IngredientRequirement::getItemId;
        Function<IngredientRequirement, Double> getAmount = IngredientRequirement::getAmount;
        BiFunction<ResourceLocation, Double, IngredientRequirement> factory = IngredientRequirement::new;

        STREAM_CODEC = StreamCodec.<RegistryFriendlyByteBuf, IngredientRequirement, ResourceLocation, Double>composite(
                idCodec,
                getId,
                amountCodec,
                getAmount,
                factory
        );
    }
}
