package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.component.value.IngredientItemComponent;
import net.boulangermod.boulanger.content.ingredient.IngredientCategory;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Locale;

public record IngredientComponent(
        IngredientCategory category,
        double targetPercent,
        List<IngredientItemComponent> allowedItems
) {
    public IngredientComponent {
        if (allowedItems == null) allowedItems = List.of();
        allowedItems = List.copyOf(allowedItems);
    }

    private static final Codec<IngredientCategory> CATEGORY_CODEC =
            Codec.STRING.xmap(
                    s -> IngredientCategory.valueOf(s.trim().toUpperCase(Locale.ROOT)),
                    c -> c.name().toLowerCase(Locale.ROOT)
            );

    public static final Codec<IngredientComponent> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            CATEGORY_CODEC.fieldOf("category").forGetter(IngredientComponent::category),
            Codec.DOUBLE.fieldOf("target_percent").forGetter(IngredientComponent::targetPercent),
            IngredientItemComponent.CODEC.listOf().optionalFieldOf("allowed_items", List.of())
                    .forGetter(IngredientComponent::allowedItems)
    ).apply(inst, IngredientComponent::new));

    // Network codec for IngredientCategory (case-insensitive)
    private static final StreamCodec<RegistryFriendlyByteBuf, IngredientCategory> CATEGORY_STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8
                    .map(
                            s -> IngredientCategory.valueOf(s.trim().toUpperCase(Locale.ROOT)),
                            c -> c.name().toLowerCase(Locale.ROOT)
                    )
                    .cast();

    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientComponent> STREAM_CODEC =
            StreamCodec.composite(
                    CATEGORY_STREAM_CODEC, IngredientComponent::category,
                    StreamCodecsCompat.DOUBLE, IngredientComponent::targetPercent,
                    StreamCodecsCompat.list(IngredientItemComponent.STREAM_CODEC), IngredientComponent::allowedItems,
                    IngredientComponent::new
            );
}
