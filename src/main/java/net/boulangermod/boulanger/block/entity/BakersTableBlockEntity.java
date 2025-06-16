package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.PanTypeComponent;
import net.boulangermod.boulanger.component.ProofingStateComponent;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.boulangermod.boulanger.recipe.StepType;
import net.boulangermod.boulanger.screen.BakersTableMenu;
import net.minecraft.core.BlockPos;
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

import net.minecraft.core.component.DataComponentType;
import java.util.List;
import java.util.Optional;

public class BakersTableBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public static final int DOUGH_SLOT = 0;
    public static final int PAN_SLOT   = 1;
    public static final int OUTPUT_SLOT= 2;

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

    @SuppressWarnings("unchecked")
    public static void copyKnownDoughComponents(ItemStack source, ItemStack target) {
        // Explicitly type the component variable to avoid 'var' error
        for (DataComponentType<?> component : List.of(
                ModDataComponentTypes.DOUGH_RECIPE.get(),
                ModDataComponentTypes.PROOFING_STATE.get(),
                ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                ModDataComponentTypes.BAKER_PERCENTAGES.get(),
                ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(),
                ModDataComponentTypes.INGREDIENT_TYPE.get(),
                ModDataComponentTypes.PAN_TYPE.get()
        )) {
            if (source.has(component)) {
                // cast to Object-component for set
                target.set((DataComponentType<Object>) component, source.get(component));
            }
        }
    }

    public boolean tryShape() {
        ItemStack dough  = itemHandler.getStackInSlot(DOUGH_SLOT);
        ItemStack pan    = itemHandler.getStackInSlot(PAN_SLOT);
        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (dough.isEmpty() || pan.isEmpty() || !output.isEmpty()) return false;

        if (!dough.has(ModDataComponentTypes.PROOFING_STATE.get())
                || !dough.has(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get())) {
            return false;
        }

        // Enforce pan-type whitelist
        if (!dough.has(ModDataComponentTypes.PAN_TYPE.get())
                || !pan.has(ModDataComponentTypes.PAN_TYPE.get())) {
            return false;
        }
        String requiredId = dough.get(ModDataComponentTypes.PAN_TYPE.get()).id();
        String presentId  = pan .get(ModDataComponentTypes.PAN_TYPE.get()).id();
        PanType required = PanType.fromId(requiredId);
        PanType present  = PanType.fromId(presentId);
        if (!required.equals(present)) {
            return false;
        }

        Optional<DoughProcessRecipe> opt  = getLevel().getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> r.getDoughType().equals(dough.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get())))
                .findFirst();
        if (opt.isEmpty()) return false;
        ProcessingStep step = opt.get().getSteps()
                .get(dough.get(ModDataComponentTypes.PROOFING_STATE.get()).stepIndex());
        if (step.type() != StepType.SHAPE) return false;

        ItemStack filledPan = pan.copy();
        copyKnownDoughComponents(dough, filledPan);
        filledPan.set(ModDataComponentTypes.PROOFING_STATE.get(),
                new ProofingStateComponent(
                        dough.get(ModDataComponentTypes.PROOFING_STATE.get()).stepIndex() + 1,
                        0,
                        true
                )
        );

        itemHandler.setStackInSlot(OUTPUT_SLOT, filledPan);
        itemHandler.setStackInSlot(DOUGH_SLOT, ItemStack.EMPTY);
        itemHandler.setStackInSlot(PAN_SLOT, ItemStack.EMPTY);
        setChanged();
        return true;
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }
}
