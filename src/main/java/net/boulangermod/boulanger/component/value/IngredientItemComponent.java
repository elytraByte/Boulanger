package net.boulangermod.boulanger.component.value;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

public record IngredientItemComponent(Item item) {
    public static final Codec<IngredientItemComponent> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    BuiltInRegistries.ITEM.byNameCodec()
                            .fieldOf("item")
                            .forGetter(IngredientItemComponent::item)
            ).apply(instance, IngredientItemComponent::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientItemComponent> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.ITEM, IngredientItemComponent::item,
                    IngredientItemComponent::new
            );
}


