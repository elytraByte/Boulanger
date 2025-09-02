package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.boulangermod.boulanger.recipe.ModRecipeSerializers.*;

public class RatioRecipe implements Recipe<MixingContainer> {
    private static final Logger LOG = LogManager.getLogger();

    private final ResourceLocation id;
    private final List<IngredientComponent> components;
    private final double tolerance;
    private final ItemStack result;

    // New optional size hints (grams). These are NOT required for crafting; they are UI/Divider hints.
    private final @Nullable Integer rollSizeG;
    private final @Nullable Integer loafSizeG;

    public RatioRecipe(ResourceLocation id,
                       List<IngredientComponent> components,
                       double tolerance,
                       ItemStack result,
                       @Nullable Integer rollSizeG,
                       @Nullable Integer loafSizeG) {
        this.id = Objects.requireNonNull(id);
        this.components = List.copyOf(Objects.requireNonNull(components));
        this.tolerance = tolerance;
        this.result = Objects.requireNonNull(result);
        this.rollSizeG = rollSizeG;
        this.loafSizeG = loafSizeG;

        // ---------- Advanced Logging ----------
        LOG.info("🔧 Loaded RatioRecipe: {}", id);
        LOG.info("   → Result: {}", result.getItem());
        LOG.info("   → Tolerance: {} ({}%)", tolerance, tolerance * 100.0);

        if (rollSizeG != null) LOG.info("   → Roll size hint: {} g", rollSizeG);
        if (loafSizeG != null) LOG.info("   → Loaf size hint: {} g", loafSizeG);

        if (tolerance > 1.0) {
            LOG.warn("⚠ Recipe {} has an unusually high tolerance (>100%): {}!", id, tolerance);
        } else if (tolerance > 0.5) {
            LOG.warn("⚠ Recipe {} has a high tolerance: {} ({}%)", id, tolerance, tolerance * 100.0);
        }

        for (IngredientComponent comp : components) {
            String allowed = comp.allowedItems().isEmpty()
                    ? "any"
                    : comp.allowedItems().toString();
            LOG.info("   → {}: {}% (allowed: {})",
                    comp.category(), comp.targetPercent(), allowed);
        }
        LOG.info("----------------------------------------");
    }

    /** We match in MixingBlockEntity; this stays unimplemented. */
    @Override
    public boolean matches(MixingContainer inv, Level level) {
        throw new UnsupportedOperationException("Use MixerState.findMatchingRecipe()");
    }

    @Override
    public ItemStack assemble(MixingContainer inv, HolderLookup.Provider ctx) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider ctx) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RATIO_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RATIO_TYPE.get();
    }

    public ResourceLocation getId() {
        return id;
    }

    public List<IngredientComponent> getComponents() {
        return components;
    }

    public double getTolerance() {
        return tolerance;
    }

    /** Optional roll size hint (grams) for divider UI; may be null. */
    public @Nullable Integer getRollSizeG() {
        return rollSizeG;
    }

    /** Optional loaf size hint (grams) for divider UI; may be null. */
    public @Nullable Integer getLoafSizeG() {
        return loafSizeG;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(MixingContainer inv) {
        return Recipe.super.getRemainingItems(inv);
    }

    public IngredientRequirement[] getItemRequirements() {
        return new IngredientRequirement[0];
    }

    public boolean hasOnlyOneFlour() {
        return components.stream()
                .filter(c -> c.category() == IngredientCategory.FLOUR)
                .count() == 1;
    }

    // -------------------------------------------------------------
    // SERIALIZER
    // -------------------------------------------------------------
    public static final class Serializer implements RecipeSerializer<RatioRecipe> {

        /**
         * JSON format (new):
         * {
         *   "type": "boulanger:ratio",
         *   "id": "boulanger:some_recipe",
         *   "components": [ ... ],
         *   "tolerance": 0.05,
         *   "result": { "item": "boulanger:dough" },
         *   "roll_size_g": 65,            // optional
         *   "loaf_size_g": 680            // optional
         * }
         *
         * Legacy read support:
         * - "serving_weight" (double grams) OR "serving_weight_g" (int grams) will be mapped to loaf_size_g
         *   if "loaf_size_g" is absent.
         * We never WRITE legacy fields back out.
         */
        public static final MapCodec<RatioRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                ResourceLocation.CODEC
                        .fieldOf("id")
                        .forGetter(RatioRecipe::getId),

                IngredientComponent.CODEC
                        .listOf()
                        .fieldOf("components")
                        .forGetter(RatioRecipe::getComponents),

                Codec.DOUBLE
                        .fieldOf("tolerance")
                        .forGetter(RatioRecipe::getTolerance),

                ItemStack.CODEC
                        .fieldOf("result")
                        .forGetter(r -> r.result),

                // New optional hints
                Codec.INT.optionalFieldOf("roll_size_g")
                        .forGetter(r -> Optional.ofNullable(r.getRollSizeG())),
                Codec.INT.optionalFieldOf("loaf_size_g")
                        .forGetter(r -> Optional.ofNullable(r.getLoafSizeG())),

                // Legacy (read-only)
                Codec.DOUBLE.optionalFieldOf("serving_weight")
                        .forGetter(r -> Optional.empty()),
                Codec.INT.optionalFieldOf("serving_weight_g")
                        .forGetter(r -> Optional.empty())

        ).apply(inst, (id, components, tolerance, result,
                       rollOpt, loafOpt, legacyServingOpt, legacyServingGOpt) -> {

            Integer loaf = loafOpt.orElseGet(() -> {
                if (legacyServingGOpt.isPresent()) return legacyServingGOpt.get();
                if (legacyServingOpt.isPresent()) return (int) Math.round(legacyServingOpt.get());
                return null;
            });
            Integer roll = rollOpt.orElse(null);

            return new RatioRecipe(id, components, tolerance, result, roll, loaf);
        }));

        @Override
        public MapCodec<RatioRecipe> codec() {
            return CODEC;
        }

        /**
         * Network sync: optional ints for roll & loaf. Older fields (serving weight) removed.
         */
        public static final StreamCodec<RegistryFriendlyByteBuf, RatioRecipe> STREAM_CODEC =
                StreamCodec.of(
                        (buf, r) -> {
                            // write
                            ResourceLocation.STREAM_CODEC.encode(buf, r.getId());
                            StreamCodecsCompat.list(IngredientComponent.STREAM_CODEC).encode(buf, r.getComponents());
                            StreamCodecsCompat.DOUBLE.encode(buf, r.getTolerance());
                            ItemStack.STREAM_CODEC.encode(buf, r.result);

                            // roll (optional)
                            buf.writeBoolean(r.getRollSizeG() != null);
                            if (r.getRollSizeG() != null) buf.writeVarInt(r.getRollSizeG());

                            // loaf (optional)
                            buf.writeBoolean(r.getLoafSizeG() != null);
                            if (r.getLoafSizeG() != null) buf.writeVarInt(r.getLoafSizeG());
                        },
                        buf -> {
                            // read
                            ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
                            List<IngredientComponent> comps =
                                    StreamCodecsCompat.list(IngredientComponent.STREAM_CODEC).decode(buf);
                            double tol = StreamCodecsCompat.DOUBLE.decode(buf);
                            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);

                            Integer roll = buf.readBoolean() ? buf.readVarInt() : null;
                            Integer loaf = buf.readBoolean() ? buf.readVarInt() : null;

                            return new RatioRecipe(id, comps, tol, result, roll, loaf);
                        }
                );

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RatioRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
