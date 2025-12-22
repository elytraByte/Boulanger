package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.item.PortionKind;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minutes-native dough process recipe.
 * - steps: ordered list of ProcessingStep (each stores minutes)
 * - serving: per portion kind rules (pan type, per-pan capacity, serving weight, etc.)
 */
public final class DoughProcessRecipe implements Recipe<RecipeInput> {

    // ===== Fields =====
    private final ResourceLocation id;
    private final List<ProcessingStep> steps;                  // ordered pipeline (minutes-native)
    private final Map<PortionKind, PanServing> serving;        // per-portion rules (weight, pan, capacity)

    // ===== Construction =====
    public DoughProcessRecipe(ResourceLocation id,
                              List<ProcessingStep> steps,
                              Map<PortionKind, PanServing> serving) {
        this.id = id;
        this.steps = steps != null ? List.copyOf(steps) : List.of();
        this.serving = serving != null ? Map.copyOf(serving) : Map.of();
    }

    // ===== Accessors =====
    public ResourceLocation id() { return id; }
    public List<ProcessingStep> steps() { return steps; }
    public Map<PortionKind, PanServing> serving() { return serving; }

    private static final StreamCodec<RegistryFriendlyByteBuf, ResourceLocation> RL_STREAM_CODEC =
            StreamCodec.of(
                    (buf, id) -> buf.writeResourceLocation(id),
                    buf -> buf.readResourceLocation()
            );

    private static final StreamCodec<RegistryFriendlyByteBuf, java.util.List<ProcessingStep>> STEP_LIST_STREAM_CODEC =
            StreamCodec.of(
                    (buf, list) -> {
                        buf.writeVarInt(list.size());
                        for (ProcessingStep s : list) ProcessingStep.STREAM_CODEC.encode(buf, s);
                    },
                    buf -> {
                        int n = buf.readVarInt();
                        java.util.ArrayList<ProcessingStep> out = new java.util.ArrayList<>(n);
                        for (int i = 0; i < n; i++) out.add(ProcessingStep.STREAM_CODEC.decode(buf));
                        return out;
                    }
            );

    private static final StreamCodec<RegistryFriendlyByteBuf, java.util.Map<PortionKind, PanServing>> SERVING_MAP_STREAM_CODEC =
            StreamCodec.of(
                    (buf, map) -> {
                        buf.writeVarInt(map.size());
                        for (var e : map.entrySet()) {
                            PortionKind.STREAM_CODEC.encode(buf, e.getKey());
                            PanServing.STREAM_CODEC.encode(buf, e.getValue());
                        }
                    },
                    buf -> {
                        int n = buf.readVarInt();
                        java.util.HashMap<PortionKind, PanServing> out = new java.util.HashMap<>(n);
                        for (int i = 0; i < n; i++) {
                            var k = PortionKind.STREAM_CODEC.decode(buf);
                            var v = PanServing.STREAM_CODEC.decode(buf);
                            out.put(k, v);
                        }
                        return out;
                    }
            );

    // final recipe codec (manual encode/decode for 3 fields)
    public static final StreamCodec<RegistryFriendlyByteBuf, DoughProcessRecipe> STREAM_CODEC =
            StreamCodec.of(
                    (buf, v) -> {
                        RL_STREAM_CODEC.encode(buf, v.id());
                        STEP_LIST_STREAM_CODEC.encode(buf, v.steps());
                        SERVING_MAP_STREAM_CODEC.encode(buf, v.serving());
                    },
                    buf -> new DoughProcessRecipe(
                            RL_STREAM_CODEC.decode(buf),
                            STEP_LIST_STREAM_CODEC.decode(buf),
                            SERVING_MAP_STREAM_CODEC.decode(buf)
                    )
            );

    // JSON codec (MapCodec required by RecipeSerializer.codec())
    public static final MapCodec<DoughProcessRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(DoughProcessRecipe::id),
            ProcessingStep.CODEC.listOf().fieldOf("steps").forGetter(DoughProcessRecipe::steps),
            // value must be Codec<PanServing>, not MapCodec
            Codec.unboundedMap(PortionKind.CODEC, PanServing.VALUE_CODEC)
                    .fieldOf("serving")
                    .forGetter(DoughProcessRecipe::serving)
    ).apply(i, DoughProcessRecipe::new));

    // Optional value codec if you use it elsewhere
    public static final Codec<DoughProcessRecipe> CODEC = MAP_CODEC.codec();


    // ===== Recipe<?> impl (recipe-like holder; not craftable directly) =====
    @Override public boolean matches(RecipeInput input, net.minecraft.world.level.Level level) { return false; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider lookup) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int w, int h) { return false; }
    @Override public ItemStack getResultItem(HolderLookup.Provider lookup) { return ItemStack.EMPTY; }

    @Override public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.DOUGH_PROCESS.get();
    }

    @Override public RecipeType<?> getType() {
        return ModRecipeTypes.DOUGH_PROCESS.get();
    }

    // ===== Convenience helpers =====
    public ResourceLocation getId() { return id; }

    public @org.jetbrains.annotations.Nullable PanType getPanTypeFor(PortionKind kind) {
        PanServing ps = serving.get(kind);
        return ps != null ? ps.panType() : null;
    }

    public @org.jetbrains.annotations.Nullable Integer getPerPanCapacityFor(PortionKind kind) {
        PanServing ps = serving.get(kind);
        return ps != null ? ps.perPanCapacity() : null;
    }

    public @org.jetbrains.annotations.Nullable Integer getServingWeightGFor(PortionKind kind) {
        PanServing ps = serving.get(kind);
        return ps != null ? ps.servingWeightG() : null;
    }

    public @org.jetbrains.annotations.Nullable PanType getAnyPanType() {
        return serving.isEmpty() ? null : serving.values().iterator().next().panType();
    }
}
