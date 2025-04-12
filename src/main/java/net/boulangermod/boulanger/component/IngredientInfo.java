package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

import static net.boulangermod.boulanger.component.DoughRecipeComponent.INT_STREAM_CODEC;
import static net.boulangermod.boulanger.component.DoughRecipeComponent.STRING_STREAM_CODEC;

public record IngredientInfo(String itemId, String category, int weight) {
    public static final Codec<IngredientInfo> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("itemId").forGetter(IngredientInfo::itemId),
                    Codec.STRING.fieldOf("category").forGetter(IngredientInfo::category),
                    Codec.INT.fieldOf("weight").forGetter(IngredientInfo::weight)
            ).apply(instance, IngredientInfo::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientInfo> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buffer, IngredientInfo info) {
                    // Suppose IngredientInfo has (String itemId, String category, int weight)
                    STRING_STREAM_CODEC.encode(buffer, info.itemId());
                    STRING_STREAM_CODEC.encode(buffer, info.category());
                    INT_STREAM_CODEC.encode(buffer, info.weight());
                }

                @Override
                public IngredientInfo decode(RegistryFriendlyByteBuf buffer) {
                    String itemId = STRING_STREAM_CODEC.decode(buffer);
                    String category = STRING_STREAM_CODEC.decode(buffer);
                    int weight = INT_STREAM_CODEC.decode(buffer);
                    return new IngredientInfo(itemId, category, weight);
                }
            };

    // Then a listOf() if needed:
    public static final StreamCodec<RegistryFriendlyByteBuf, List<IngredientInfo>> INGREDIENT_INFO_LIST =
            new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buffer, List<IngredientInfo> list) {
                    INT_STREAM_CODEC.encode(buffer, list.size());
                    for (IngredientInfo info : list) {
                        STREAM_CODEC.encode(buffer, info);
                    }
                }

                @Override
                public List<IngredientInfo> decode(RegistryFriendlyByteBuf buffer) {
                    int size = INT_STREAM_CODEC.decode(buffer);
                    List<IngredientInfo> result = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        result.add(STREAM_CODEC.decode(buffer));
                    }
                    return result;
                }
            };

}
