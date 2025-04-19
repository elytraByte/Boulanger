package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

import static net.boulangermod.boulanger.component.DoughRecipeComponent.INT_STREAM_CODEC;
import static net.boulangermod.boulanger.component.DoughRecipeComponent.STRING_STREAM_CODEC;

public record IngredientInfo(String itemId, IngredientCategory category, int weight) {
    /**
     * Codec<String,IngredientCategory> that goes back and forth
     * between the enum’s name() and the enum instance.
     */
    private static final Codec<IngredientCategory> CATEGORY_CODEC =
            Codec.STRING.xmap(IngredientCategory::valueOf, IngredientCategory::name);

    public static final Codec<IngredientInfo> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("itemId")
                            .forGetter(IngredientInfo::itemId),

                    // use our CATEGORY_CODEC here:
                    CATEGORY_CODEC.fieldOf("category")
                            .forGetter(IngredientInfo::category),

                    Codec.INT.fieldOf("weight")
                            .forGetter(IngredientInfo::weight)
            ).apply(instance, IngredientInfo::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientInfo> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buffer, IngredientInfo info) {
                    // itemId
                    STRING_STREAM_CODEC.encode(buffer, info.itemId());
                    // category as its name()
                    STRING_STREAM_CODEC.encode(buffer, info.category().name());
                    // weight
                    INT_STREAM_CODEC.encode(buffer, info.weight());
                }

                @Override
                public IngredientInfo decode(RegistryFriendlyByteBuf buffer) {
                    String itemId = STRING_STREAM_CODEC.decode(buffer);
                    String catName = STRING_STREAM_CODEC.decode(buffer);
                    int weight    = INT_STREAM_CODEC.decode(buffer);

                    // valueOf back into the enum
                    IngredientCategory cat = IngredientCategory.valueOf(catName);
                    return new IngredientInfo(itemId, cat, weight);
                }
            };

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
