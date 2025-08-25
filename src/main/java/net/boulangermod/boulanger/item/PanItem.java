package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.DoughRecipeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.PanTypeComponent;
import net.boulangermod.boulanger.component.ProofingStateComponent;
import net.boulangermod.boulanger.component.IngredientInfo;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.ProcessingStep;
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
import net.minecraft.world.item.component.CustomModelData;
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

    // Ensure we backfill PAN_TYPE for crafted/picked stacks that don’t have it.
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
                if (type.getEmptyModelIndex() == modelIndex || type.getFullModelIndex() == modelIndex) {
                    stack.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent(type.getId()));
                    return;
                }
            }
        }
        // fallback to item’s constructor default
        stack.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent(panType.getId()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);

        // Always show the resolved pan type even in creative previews
        PanType resolved = resolvePanTypeForDisplay(stack);
        tooltip.add(Component.literal("Pan: " + resolved.getId()).withStyle(ChatFormatting.GOLD));

        // Dough details
        DoughRecipeComponent dr = stack.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        if (dr == null) {
            tooltip.add(Component.literal("Empty pan").withStyle(ChatFormatting.RED));
            return;
        }

        tooltip.add(Component.literal("Dough: " + dr.recipeId()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Recipe: " + dr.recipeId()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("-----").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Ingredients").withStyle(ChatFormatting.GREEN));

        int flourTotal = dr.ingredients().stream()
                .filter(info -> info.category() == IngredientCategory.FLOUR)
                .mapToInt(IngredientInfo::weight)
                .sum();

        for (IngredientInfo info : dr.ingredients()) {
            int w = info.weight();
            double pct = flourTotal > 0 ? (double) w / flourTotal * 100.0 : 0.0;
            tooltip.add(Component.literal(String.format("  %s: %dg (%.1f%%)", info.itemId(), w, pct))
                    .withStyle(ChatFormatting.GRAY));
        }

        tooltip.add(Component.literal("-----").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal(String.format("Total Weight: %dg", dr.totalWeight()))
                .withStyle(ChatFormatting.AQUA));

        ProofingStateComponent proof = stack.get(ModDataComponentTypes.PROOFING_STATE.get());
        if (proof != null) {
            tooltip.add(Component.literal(String.format("Step %d: %d ticks", proof.stepIndex(), proof.ticksInStep()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private PanType resolvePanTypeForDisplay(ItemStack stack) {
        // 1) If component is set, use it
        PanTypeComponent comp = stack.get(ModDataComponentTypes.PAN_TYPE.get());
        if (comp != null) {
            for (PanType t : PanType.values()) {
                if (t.getId().equals(comp.id())) return t;
            }
        }

        // 2) Try to infer from CustomModelData
        CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd != null) {
            int v = cmd.value();
            for (PanType t : PanType.values()) {
                if (t.getEmptyModelIndex() == v || t.getFullModelIndex() == v) return t;
            }
        }

        // 3) Fall back to the item's default constructor type
        return this.panType;
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

        if (opt.isEmpty()) return 0;

        List<ProcessingStep> steps = opt.get().getSteps();
        int idx = state.stepIndex();

        if (idx < 0) return 0;
        if (idx >= steps.size()) return 13; // fully done → full bar

        ProcessingStep step = steps.get(idx);
        float progress = (float) state.ticksInStep() / (float) step.durationTicks();
        return (int) (13f * progress);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        // Pre-tag creative/picked stacks so they’re fully identified
        stack.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent(panType.getId()));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(panType.getEmptyModelIndex()));
        return stack;
    }
}
