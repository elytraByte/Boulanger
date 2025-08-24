package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.boulangermod.boulanger.component.DoughRecipeComponent.INT_STREAM_CODEC;
import static net.boulangermod.boulanger.component.DoughRecipeComponent.STRING_STREAM_CODEC;

public record IngredientInfo(
        String itemId,                  // e.g. "boulanger:flour" or "minecraft:water_bucket"
        IngredientCategory category,    // FLOUR, WATER, SALT, YEAST, …
        int weight,                     // grams
        FlourType flourType             // OPTIONAL; only populated when category == FLOUR
) {

    // ----- Constructors / factories -------------------------------------------------

    public IngredientInfo(String itemId, IngredientCategory category, int weight) {
        this(itemId, category, weight, null);
    }

    /** Generic factory (no flour variant). */
    public static IngredientInfo of(String itemId, IngredientCategory category, int weight) {
        return new IngredientInfo(itemId, category, weight, null);
    }

    /** Returns a copy with a flour variant attached (convenient for your single-flour-item setup). */
    public IngredientInfo withFlourType(FlourType type) {
        return new IngredientInfo(this.itemId, this.category, this.weight, type);
    }

    /** Quick check. */
    public boolean isFlour() { return category == IngredientCategory.FLOUR; }

    // ----- CODEC -------------------------------------------------------------------

    private static final Codec<IngredientCategory> CATEGORY_CODEC =
            Codec.STRING.xmap(IngredientCategory::valueOf, IngredientCategory::name);

    public static final Codec<IngredientInfo> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("itemId").forGetter(IngredientInfo::itemId),
                    CATEGORY_CODEC.fieldOf("category").forGetter(IngredientInfo::category),
                    Codec.INT.fieldOf("weight").forGetter(IngredientInfo::weight),
                    // Optional; absent for non-flour entries
                    FlourType.CODEC.optionalFieldOf("flourType").forGetter(i -> Optional.ofNullable(i.flourType))
            ).apply(instance, (itemId, category, weight, flourOpt) ->
                    new IngredientInfo(itemId, category, weight, flourOpt.orElse(null)))
    );

    // ----- STREAM_CODEC -------------------------------------------------------------

    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientInfo> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buffer, IngredientInfo info) {
                    // itemId
                    STRING_STREAM_CODEC.encode(buffer, info.itemId());
                    // category
                    STRING_STREAM_CODEC.encode(buffer, info.category().name());
                    // weight
                    INT_STREAM_CODEC.encode(buffer, info.weight());
                    // flourType presence + payload
                    boolean hasFlour = info.flourType != null;
                    buffer.writeBoolean(hasFlour);
                    if (hasFlour) FlourType.STREAM_CODEC.encode(buffer, info.flourType);
                }

                @Override
                public IngredientInfo decode(RegistryFriendlyByteBuf buffer) {
                    String itemId  = STRING_STREAM_CODEC.decode(buffer);
                    String catName = STRING_STREAM_CODEC.decode(buffer);
                    int weight     = INT_STREAM_CODEC.decode(buffer);
                    FlourType flour = buffer.readBoolean() ? FlourType.STREAM_CODEC.decode(buffer) : null;
                    return new IngredientInfo(itemId, IngredientCategory.valueOf(catName), weight, flour);
                }
            };

    public static final StreamCodec<RegistryFriendlyByteBuf, List<IngredientInfo>> INGREDIENT_INFO_LIST =
            new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buffer, List<IngredientInfo> list) {
                    INT_STREAM_CODEC.encode(buffer, list.size());
                    for (IngredientInfo info : list) STREAM_CODEC.encode(buffer, info);
                }

                @Override
                public List<IngredientInfo> decode(RegistryFriendlyByteBuf buffer) {
                    int size = INT_STREAM_CODEC.decode(buffer);
                    List<IngredientInfo> result = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) result.add(STREAM_CODEC.decode(buffer));
                    return result;
                }
            };
}
