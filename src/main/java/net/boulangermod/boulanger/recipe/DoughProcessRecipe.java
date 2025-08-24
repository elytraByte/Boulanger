package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.PanTypeComponent;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

import static net.boulangermod.boulanger.recipe.ModRecipeSerializers.*;

public class DoughProcessRecipe implements Recipe<DoughProcessInput> {
    private final ResourceLocation id;
    private final ResourceLocation doughType;
    private final @Nullable ResourceLocation panType;
    private final List<ProcessingStep> steps;
    private final double servingWeightGrams;

    public DoughProcessRecipe(ResourceLocation id,
                              ResourceLocation doughType,
                              @Nullable ResourceLocation panType,
                              List<ProcessingStep> steps,
                              double servingWeightGrams) {
        this.id                 = id;
        this.doughType          = doughType;
        this.panType            = panType;
        this.steps              = List.copyOf(steps);
        this.servingWeightGrams = servingWeightGrams;
    }

    public ResourceLocation getDoughType() { return doughType; }
    public List<ProcessingStep> getSteps() { return steps; }
    public double getServingWeightGrams() { return servingWeightGrams; }
    public ResourceLocation getId() { return id; }
    public @Nullable ResourceLocation getPanType() { return panType; }

    public boolean canSkipStep(ItemStack dough, ProcessingStep step) {
        if (step.type() == StepType.DIVIDE) {
            WeightComponent weight = dough.get(ModDataComponentTypes.INGREDIENT_GRAMS);
            return weight != null && weight.grams() <= servingWeightGrams;
        }
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
                ResourceLocation.CODEC.fieldOf("pan_type").forGetter(DoughProcessRecipe::getPanType),
                ProcessingStep.CODEC.listOf().fieldOf("steps").forGetter(DoughProcessRecipe::getSteps),
                Codec.DOUBLE.fieldOf("serving_weight_grams").forGetter(DoughProcessRecipe::getServingWeightGrams)
        ).apply(instance, DoughProcessRecipe::new));

        @Override public MapCodec<DoughProcessRecipe> codec() { return CODEC; }

        public static final StreamCodec<RegistryFriendlyByteBuf, DoughProcessRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC, DoughProcessRecipe::getId,
                        ResourceLocation.STREAM_CODEC, DoughProcessRecipe::getDoughType,
                        ResourceLocation.STREAM_CODEC, DoughProcessRecipe::getPanType,
                        StreamCodecsCompat.list(ProcessingStep.STREAM_CODEC), DoughProcessRecipe::getSteps,
                        StreamCodecsCompat.DOUBLE, DoughProcessRecipe::getServingWeightGrams,
                        DoughProcessRecipe::new
                );

        @Override public StreamCodec<RegistryFriendlyByteBuf, DoughProcessRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
