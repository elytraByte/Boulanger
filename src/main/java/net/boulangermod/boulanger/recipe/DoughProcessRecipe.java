package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.content.pan.PanType;
import net.boulangermod.boulanger.item.PortionKind;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DoughProcessRecipe implements Recipe<DoughProcessInput> {

    private final List<ProcessingStep> steps;
    private final Map<PortionKind, PanServing> serving;

    public DoughProcessRecipe(List<ProcessingStep> steps, Map<PortionKind, PanServing> serving) {
        this.steps = steps != null ? List.copyOf(steps) : List.of();
        this.serving = serving != null ? Map.copyOf(serving) : Map.of();
    }

    public List<ProcessingStep> steps() { return steps; }
    public Map<PortionKind, PanServing> serving() { return serving; }

    // ---------------------------------------------------------------------
    // ID Lookup Model (Index)
    // ---------------------------------------------------------------------
    private static volatile Map<ResourceLocation, DoughProcessRecipe> INDEX = Map.of();

    /** Rebuild the id -> recipe map from the current RecipeManager (call on reload). */
    public static void rebuildIndex(RecipeManager manager) {
        Map<ResourceLocation, DoughProcessRecipe> out = new HashMap<>();
        for (RecipeHolder<DoughProcessRecipe> h : manager.getAllRecipesFor(ModRecipeTypes.DOUGH_PROCESS.get())) {
            out.put(h.id(), h.value());
        }
        INDEX = Map.copyOf(out);
    }

    /** Clears the index (optional; useful on server stop). */
    public static void clearIndex() {
        INDEX = Map.of();
    }

    /** Resolve a process recipe by its datapack id (the recipe file path id). */
    public static @Nullable DoughProcessRecipe resolve(Level level, @Nullable ResourceLocation processId) {
        if (level == null || processId == null) return null;

        DoughProcessRecipe r = INDEX.get(processId);
        if (r != null) return r;

        // Lazy rebuild fallback (covers initial access and datapack reloads if you forgot to call rebuildIndex)
        rebuildIndex(level.getRecipeManager());
        return INDEX.get(processId);
    }

    /** Resolve from an input that stores DOUGH_PROCESS_TYPE on the dough stack. */
    public static @Nullable DoughProcessRecipe resolve(Level level, DoughProcessInput input) {
        return resolve(level, input != null ? input.getDoughType() : null);
    }

    // ---------------------------------------------------------------------
    // Stream / JSON Codecs (no "id" field)
    // ---------------------------------------------------------------------
    private static final StreamCodec<RegistryFriendlyByteBuf, List<ProcessingStep>> STEP_LIST_STREAM_CODEC =
            StreamCodec.of(
                    (buf, list) -> {
                        buf.writeVarInt(list.size());
                        for (ProcessingStep s : list) ProcessingStep.STREAM_CODEC.encode(buf, s);
                    },
                    buf -> {
                        int n = buf.readVarInt();
                        var out = new java.util.ArrayList<ProcessingStep>(n);
                        for (int i = 0; i < n; i++) out.add(ProcessingStep.STREAM_CODEC.decode(buf));
                        return out;
                    }
            );

    private static final StreamCodec<RegistryFriendlyByteBuf, Map<PortionKind, PanServing>> SERVING_MAP_STREAM_CODEC =
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
                        var out = new HashMap<PortionKind, PanServing>(n);
                        for (int i = 0; i < n; i++) {
                            var k = PortionKind.STREAM_CODEC.decode(buf);
                            var v = PanServing.STREAM_CODEC.decode(buf);
                            out.put(k, v);
                        }
                        return out;
                    }
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, DoughProcessRecipe> STREAM_CODEC =
            StreamCodec.of(
                    (buf, v) -> {
                        STEP_LIST_STREAM_CODEC.encode(buf, v.steps());
                        SERVING_MAP_STREAM_CODEC.encode(buf, v.serving());
                    },
                    buf -> new DoughProcessRecipe(
                            STEP_LIST_STREAM_CODEC.decode(buf),
                            SERVING_MAP_STREAM_CODEC.decode(buf)
                    )
            );

    /**
     * NOTE: No "id" field. The recipe id is the datapack path and is available via RecipeHolder.id().
     */
    public static final MapCodec<DoughProcessRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    ProcessingStep.CODEC.listOf().fieldOf("steps").forGetter(DoughProcessRecipe::steps),
                    Codec.unboundedMap(PortionKind.CODEC, PanServing.CODEC)
                            .optionalFieldOf("serving", Map.of())
                            .forGetter(DoughProcessRecipe::serving)
            ).apply(i, DoughProcessRecipe::new)
    );

    public static final Codec<DoughProcessRecipe> CODEC = MAP_CODEC.codec();

    // ---------------------------------------------------------------------
    // Recipe implementation
    // ---------------------------------------------------------------------

    /**
     * This recipe is intended to be selected by id (via DOUGH_PROCESS_TYPE),
     * but we implement matches defensively so RecipeManager#getRecipeFor can work.
     */
    @Override
    public boolean matches(DoughProcessInput input, Level level) {
        if (input == null || level == null) return false;

        ResourceLocation wanted = input.getDoughType();
        if (wanted == null) return false;

        // Ensure the index exists; avoid repeated work after first build.
        if (INDEX.isEmpty() || !INDEX.containsKey(wanted)) {
            rebuildIndex(level.getRecipeManager());
        }

        // Match only the recipe referenced by the dough.
        return INDEX.get(wanted) == this;
    }

    @Override
    public ItemStack assemble(DoughProcessInput input, HolderLookup.Provider lookup) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider lookup) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.DOUGH_PROCESS.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.DOUGH_PROCESS.get();
    }

    // ---------------------------------------------------------------------
    // Serving helpers
    // ---------------------------------------------------------------------

    public @Nullable PanType getPanTypeFor(PortionKind kind) {
        PanServing ps = serving.get(kind);
        return ps != null ? ps.panType() : null;
    }

    public @Nullable Integer getPerPanCapacityFor(PortionKind kind) {
        PanServing ps = serving.get(kind);
        return ps != null ? ps.perPanCapacity() : null;
    }

    public @Nullable Integer getServingWeightGFor(PortionKind kind) {
        PanServing ps = serving.get(kind);
        return ps != null ? ps.servingWeightG() : null;
    }

    public @Nullable PanType getAnyPanType() {
        return serving.isEmpty() ? null : serving.values().iterator().next().panType();
    }
}
