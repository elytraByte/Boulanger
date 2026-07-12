package net.boulangermod.boulanger.content.ingredient.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.content.ingredient.IngredientCategory;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Authoritative "ingredient chemistry" and semantic traits used by
 * recipe matching + hydration/gluten computation + scoring.
 *
 * Fractions are "by weight": e.g. whole egg waterFraction ~0.75 means:
 * 100g egg contributes ~75g water-equivalent.
 */
public record IngredientProfile(
        ResourceLocation id,
        IngredientCategory category,

        // Fractions by weight [0..1]
        double waterFraction,
        double fatFraction,
        double sugarFraction,
        double saltFraction,

        // Optional specializations
        @Nullable FlourSpec flour,
        @Nullable YeastSpec yeast,

        // Semantic traits (not Minecraft item tags)
        List<ResourceLocation> traits
) {
    public IngredientProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(category, "category");
        traits = List.copyOf(Objects.requireNonNull(traits, "traits"));

        validateFraction("waterFraction", waterFraction);
        validateFraction("fatFraction", fatFraction);
        validateFraction("sugarFraction", sugarFraction);
        validateFraction("saltFraction", saltFraction);
    }

    private static void validateFraction(String name, double v) {
        if (v < 0.0 || v > 1.0) {
            throw new IllegalArgumentException(name + " must be in [0,1] but was " + v);
        }
    }

    public double waterEquivalentGrams(double ingredientGrams) {
        return ingredientGrams * waterFraction;
    }

    public double fatEquivalentGrams(double ingredientGrams) {
        return ingredientGrams * fatFraction;
    }

    public double sugarEquivalentGrams(double ingredientGrams) {
        return ingredientGrams * sugarFraction;
    }

    public double saltEquivalentGrams(double ingredientGrams) {
        return ingredientGrams * saltFraction;
    }

    // ----------------------------
    // CODEC (JSON)
    // ----------------------------
    public static final Codec<IngredientProfile> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(IngredientProfile::id),
            IngredientCategory.CODEC.fieldOf("category").forGetter(IngredientProfile::category),

            Codec.DOUBLE.optionalFieldOf("water_fraction", 0.0).forGetter(IngredientProfile::waterFraction),
            Codec.DOUBLE.optionalFieldOf("fat_fraction", 0.0).forGetter(IngredientProfile::fatFraction),
            Codec.DOUBLE.optionalFieldOf("sugar_fraction", 0.0).forGetter(IngredientProfile::sugarFraction),
            Codec.DOUBLE.optionalFieldOf("salt_fraction", 0.0).forGetter(IngredientProfile::saltFraction),

            FlourSpec.CODEC.optionalFieldOf("flour").forGetter(p -> java.util.Optional.ofNullable(p.flour)),
            YeastSpec.CODEC.optionalFieldOf("yeast").forGetter(p -> java.util.Optional.ofNullable(p.yeast)),

            ResourceLocation.CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(IngredientProfile::traits)
    ).apply(inst, (id, cat, w, f, s, sa, flourOpt, yeastOpt, traits) ->
            new IngredientProfile(id, cat, w, f, s, sa, flourOpt.orElse(null), yeastOpt.orElse(null), traits)
    ));

    // ----------------------------
    // STREAM_CODEC (network sync)
    // ----------------------------
    private static final StreamCodec<RegistryFriendlyByteBuf, List<ResourceLocation>> RL_LIST_CODEC =
            new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buf, List<ResourceLocation> value) {
                    buf.writeVarInt(value.size());
                    for (ResourceLocation rl : value) {
                        ResourceLocation.STREAM_CODEC.encode(buf, rl);
                    }
                }

                @Override
                public List<ResourceLocation> decode(RegistryFriendlyByteBuf buf) {
                    int n = buf.readVarInt();
                    List<ResourceLocation> out = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) {
                        out.add(ResourceLocation.STREAM_CODEC.decode(buf));
                    }
                    return out;
                }
            };

    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientProfile> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        ResourceLocation.STREAM_CODEC.encode(buf, p.id());
                        IngredientCategory.STREAM_CODEC.encode(buf, p.category());

                        StreamCodecsCompat.DOUBLE.encode(buf, p.waterFraction());
                        StreamCodecsCompat.DOUBLE.encode(buf, p.fatFraction());
                        StreamCodecsCompat.DOUBLE.encode(buf, p.sugarFraction());
                        StreamCodecsCompat.DOUBLE.encode(buf, p.saltFraction());

                        buf.writeBoolean(p.flour() != null);
                        if (p.flour() != null) FlourSpec.STREAM_CODEC.encode(buf, p.flour());

                        buf.writeBoolean(p.yeast() != null);
                        if (p.yeast() != null) YeastSpec.STREAM_CODEC.encode(buf, p.yeast());

                        RL_LIST_CODEC.encode(buf, p.traits());
                    },
                    buf -> {
                        ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
                        IngredientCategory cat = IngredientCategory.STREAM_CODEC.decode(buf);

                        double w = StreamCodecsCompat.DOUBLE.decode(buf);
                        double f = StreamCodecsCompat.DOUBLE.decode(buf);
                        double s = StreamCodecsCompat.DOUBLE.decode(buf);
                        double sa = StreamCodecsCompat.DOUBLE.decode(buf);

                        FlourSpec flour = buf.readBoolean() ? FlourSpec.STREAM_CODEC.decode(buf) : null;
                        YeastSpec yeast = buf.readBoolean() ? YeastSpec.STREAM_CODEC.decode(buf) : null;

                        List<ResourceLocation> traits = RL_LIST_CODEC.decode(buf);

                        return new IngredientProfile(id, cat, w, f, s, sa, flour, yeast, traits);
                    }
            );

    // ----------------------------
    // Nested specs
    // ----------------------------
    public record FlourSpec(
            double proteinPercent,
            double ashPercent,
            double absorptionFactor,
            double glutenStrengthFactor
    ) {
        public FlourSpec {
            if (proteinPercent < 0.0) throw new IllegalArgumentException("proteinPercent must be >= 0");
            if (ashPercent < 0.0) throw new IllegalArgumentException("ashPercent must be >= 0");
        }

        public static final Codec<FlourSpec> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.DOUBLE.fieldOf("protein").forGetter(FlourSpec::proteinPercent),
                Codec.DOUBLE.fieldOf("ash").forGetter(FlourSpec::ashPercent),
                Codec.DOUBLE.optionalFieldOf("absorption", 1.0).forGetter(FlourSpec::absorptionFactor),
                Codec.DOUBLE.optionalFieldOf("gluten_strength", 1.0).forGetter(FlourSpec::glutenStrengthFactor)
        ).apply(inst, FlourSpec::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, FlourSpec> STREAM_CODEC =
                StreamCodec.of(
                        (buf, v) -> {
                            StreamCodecsCompat.DOUBLE.encode(buf, v.proteinPercent());
                            StreamCodecsCompat.DOUBLE.encode(buf, v.ashPercent());
                            StreamCodecsCompat.DOUBLE.encode(buf, v.absorptionFactor());
                            StreamCodecsCompat.DOUBLE.encode(buf, v.glutenStrengthFactor());
                        },
                        buf -> new FlourSpec(
                                StreamCodecsCompat.DOUBLE.decode(buf),
                                StreamCodecsCompat.DOUBLE.decode(buf),
                                StreamCodecsCompat.DOUBLE.decode(buf),
                                StreamCodecsCompat.DOUBLE.decode(buf)
                        )
                );
    }

    public record YeastSpec(
            double fermentationRate,
            double flavorIntensity
    ) {
        public static final Codec<YeastSpec> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.DOUBLE.optionalFieldOf("fermentation_rate", 1.0).forGetter(YeastSpec::fermentationRate),
                Codec.DOUBLE.optionalFieldOf("flavor", 1.0).forGetter(YeastSpec::flavorIntensity)
        ).apply(inst, YeastSpec::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, YeastSpec> STREAM_CODEC =
                StreamCodec.of(
                        (buf, v) -> {
                            StreamCodecsCompat.DOUBLE.encode(buf, v.fermentationRate());
                            StreamCodecsCompat.DOUBLE.encode(buf, v.flavorIntensity());
                        },
                        buf -> new YeastSpec(
                                StreamCodecsCompat.DOUBLE.decode(buf),
                                StreamCodecsCompat.DOUBLE.decode(buf)
                        )
                );
    }
}
