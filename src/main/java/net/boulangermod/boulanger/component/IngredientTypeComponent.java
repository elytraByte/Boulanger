package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

public record IngredientTypeComponent(Item item) {
    public static final Codec<IngredientTypeComponent> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    // Apply xmap BEFORE fieldOf
                    BuiltInRegistries.ITEM.byNameCodec()
                            .fieldOf("item")
                            .forGetter(IngredientTypeComponent::item)
            ).apply(instance, IngredientTypeComponent::new)
    );



    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientTypeComponent> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.ITEM, IngredientTypeComponent::item,
                    IngredientTypeComponent::new
            );
}


