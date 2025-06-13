package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.boulangermod.boulanger.util.IngredientTags;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.crafting.IngredientType;

import static net.boulangermod.boulanger.util.IngredientCategory.*;

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

    public float getIngType() {
        return 0;
    }
    public static IngredientCategory fromItem(Item item) {
        if (item.builtInRegistryHolder().is(IngredientTags.WATER))         return WATER;
        if (item.builtInRegistryHolder().is(IngredientTags.EGGS))          return EGGS;
        if (item.builtInRegistryHolder().is(IngredientTags.DAIRY))         return DAIRY;
        if (item.builtInRegistryHolder().is(IngredientTags.SALTS))         return SALT;
        if (item.builtInRegistryHolder().is(IngredientTags.YEASTS))        return YEAST;
        if (item.builtInRegistryHolder().is(IngredientTags.FATS))          return FAT;
        if (item.builtInRegistryHolder().is(IngredientTags.SUGARS))        return SUGAR;
        if (item.builtInRegistryHolder().is(IngredientTags.ADDITIVES))     return ADDITIVE;
        if (item.builtInRegistryHolder().is(IngredientTags.ENRICHMENTS))   return ENRICHMENT;

        return CUSTOM;
    }
}


