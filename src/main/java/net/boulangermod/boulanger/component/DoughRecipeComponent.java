package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record DoughRecipeComponent(ResourceLocation recipeId,
                                   Map<IngredientCategory, Double> targetPercentages,
                                   List<IngredientInfo> ingredients,
                                   int totalWeight) {

    public ResourceLocation id() {
        return recipeId;
    }


    /**
     * Helper method to extract the map keys as a list.
     */
    public List<IngredientCategory> targetPercentagesKeys() {
        return new ArrayList<>(targetPercentages.keySet());
    }

    /**
     * Helper method to extract the map values as a list.
     */
    public List<Double> targetPercentagesValues() {
        return new ArrayList<>(targetPercentages.values());
    }

    public static final Codec<DoughRecipeComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("recipeId").forGetter(DoughRecipeComponent::recipeId),
                    // Encode the map as two parallel lists.
                    Codec.STRING.listOf().fieldOf("targetPercentagesKeys").forGetter(
                            comp -> {
                                List<String> keyNames = new ArrayList<>();
                                for (IngredientCategory cat : comp.targetPercentages().keySet()) {
                                    keyNames.add(cat.name());
                                }
                                return keyNames;
                            }
                    ),
                    Codec.DOUBLE.listOf().fieldOf("targetPercentagesValues").forGetter(DoughRecipeComponent::targetPercentagesValues),
                    IngredientInfo.CODEC.listOf().fieldOf("ingredients").forGetter(DoughRecipeComponent::ingredients),
                    Codec.INT.fieldOf("totalWeight").forGetter(DoughRecipeComponent::totalWeight)
            ).apply(instance, (recipeId, keyNames, values, ingredients, totalWeight) -> {
                Map<IngredientCategory, Double> percentages = new EnumMap<>(IngredientCategory.class);
                for (int i = 0; i < keyNames.size(); i++) {
                    percentages.put(IngredientCategory.valueOf(keyNames.get(i)), values.get(i));
                }
                return new DoughRecipeComponent(recipeId, percentages, ingredients, totalWeight);
            })
    );

    // For reading/writing a single INT
    public static final StreamCodec<RegistryFriendlyByteBuf, Integer> INT_STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buffer, Integer value) {
                    net.minecraft.network.codec.ByteBufCodecs.INT.encode(buffer, value);
                }

                @Override
                public Integer decode(RegistryFriendlyByteBuf buffer) {
                    return net.minecraft.network.codec.ByteBufCodecs.INT.decode(buffer);
                }
            };

    // For reading/writing a single DOUBLE
    public static final StreamCodec<RegistryFriendlyByteBuf, Double> DOUBLE_STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buffer, Double value) {
                    net.minecraft.network.codec.ByteBufCodecs.DOUBLE.encode(buffer, value);
                }

                @Override
                public Double decode(RegistryFriendlyByteBuf buffer) {
                    return net.minecraft.network.codec.ByteBufCodecs.DOUBLE.decode(buffer);
                }
            };

    // For reading/writing a list of DOUBLES
    public static final StreamCodec<RegistryFriendlyByteBuf, List<Double>> DOUBLE_LIST_STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buffer, List<Double> list) {
                    // Write size
                    INT_STREAM_CODEC.encode(buffer, list.size());
                    // Write each double
                    for (Double d : list) {
                        DOUBLE_STREAM_CODEC.encode(buffer, d);
                    }
                }

                @Override
                public List<Double> decode(RegistryFriendlyByteBuf buffer) {
                    int size = INT_STREAM_CODEC.decode(buffer);
                    List<Double> result = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        result.add(DOUBLE_STREAM_CODEC.decode(buffer));
                    }
                    return result;
                }
            };

    // For reading/writing a single STRING
    public static final StreamCodec<RegistryFriendlyByteBuf, String> STRING_STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buffer, String value) {
                    net.boulangermod.boulanger.util.StreamCodecsCompat.STRING.encode(buffer, value);
                }

                @Override
                public String decode(RegistryFriendlyByteBuf buffer) {
                    return net.boulangermod.boulanger.util.StreamCodecsCompat.STRING.decode(buffer);
                }
            };

    public static final StreamCodec<RegistryFriendlyByteBuf, List<IngredientCategory>> INGREDIENT_CATEGORY_LIST_CODEC =
            IngredientCategory.listOf();

    // Codec for a list of IngredientInfo.
    // We assume that IngredientInfo has a static field STREAM_CODEC defined. We now define a helper list codec for IngredientInfo.
    public static final StreamCodec<RegistryFriendlyByteBuf, List<IngredientInfo>> INGREDIENT_INFO_LIST =
            new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buffer, List<IngredientInfo> list) {
                    INT_STREAM_CODEC.encode(buffer, list.size());
                    for (IngredientInfo info : list) {
                        IngredientInfo.STREAM_CODEC.encode(buffer, info);
                    }
                }

                @Override
                public List<IngredientInfo> decode(RegistryFriendlyByteBuf buffer) {
                    int size = INT_STREAM_CODEC.decode(buffer);
                    List<IngredientInfo> result = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        result.add(IngredientInfo.STREAM_CODEC.decode(buffer));
                    }
                    return result;
                }
            };

    /*
     * --- End StreamCodec Helpers ---
     */

    /**
     * StreamCodec for DoughRecipeComponent that writes:
     * - recipeName (String)
     * - targetPercentages as two parallel lists (keys and values)
     * - ingredients (List<IngredientInfo>)
     * - totalWeight (int)
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, DoughRecipeComponent> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buffer, DoughRecipeComponent doughRecipe) {
                    // Encode recipeId as ResourceLocation
                    ResourceLocation.STREAM_CODEC.encode(buffer, doughRecipe.recipeId());

                    // Encode targetPercentages as two parallel lists.
                    List<IngredientCategory> keyList = new ArrayList<>(doughRecipe.targetPercentages().keySet());
                    List<Double> valueList = new ArrayList<>(doughRecipe.targetPercentages().values());
                    INGREDIENT_CATEGORY_LIST_CODEC.encode(buffer, keyList);
                    DOUBLE_LIST_STREAM_CODEC.encode(buffer, valueList);

                    // Encode the list of IngredientInfo.
                    INGREDIENT_INFO_LIST.encode(buffer, doughRecipe.ingredients());

                    // Encode totalWeight.
                    INT_STREAM_CODEC.encode(buffer, doughRecipe.totalWeight());
                }

                @Override
                public DoughRecipeComponent decode(RegistryFriendlyByteBuf buffer) {
                    ResourceLocation recipeId = ResourceLocation.STREAM_CODEC.decode(buffer);
                    List<IngredientCategory> keyList = INGREDIENT_CATEGORY_LIST_CODEC.decode(buffer);
                    List<Double> valueList = DOUBLE_LIST_STREAM_CODEC.decode(buffer);

                    Map<IngredientCategory, Double> targetPercentages = new EnumMap<>(IngredientCategory.class);
                    Iterator<IngredientCategory> keyIter = keyList.iterator();
                    Iterator<Double> valueIter = valueList.iterator();
                    while (keyIter.hasNext() && valueIter.hasNext()) {
                        targetPercentages.put(keyIter.next(), valueIter.next());
                    }

                    List<IngredientInfo> ingredients = INGREDIENT_INFO_LIST.decode(buffer);
                    int totalWeight = INT_STREAM_CODEC.decode(buffer);

                    return new DoughRecipeComponent(recipeId, targetPercentages, ingredients, totalWeight);
                }
            };

}


