package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.PanTypeComponent;
import net.boulangermod.boulanger.component.ProofingStateComponent;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.boulangermod.boulanger.recipe.StepType;
import net.boulangermod.boulanger.screen.BakersTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.Optional;

public class BakersTableBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public BakersTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BAKERS_TABLE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.boulanger.bakers_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new BakersTableMenu(containerId, inv, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BakersTableBlockEntity blockEntity) {
        if (level == null || level.isClientSide) return;
        blockEntity.tryShape();
    }

    public static void copyKnownDoughComponents(ItemStack source, ItemStack target) {
        for (DataComponentType<?> component : List.of(
                ModDataComponentTypes.DOUGH_RECIPE.get(),
                ModDataComponentTypes.PROOFING_STATE.get(),
                ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                ModDataComponentTypes.BAKER_PERCENTAGES.get(),
                ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(),
                ModDataComponentTypes.INGREDIENT_TYPE.get()
        )) {
            if (source.has(component)) {
                // Unsafe cast okay for controlled use
                target.set((DataComponentType<Object>) component, source.get(component));
            }
        }
    }

    public boolean tryShape() {
        ItemStack dough = itemHandler.getStackInSlot(0);
        ItemStack pan   = itemHandler.getStackInSlot(1);
        ItemStack output= itemHandler.getStackInSlot(2);

        if (dough.isEmpty() || pan.isEmpty() || !output.isEmpty()) return false;

        // 1) Check proofing state & recipe
        if (!dough.has(ModDataComponentTypes.PROOFING_STATE.get())
                || !dough.has(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get())) return false;

        // 2) Ensure this exact pan stack has a PanType component
        if (!pan.has(ModDataComponentTypes.PAN_TYPE.get())) {
            return false;
        }

        // 3) Lookup the shaping step
        ProofingStateComponent stateComp = dough.get(ModDataComponentTypes.PROOFING_STATE.get());
        ResourceLocation recipeId         = dough.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
        Optional<DoughProcessRecipe> opt  = getLevel().getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> r.getDoughType().equals(recipeId))
                .findFirst();
        if (opt.isEmpty()) return false;
        DoughProcessRecipe recipe = opt.get();
        ProcessingStep step       = recipe.getSteps().get(stateComp.stepIndex());
        if (step.type() != StepType.SHAPE) return false;

        // 4) Create a copy of the pan so we keep its item, model-data, etc.
        ItemStack filledPan = pan.copy();

        // 5) Copy the dough components into that pan
        copyKnownDoughComponents(dough, filledPan);

        // 6) Advance proofing step
        filledPan.set(ModDataComponentTypes.PROOFING_STATE.get(),
                new ProofingStateComponent(stateComp.stepIndex() + 1, 0, true)
        );

        // 7) Consume inputs & set result
        itemHandler.setStackInSlot(2, filledPan);
        itemHandler.setStackInSlot(0, ItemStack.EMPTY);
        itemHandler.setStackInSlot(1, ItemStack.EMPTY);

        setChanged();
        return true;
    }




    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }
}
