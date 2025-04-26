package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.boulangermod.boulanger.util.IngredientCategory;

import java.util.List;

public record IngredientComponent(
        IngredientCategory category,
        double             targetPercent,
        List<ResourceLocation> allowedItems
) {
    public static final Codec<IngredientComponent> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            IngredientCategory.CODEC.fieldOf("category").forGetter(IngredientComponent::category),
            Codec.DOUBLE.fieldOf("target_percent").forGetter(IngredientComponent::targetPercent),
            ResourceLocation.CODEC.listOf()
                    .fieldOf("allowed_items").forGetter(IngredientComponent::allowedItems)
    ).apply(inst, IngredientComponent::new));

    /** Network‐buffer stream codec */
    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientComponent> STREAM_CODEC =
            StreamCodec.composite(
                    IngredientCategory.STREAM_CODEC,    // category
                    IngredientComponent::category,
                    StreamCodecsCompat.DOUBLE,          // targetPercent
                    IngredientComponent::targetPercent,
                    StreamCodecsCompat.list(ResourceLocation.STREAM_CODEC), // allowedItems
                    IngredientComponent::allowedItems,
                    IngredientComponent::new
            );
}
