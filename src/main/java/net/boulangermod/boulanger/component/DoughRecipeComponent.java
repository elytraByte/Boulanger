package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

public record DoughRecipeComponent(
        ResourceLocation recipeId,
        @Nullable ResourceLocation processId,                        // NEW
        Map<IngredientCategory, Double> targetPercentages,
        List<IngredientInfo> ingredients,
        int totalWeight
) {

    public DoughRecipeComponent {
        Objects.requireNonNull(recipeId, "recipeId");
        Objects.requireNonNull(targetPercentages, "targetPercentages");
        Objects.requireNonNull(ingredients, "ingredients");

        // copy → EnumMap → unmodifiable, then reassign the PARAMETER
        EnumMap<IngredientCategory, Double> tmp = new EnumMap<>(IngredientCategory.class);
        tmp.putAll(targetPercentages);
        targetPercentages = Collections.unmodifiableMap(tmp);

        // make list unmodifiable (reassign the PARAMETER)
        ingredients = List.copyOf(ingredients);
        // totalWeight stays as-is
    }

    /* ── Factories (both orders for convenience) ──────────────────────────── */
    public static DoughRecipeComponent of(ResourceLocation recipeId,
                                          @Nullable ResourceLocation processId,
                                          List<IngredientInfo> ingredients,
                                          int totalWeight,
                                          Map<IngredientCategory, Double> targetPercentages) {
        return new DoughRecipeComponent(recipeId, processId, targetPercentages, ingredients, totalWeight);
    }
    // Backward-compat factory for old call sites
    public static DoughRecipeComponent of(ResourceLocation recipeId,
                                          List<IngredientInfo> ingredients,
                                          int totalWeight,
                                          Map<IngredientCategory, Double> targetPercentages) {
        return new DoughRecipeComponent(recipeId, null, targetPercentages, ingredients, totalWeight);
    }

    /* ── Helpers ──────────────────────────────────────────────────────────── */
    public ResourceLocation id() { return recipeId; }

    /** Keys as a list (enum natural order due to EnumMap). */
    public List<IngredientCategory> targetPercentagesKeys() {
        return new ArrayList<>(targetPercentages.keySet());
    }

    /** Values as a list (aligned with keys because EnumMap preserves enum order). */
    public List<Double> targetPercentagesValues() {
        return new ArrayList<>(targetPercentages.values());
    }

    /* ── CODEC (unchanged from yours, with a tiny safety check) ───────────── */
    /* ===== Codec (add optional "process") ===== */
    public static final Codec<DoughRecipeComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("recipeId").forGetter(DoughRecipeComponent::recipeId),
                    ResourceLocation.CODEC.optionalFieldOf("process")
                            .forGetter(dc -> Optional.ofNullable(dc.processId)),         // NEW
                    Codec.STRING.listOf().fieldOf("targetPercentagesKeys").forGetter(
                            dc -> dc.targetPercentages().keySet().stream().map(Enum::name).toList()
                    ),
                    Codec.DOUBLE.listOf().fieldOf("targetPercentagesValues").forGetter(
                            DoughRecipeComponent::targetPercentagesValues
                    ),
                    IngredientInfo.CODEC.listOf().fieldOf("ingredients").forGetter(DoughRecipeComponent::ingredients),
                    Codec.INT.fieldOf("totalWeight").forGetter(DoughRecipeComponent::totalWeight)
            ).apply(instance, (recipeId, optProcess, keyNames, values, ingredients, totalWeight) -> {
                if (keyNames.size() != values.size())
                    throw new IllegalArgumentException("targetPercentages keys/values size mismatch");
                Map<IngredientCategory, Double> percentages = new EnumMap<>(IngredientCategory.class);
                for (int i = 0; i < keyNames.size(); i++) {
                    percentages.put(IngredientCategory.valueOf(keyNames.get(i)), values.get(i));
                }
                return new DoughRecipeComponent(recipeId, optProcess.orElse(null), percentages, ingredients, totalWeight);
            })
    );

    /* ===== StreamCodec (RegistryFriendlyByteBuf) — write optional process id ===== */
    public static final StreamCodec<RegistryFriendlyByteBuf, DoughRecipeComponent> STREAM_CODEC =
            new StreamCodec<>() {
                @Override public void encode(RegistryFriendlyByteBuf buf, DoughRecipeComponent dr) {
                    ResourceLocation.STREAM_CODEC.encode(buf, dr.recipeId());
                    // NEW: optional process id
                    buf.writeBoolean(dr.processId() != null);
                    if (dr.processId() != null) {
                        ResourceLocation.STREAM_CODEC.encode(buf, dr.processId());
                    }
                    // existing fields
                    var keyList = dr.targetPercentages().keySet().stream().toList();
                    var valueList = dr.targetPercentages().values().stream().toList();
                    INGREDIENT_CATEGORY_LIST_CODEC.encode(buf, keyList);
                    DOUBLE_LIST_STREAM_CODEC.encode(buf, valueList);
                    INGREDIENT_INFO_LIST.encode(buf, dr.ingredients());
                    INT_STREAM_CODEC.encode(buf, dr.totalWeight());
                }
                @Override public DoughRecipeComponent decode(RegistryFriendlyByteBuf buf) {
                    ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
                    // NEW: optional process id
                    ResourceLocation proc = buf.readBoolean() ? ResourceLocation.STREAM_CODEC.decode(buf) : null;

                    List<IngredientCategory> keys = INGREDIENT_CATEGORY_LIST_CODEC.decode(buf);
                    List<Double> vals = DOUBLE_LIST_STREAM_CODEC.decode(buf);
                    if (keys.size() != vals.size())
                        throw new IllegalStateException("targetPercentages keys/values size mismatch");
                    Map<IngredientCategory, Double> targets = new EnumMap<>(IngredientCategory.class);
                    for (int i = 0; i < keys.size(); i++) targets.put(keys.get(i), vals.get(i));
                    List<IngredientInfo> ings = INGREDIENT_INFO_LIST.decode(buf);
                    int total = INT_STREAM_CODEC.decode(buf);
                    return new DoughRecipeComponent(id, proc, targets, ings, total);
                }
            };

    /* ── StreamCodec helpers (unchanged) ──────────────────────────────────── */
    public static final StreamCodec<RegistryFriendlyByteBuf, Integer> INT_STREAM_CODEC =
            new StreamCodec<>() {
                @Override public void encode(RegistryFriendlyByteBuf buf, Integer v) {
                    net.minecraft.network.codec.ByteBufCodecs.INT.encode(buf, v);
                }
                @Override public Integer decode(RegistryFriendlyByteBuf buf) {
                    return net.minecraft.network.codec.ByteBufCodecs.INT.decode(buf);
                }
            };

    public static final StreamCodec<RegistryFriendlyByteBuf, Double> DOUBLE_STREAM_CODEC =
            new StreamCodec<>() {
                @Override public void encode(RegistryFriendlyByteBuf buf, Double v) {
                    net.minecraft.network.codec.ByteBufCodecs.DOUBLE.encode(buf, v);
                }
                @Override public Double decode(RegistryFriendlyByteBuf buf) {
                    return net.minecraft.network.codec.ByteBufCodecs.DOUBLE.decode(buf);
                }
            };

    public static final StreamCodec<RegistryFriendlyByteBuf, List<Double>> DOUBLE_LIST_STREAM_CODEC =
            new StreamCodec<>() {
                @Override public void encode(RegistryFriendlyByteBuf buf, List<Double> list) {
                    INT_STREAM_CODEC.encode(buf, list.size());
                    for (Double d : list) DOUBLE_STREAM_CODEC.encode(buf, d);
                }
                @Override public List<Double> decode(RegistryFriendlyByteBuf buf) {
                    int size = INT_STREAM_CODEC.decode(buf);
                    List<Double> out = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) out.add(DOUBLE_STREAM_CODEC.decode(buf));
                    return out;
                }
            };

    public static final StreamCodec<RegistryFriendlyByteBuf, String> STRING_STREAM_CODEC =
            new StreamCodec<>() {
                @Override public void encode(RegistryFriendlyByteBuf buf, String v) {
                    StreamCodecsCompat.STRING.encode(buf, v);
                }
                @Override public String decode(RegistryFriendlyByteBuf buf) {
                    return StreamCodecsCompat.STRING.decode(buf);
                }
            };

    public static final StreamCodec<RegistryFriendlyByteBuf, List<IngredientCategory>> INGREDIENT_CATEGORY_LIST_CODEC =
            IngredientCategory.listOf();

    public static final StreamCodec<RegistryFriendlyByteBuf, List<IngredientInfo>> INGREDIENT_INFO_LIST =
            new StreamCodec<>() {
                @Override public void encode(RegistryFriendlyByteBuf buf, List<IngredientInfo> list) {
                    INT_STREAM_CODEC.encode(buf, list.size());
                    for (IngredientInfo info : list) IngredientInfo.STREAM_CODEC.encode(buf, info);
                }
                @Override public List<IngredientInfo> decode(RegistryFriendlyByteBuf buf) {
                    int size = INT_STREAM_CODEC.decode(buf);
                    List<IngredientInfo> out = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) out.add(IngredientInfo.STREAM_CODEC.decode(buf));
                    return out;
                }
            };
}
