package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.component.value.IngredientInfo;
import net.boulangermod.boulanger.content.flour.FlourType;
import net.boulangermod.boulanger.content.ingredient.IngredientCategory;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

public record DoughRecipeComponent(
        ResourceLocation recipeId,
        @Nullable ResourceLocation processId,                        // NEW
        Map<IngredientCategory, Double> targetPercentages,
        List<IngredientInfo> ingredients,
        int totalMilligrams
) {
    // round baker % values to 0.001%
    private static final double PCT_SCALE = 1000.0;

    public DoughRecipeComponent {
        Objects.requireNonNull(recipeId, "recipeId");
        Objects.requireNonNull(targetPercentages, "targetPercentages");
        Objects.requireNonNull(ingredients, "ingredients");

        // ---- canonicalize targetPercentages: EnumMap order + rounded values ----
        EnumMap<IngredientCategory, Double> tmp = new EnumMap<>(IngredientCategory.class);
        for (var e : targetPercentages.entrySet()) {
            IngredientCategory cat = Objects.requireNonNull(e.getKey(), "targetPercentages key");
            double v = (e.getValue() == null) ? 0.0 : e.getValue();
            if (Double.isNaN(v) || Double.isInfinite(v)) v = 0.0;
            tmp.put(cat, roundPct(v));
        }
        targetPercentages = Collections.unmodifiableMap(tmp);

        // ---- canonicalize ingredients: normalize itemId + stable sort ----
        List<IngredientInfo> norm = new ArrayList<>(ingredients.size());
        for (IngredientInfo ing : ingredients) {
            if (ing == null) continue;

            norm.add(new IngredientInfo(
                    canonicalItemId(ing.itemId()),
                    Objects.requireNonNull(ing.category(), "ingredient category"),
                    Math.max(0, ing.milligrams()),
                    canonicalFlourType(ing.flourType())
            ));
        }

        norm.sort(
                Comparator.comparing(IngredientInfo::category)
                        .thenComparing(IngredientInfo::itemId)
                        .thenComparing(i -> flourIdKey(i.flourType()))
                        .thenComparingInt(IngredientInfo::milligrams)
        );

        ingredients = List.copyOf(norm);

        if (totalMilligrams < 0) totalMilligrams = 0;
    }

    /* ── Factories (both orders for convenience) ──────────────────────────── */
    public static DoughRecipeComponent of(ResourceLocation recipeId,
                                          @Nullable ResourceLocation processId,
                                          List<IngredientInfo> ingredients,
                                          int totalMilligrams,
                                          Map<IngredientCategory, Double> targetPercentages) {
        return new DoughRecipeComponent(recipeId, processId, targetPercentages, ingredients, totalMilligrams);
    }

    // Backward-compat factory for old call sites
    public static DoughRecipeComponent of(ResourceLocation recipeId,
                                          List<IngredientInfo> ingredients,
                                          int totalMilligrams,
                                          Map<IngredientCategory, Double> targetPercentages) {
        return new DoughRecipeComponent(recipeId, null, targetPercentages, ingredients, totalMilligrams);
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

    /* ── Canonicalization helpers ─────────────────────────────────────────── */
    private static double roundPct(double v) {
        return Math.round(v * PCT_SCALE) / PCT_SCALE;
    }

    private static String canonicalItemId(String raw) {
        if (raw == null) return "";
        raw = raw.trim();
        ResourceLocation rl = ResourceLocation.tryParse(raw);
        return (rl != null) ? rl.toString() : raw;
    }

    private static @Nullable FlourType canonicalFlourType(@Nullable FlourType ft) {
        if (ft == null) return null;
        String id = (ft.id() == null) ? "" : ft.id().trim().toLowerCase(Locale.ROOT);
        // keep the numeric fields exactly as provided; only normalize id casing/whitespace
        return new FlourType(id, ft.ash(), ft.protein(), ft.modelIndex(), ft.unitMg());
    }

    private static String flourIdKey(@Nullable FlourType ft) {
        if (ft == null) return "";
        String id = ft.id();
        return (id == null) ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    /* ── CODEC ────────────────────────────────────────────────────────────── */
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
                    Codec.INT.fieldOf("totalMilligrams").forGetter(DoughRecipeComponent::totalMilligrams)
            ).apply(instance, (recipeId, optProcess, keyNames, values, ingredients, totalMilligrams) -> {
                if (keyNames.size() != values.size()) {
                    throw new IllegalArgumentException("targetPercentages keys/values size mismatch");
                }

                Map<IngredientCategory, Double> percentages = new EnumMap<>(IngredientCategory.class);
                for (int i = 0; i < keyNames.size(); i++) {
                    String raw = keyNames.get(i);
                    IngredientCategory cat = IngredientCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT));
                    percentages.put(cat, values.get(i));
                }

                return new DoughRecipeComponent(recipeId, optProcess.orElse(null), percentages, ingredients, totalMilligrams);
            })
    );

    /* ── StreamCodec (RegistryFriendlyByteBuf) — write optional process id ─── */
    public static final StreamCodec<RegistryFriendlyByteBuf, DoughRecipeComponent> STREAM_CODEC =
            new StreamCodec<>() {
                @Override public void encode(RegistryFriendlyByteBuf buf, DoughRecipeComponent dr) {
                    ResourceLocation.STREAM_CODEC.encode(buf, dr.recipeId());

                    // NEW: optional process id
                    buf.writeBoolean(dr.processId() != null);
                    if (dr.processId() != null) {
                        ResourceLocation.STREAM_CODEC.encode(buf, dr.processId());
                    }

                    var keyList = dr.targetPercentages().keySet().stream().toList();
                    var valueList = dr.targetPercentages().values().stream().toList();

                    INGREDIENT_CATEGORY_LIST_CODEC.encode(buf, keyList);
                    DOUBLE_LIST_STREAM_CODEC.encode(buf, valueList);
                    INGREDIENT_INFO_LIST.encode(buf, dr.ingredients());
                    INT_STREAM_CODEC.encode(buf, dr.totalMilligrams());
                }

                @Override public DoughRecipeComponent decode(RegistryFriendlyByteBuf buf) {
                    ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);

                    // NEW: optional process id
                    ResourceLocation proc = buf.readBoolean() ? ResourceLocation.STREAM_CODEC.decode(buf) : null;

                    List<IngredientCategory> keys = INGREDIENT_CATEGORY_LIST_CODEC.decode(buf);
                    List<Double> vals = DOUBLE_LIST_STREAM_CODEC.decode(buf);
                    if (keys.size() != vals.size()) {
                        throw new IllegalStateException("targetPercentages keys/values size mismatch");
                    }

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
