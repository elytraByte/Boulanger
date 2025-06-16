package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.DoughRecipeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.PanTypeComponent;
import net.boulangermod.boulanger.component.ProofingStateComponent;
import net.boulangermod.boulanger.component.IngredientInfo;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.boulangermod.boulanger.recipe.StepType;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class PanItem extends Item {
    private final PanType panType;

    public PanItem(Properties properties, PanType panType) {
        super(properties);
        this.panType = panType;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        ensurePanTypeSet(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        ensurePanTypeSet(stack);
    }

    private void ensurePanTypeSet(ItemStack stack) {
        if (stack.has(ModDataComponentTypes.PAN_TYPE.get())) return;

        if (stack.has(DataComponents.CUSTOM_MODEL_DATA)) {
            int modelIndex = stack.get(DataComponents.CUSTOM_MODEL_DATA).value();
            for (PanType type : PanType.values()) {
                if (type.getModelIndex() == modelIndex) {
                    stack.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent(type.getId()));
                    return;
                }
            }
        }

        stack.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent(panType.getId()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        // Pan Type header
        if (stack.has(ModDataComponentTypes.PAN_TYPE.get())) {
            PanTypeComponent comp = stack.get(ModDataComponentTypes.PAN_TYPE.get());
            tooltipComponents.add(
                    Component.literal("Pan: " + comp.id())
                            .withStyle(ChatFormatting.GOLD)
            );
        }

        // Dough details
        DoughRecipeComponent dr = stack.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        if (dr == null) {
            tooltipComponents.add(
                    Component.literal("Empty pan")
                            .withStyle(ChatFormatting.RED)
            );
            return;
        }

        // Dough recipe header
        tooltipComponents.add(
                Component.literal("Dough: " + dr.recipeId())
                        .withStyle(ChatFormatting.AQUA)
        );
        tooltipComponents.add(
                Component.literal("Recipe: " + dr.recipeId())
                        .withStyle(ChatFormatting.GOLD)
        );
        tooltipComponents.add(
                Component.literal("-----")
                        .withStyle(ChatFormatting.DARK_GRAY)
        );
        tooltipComponents.add(
                Component.literal("Ingredients")
                        .withStyle(ChatFormatting.GREEN)
        );

        int flourTotal = dr.ingredients().stream()
                .filter(info -> info.category() == IngredientCategory.FLOUR)
                .mapToInt(IngredientInfo::weight)
                .sum();

        for (IngredientInfo info : dr.ingredients()) {
            int w = info.weight();
            double pct = flourTotal > 0 ? (double) w / flourTotal * 100.0 : 0.0;
            tooltipComponents.add(
                    Component.literal(
                            String.format("  %s: %dg (%.1f%%)", info.itemId(), w, pct)
                    ).withStyle(ChatFormatting.GRAY)
            );
        }

        tooltipComponents.add(
                Component.literal("-----")
                        .withStyle(ChatFormatting.DARK_GRAY)
        );
        tooltipComponents.add(
                Component.literal(String.format("Total Weight: %dg", dr.totalWeight()))
                        .withStyle(ChatFormatting.AQUA)
        );

        ProofingStateComponent proof = stack.get(ModDataComponentTypes.PROOFING_STATE.get());
        if (proof != null) {
            tooltipComponents.add(
                    Component.literal(String.format("Step %d: %d ticks", proof.stepIndex(), proof.ticksInStep()))
                            .withStyle(ChatFormatting.LIGHT_PURPLE)
            );
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getBarColor(ItemStack stack) {
        // purple
        return 0xFF8800FF;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(ModDataComponentTypes.PROOFING_STATE.get());
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return 0;
        return getBarWidth(stack, level);
    }

    // Internal helper — no @Override
    public int getBarWidth(ItemStack stack, @Nullable Level level) {
        ProofingStateComponent state = stack.get(ModDataComponentTypes.PROOFING_STATE.get());
        ResourceLocation processId = stack.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
        if (state == null || processId == null || level == null) {
            return 0;
        }

        Optional<DoughProcessRecipe> opt = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> r.getDoughType().equals(processId))
                .findFirst();

        if (opt.isEmpty()) {
            return 0;
        }

        List<ProcessingStep> steps = opt.get().getSteps();
        int idx = state.stepIndex();

        if (idx < 0) {
            return 0;
        } else if (idx >= steps.size()) {
            // fully done → full bar
            return 13;
        }

        ProcessingStep step = steps.get(idx);
        float progress = (float) state.ticksInStep() / (float) step.durationTicks();
        return (int) (13f * progress);
    }
}
