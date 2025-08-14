package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.screen.BakersTableMenu;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.PanTypeComponent;
import net.boulangermod.boulanger.component.ProofingStateComponent;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.boulangermod.boulanger.recipe.StepType;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.util.ScaleLogic; // if needed
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;                 // NEW
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;          // NEW
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.Optional;

public class BakersTableBlockEntity extends AbstractProcessingBlockEntity implements MenuProvider {
    public static final int DOUGH_SLOT  = 0;
    public static final int PAN_SLOT    = 1;
    public static final int OUTPUT_SLOT = 2;

    public BakersTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BAKERS_TABLE.get(), pos, state, 3);
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.BAKERS_TABLE.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.boulanger.bakers_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new BakersTableMenu(id, inv, this);
    }

    /** Ticker registration from block class */
    public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> ((BakersTableBlockEntity) be).tryShape();
    }

    public static void tick(
            Level level,
            BlockPos pos,
            BlockState state,
            BakersTableBlockEntity be
    ) {
        if (level.isClientSide()) return;
        be.tryShape();
    }

    @SuppressWarnings("unchecked")
    private static void copyKnownDoughComponents(ItemStack source, ItemStack target) {
        for (var comp : List.of(
                ModDataComponentTypes.DOUGH_RECIPE.get(),
                ModDataComponentTypes.PROOFING_STATE.get(),
                ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                ModDataComponentTypes.BAKER_PERCENTAGES.get(),
                ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(),
                ModDataComponentTypes.INGREDIENT_TYPE.get(),
                ModDataComponentTypes.PAN_TYPE.get()
        )) {
            if (source.has(comp)) {
                target.set((net.minecraft.core.component.DataComponentType<Object>) comp, source.get(comp));
            }
        }
    }

    public boolean tryShape() {
        var handler = getItemHandler();
        ItemStack dough  = handler.getStackInSlot(DOUGH_SLOT);
        ItemStack pan    = handler.getStackInSlot(PAN_SLOT);
        ItemStack output = handler.getStackInSlot(OUTPUT_SLOT);

        if (dough.isEmpty() || pan.isEmpty() || !output.isEmpty()) return false;

        // ensure dough & pan both have the right components
        var ds      = ModDataComponentTypes.PROOFING_STATE.get();
        var pt      = ModDataComponentTypes.DOUGH_PROCESS_TYPE.get();
        var panComp = ModDataComponentTypes.PAN_TYPE.get();
        if (!dough.has(ds) || !dough.has(pt) || !dough.has(panComp) || !pan.has(panComp)) {
            return false;
        }
        String required = dough.get(panComp).id();
        String present  = pan.get(panComp).id();
        if (!PanType.fromId(required).equals(PanType.fromId(present))) {
            return false;
        }

        // find the shaping recipe
        Optional<DoughProcessRecipe> recipe = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> r.getDoughType().equals(dough.get(pt)))
                .findFirst();
        if (recipe.isEmpty()) return false;

        ProcessingStep step = recipe.get().getSteps().get(dough.get(ds).stepIndex());
        if (step.type() != StepType.SHAPE) return false;

        // create a single-shaped pan
        ItemStack shapedPan = pan.copy();
        shapedPan.setCount(1);
        copyKnownDoughComponents(dough, shapedPan);
        shapedPan.set(ds, new ProofingStateComponent(
                dough.get(ds).stepIndex() + 1,
                0,
                true
        ));

        // NEW: flip model to "full" (filled pan) based on PanType
        var panTypeId = shapedPan.get(panComp).id();
        PanType panType = PanType.fromId(panTypeId);
        shapedPan.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(panType.getFullModelIndex()));

        // consume exactly one dough and one pan
        dough.shrink(1);
        pan.shrink(1);

        // write back remaining stacks (or empty if count == 0)
        if (dough.isEmpty()) handler.setStackInSlot(DOUGH_SLOT, ItemStack.EMPTY);
        else                 handler.setStackInSlot(DOUGH_SLOT, dough);

        if (pan.isEmpty()) handler.setStackInSlot(PAN_SLOT, ItemStack.EMPTY);
        else               handler.setStackInSlot(PAN_SLOT, pan);

        // place the shaped pan into the output
        handler.setStackInSlot(OUTPUT_SLOT, shapedPan);

        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("inventory", getItemHandler().serializeNBT(provider));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        getItemHandler().deserializeNBT(provider, tag.getCompound("inventory"));
    }
}