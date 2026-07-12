package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.content.ingredient.IngredientCategory;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RatioRecipe implements Recipe<MixingContainer> {
    private static final Logger LOG = LogManager.getLogger();

    private final List<IngredientComponent> components;
    private final double tolerance;
    private final ItemStack result;

    private final @Nullable ResourceLocation processId;

    private final @Nullable Integer rollSizeG;
    private final @Nullable Integer loafSizeG;

    public RatioRecipe(
            List<IngredientComponent> components,
            double tolerance,
            ItemStack result,
            @Nullable Integer rollSizeG,
            @Nullable Integer loafSizeG,
            @Nullable ResourceLocation processId
    ) {
        this.components = List.copyOf(Objects.requireNonNull(components));
        this.tolerance = tolerance;
        this.result = Objects.requireNonNull(result);
        this.rollSizeG = rollSizeG;
        this.loafSizeG = loafSizeG;
        this.processId = processId;

        // Logging (no id anymore; the holder id is the datapack path)
        LOG.info("🔧 Loaded RatioRecipe");
        LOG.info("   → Result: {}", result.getItem());
        LOG.info("   → Tolerance: {} ({}%)", tolerance, tolerance * 100.0);
        if (processId != null) LOG.info("   → Process: {}", processId);
        if (rollSizeG != null) LOG.info("   → Roll size hint: {} g", rollSizeG);
        if (loafSizeG != null) LOG.info("   → Loaf size hint: {} g", loafSizeG);
        for (IngredientComponent comp : components) {
            String allowed = comp.allowedItems().isEmpty() ? "any" : comp.allowedItems().toString();
            LOG.info("   → {}: {}% (allowed: {})", comp.category(), comp.targetPercent(), allowed);
        }
        LOG.info("----------------------------------------");
    }

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

    public @Nullable ResourceLocation getProcessId() { return processId; }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider ctx) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.RATIO_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.RATIO.get();
    }

    public List<IngredientComponent> getComponents() { return components; }
    public double getTolerance() { return tolerance; }
    public @Nullable Integer getRollSizeG() { return rollSizeG; }
    public @Nullable Integer getLoafSizeG() { return loafSizeG; }

    @Override
    public NonNullList<ItemStack> getRemainingItems(MixingContainer inv) {
        return Recipe.super.getRemainingItems(inv);
    }

    public boolean hasOnlyOneFlour() {
        return components.stream().filter(c -> c.category() == IngredientCategory.FLOUR).count() == 1;
    }

    // -------------------------------------------------------------
    // SERIALIZER
    // -------------------------------------------------------------
    public static final class Serializer implements RecipeSerializer<RatioRecipe> {

        /**
         * NOTE: No "id" field here anymore. The recipe ID is the datapack path.
         */
        public static final MapCodec<RatioRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                IngredientComponent.CODEC.listOf().fieldOf("components").forGetter(RatioRecipe::getComponents),
                Codec.DOUBLE.fieldOf("tolerance").forGetter(RatioRecipe::getTolerance),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),

                ResourceLocation.CODEC.optionalFieldOf("process")
                        .forGetter(r -> Optional.ofNullable(r.getProcessId())),

                Codec.INT.optionalFieldOf("roll_size_g")
                        .forGetter(r -> Optional.ofNullable(r.getRollSizeG())),
                Codec.INT.optionalFieldOf("loaf_size_g")
                        .forGetter(r -> Optional.ofNullable(r.getLoafSizeG())),

                // legacy read-only (ignored; kept so old JSON fields don't crash loading)
                Codec.DOUBLE.optionalFieldOf("serving_weight").forGetter(r -> Optional.empty()),
                Codec.INT.optionalFieldOf("serving_weight_g").forGetter(r -> Optional.empty())

        ).apply(inst, (components, tolerance, result,
                       processOpt, rollOpt, loafOpt,
                       legacyServingOpt, legacyServingGOpt) -> {

            Integer loaf = loafOpt.orElseGet(() ->
                    legacyServingGOpt.orElseGet(() ->
                            legacyServingOpt.map(d -> (int) Math.round(d)).orElse(null)
                    )
            );

            return new RatioRecipe(
                    components,
                    tolerance,
                    result,
                    rollOpt.orElse(null),
                    loaf,
                    processOpt.orElse(null)
            );
        }));

        @Override
        public MapCodec<RatioRecipe> codec() {
            return CODEC;
        }

        /**
         * NOTE: Stream codec encodes recipe DATA ONLY; the recipe ID is handled externally.
         */
        public static final StreamCodec<RegistryFriendlyByteBuf, RatioRecipe> STREAM_CODEC =
                StreamCodec.of(
                        (buf, r) -> {
                            StreamCodecsCompat.list(IngredientComponent.STREAM_CODEC).encode(buf, r.getComponents());
                            StreamCodecsCompat.DOUBLE.encode(buf, r.getTolerance());
                            ItemStack.STREAM_CODEC.encode(buf, r.result);

                            buf.writeBoolean(r.getProcessId() != null);
                            if (r.getProcessId() != null) {
                                ResourceLocation.STREAM_CODEC.encode(buf, r.getProcessId());
                            }

                            buf.writeBoolean(r.getRollSizeG() != null);
                            if (r.getRollSizeG() != null) buf.writeVarInt(r.getRollSizeG());

                            buf.writeBoolean(r.getLoafSizeG() != null);
                            if (r.getLoafSizeG() != null) buf.writeVarInt(r.getLoafSizeG());
                        },
                        buf -> {
                            List<IngredientComponent> comps =
                                    StreamCodecsCompat.list(IngredientComponent.STREAM_CODEC).decode(buf);
                            double tol = StreamCodecsCompat.DOUBLE.decode(buf);
                            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);

                            ResourceLocation proc = buf.readBoolean() ? ResourceLocation.STREAM_CODEC.decode(buf) : null;

                            Integer roll = buf.readBoolean() ? buf.readVarInt() : null;
                            Integer loaf = buf.readBoolean() ? buf.readVarInt() : null;

                            return new RatioRecipe(comps, tol, result, roll, loaf, proc);
                        }
                );

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RatioRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
