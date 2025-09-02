package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.PanTypeComponent;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

import static net.boulangermod.boulanger.recipe.ModRecipeSerializers.*;

public class DoughProcessRecipe implements Recipe<DoughProcessInput> {
    private final ResourceLocation id;
    private final ResourceLocation doughType;
    private final @Nullable ResourceLocation panType;
    private final List<ProcessingStep> steps;

    public DoughProcessRecipe(ResourceLocation id,
                              ResourceLocation doughType,
                              @Nullable ResourceLocation panType,
                              List<ProcessingStep> steps) {
        this.id        = id;
        this.doughType = doughType;
        this.panType   = panType;
        this.steps     = List.copyOf(steps);
    }

    public ResourceLocation getId() { return id; }
    public ResourceLocation getDoughType() { return doughType; }
    public @Nullable ResourceLocation getPanType() { return panType; }
    public List<ProcessingStep> getSteps() { return steps; }

    /**
     * Optional step-skipping policy. With serving weights removed, we no longer
     * auto-skip DIVIDE based on a size target—return false for now.
     */
    public boolean canSkipStep(ItemStack dough, ProcessingStep step) {
        return false;
    }

    @Override
    public boolean matches(DoughProcessInput input, Level level) {
        // What’s stored on the dough (we stamp process-id today, old items might stamp dough_type)
        ResourceLocation tag = input.getDoughType();
        if (tag == null) return false;

        // Accept either exact process id or dough_type id for back-compat.
        if (!tag.equals(this.id) && !tag.equals(this.doughType)) {
            return false;
        }

        // Pan handling:
        // - If this process defines a panType, we only require it when a pan stack is actually provided here.
        // - Many machines (bulk proof, divide, punchdown) don't have/need a pan yet, so don't hard-fail.
        if (this.panType == null) {
            return true; // no pan requirements at all
        }

        ItemStack panStack = input.getPanStack();
        if (panStack == null) {
            return true; // allow match; step/machine logic will enforce pan when needed
        }

        if (!panStack.has(ModDataComponentTypes.PAN_TYPE.get())) {
            return false;
        }
        PanTypeComponent actual = panStack.get(ModDataComponentTypes.PAN_TYPE.get());
        if (actual == null) return false;

        ResourceLocation actualRL = ResourceLocation.tryParse(actual.id());
        return actualRL != null && actualRL.equals(this.panType);
    }

    @Override public ItemStack assemble(DoughProcessInput input, HolderLookup.Provider provider) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public ItemStack getResultItem(HolderLookup.Provider ctx) { return ItemStack.EMPTY; }
    @Override public RecipeSerializer<?> getSerializer() { return DOUGH_PROCESS_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return DOUGH_PROCESS_TYPE.get(); }

    // ------------------------------------------------------------------
    // SERIALIZER
    // ------------------------------------------------------------------
    public static final class Serializer implements RecipeSerializer<DoughProcessRecipe> {
        public static final MapCodec<DoughProcessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(DoughProcessRecipe::getId),
                ResourceLocation.CODEC.fieldOf("dough_type").forGetter(DoughProcessRecipe::getDoughType),
                // Keep pan_type required if your datagen always writes it; switch to optionalFieldOf if you want it optional.
                ResourceLocation.CODEC.fieldOf("pan_type").forGetter(DoughProcessRecipe::getPanType),
                ProcessingStep.CODEC.listOf().fieldOf("steps").forGetter(DoughProcessRecipe::getSteps)
        ).apply(instance, DoughProcessRecipe::new));

        @Override public MapCodec<DoughProcessRecipe> codec() { return CODEC; }

        public static final StreamCodec<RegistryFriendlyByteBuf, DoughProcessRecipe> STREAM_CODEC =
                StreamCodec.of(
                        // encode
                        (buf, r) -> {
                            ResourceLocation.STREAM_CODEC.encode(buf, r.getId());
                            ResourceLocation.STREAM_CODEC.encode(buf, r.getDoughType());
                            ResourceLocation.STREAM_CODEC.encode(buf, r.getPanType());
                            StreamCodecsCompat.list(ProcessingStep.STREAM_CODEC).encode(buf, r.getSteps());
                        },
                        // decode
                        buf -> {
                            ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
                            ResourceLocation doughType = ResourceLocation.STREAM_CODEC.decode(buf);
                            ResourceLocation panType = ResourceLocation.STREAM_CODEC.decode(buf);
                            List<ProcessingStep> steps =
                                    StreamCodecsCompat.list(ProcessingStep.STREAM_CODEC).decode(buf);
                            return new DoughProcessRecipe(id, doughType, panType, steps);
                        }
                );

        @Override public StreamCodec<RegistryFriendlyByteBuf, DoughProcessRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
