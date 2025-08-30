package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.block.WoodOvenBlock;
import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.BreadType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.screen.WoodOvenMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class WoodOvenBlockEntity extends AbstractProcessingBlockEntity implements AbstractProcessingBlock.Tickable {
    public static final int SLOT_INPUT      = 0;
    public static final int SLOT_FUEL       = 1;
    public static final int SLOT_OUTPUT     = 2;
    public static final int SLOT_PAN_RETURN = 3;  // new

    private int burnTime    = 0;
    private int maxBurnTime = 0;
    private int cookTime    = 0;
    public static final int MAX_COOK_TIME = 200;

    private final ContainerData dataAccess =
            new SimpleContainerData(4) {
                @Override
                public int get(int index) {
                    return switch (index) {
                        case 0 -> burnTime;          // remaining burn
                        case 1 -> maxBurnTime;       // burn duration
                        case 2 -> cookTime;          // elapsed cook
                        case 3 -> MAX_COOK_TIME;     // total cook time
                        default -> 0;
                    };
                }

                @Override
                public void set(int index, int value) {
                    switch (index) {
                        case 0 -> burnTime = value;
                        case 1 -> maxBurnTime = value;
                        case 2 -> cookTime = value;
                        case 3 -> { /* read-only */ }
                    }
                }

                @Override
                public int getCount() { return 4; }
            };

    public WoodOvenBlockEntity(BlockPos pos, BlockState state) {
        // now 4 slots: input, fuel, output, pan-return
        super(ModBlockEntities.WOOD_OVEN_BE.get(), pos, state, 4);
    }

    public int getBurnTime()    { return burnTime; }
    public int getMaxBurnTime() { return maxBurnTime; }
    public int getCookTime()    { return cookTime;    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        boolean wasBurning = isBurning();

        if (burnTime > 0) {
            burnTime--;
        }

        boolean canSmelt = canCook();
        ItemStack fuelStack = itemHandler.getStackInSlot(SLOT_FUEL);

        if (burnTime == 0 && canSmelt && fuelStack.getItem() == ModItems.SPLIT_PINE_LOGS.get()) {
            burnTime = (int) (160 * 1.5f); // 240 ticks
            maxBurnTime = burnTime;
            fuelStack.shrink(1);
            setChanged();
        }

        if (isBurning() && canSmelt) {
            cookTime++;
            if (cookTime >= MAX_COOK_TIME) {
                cookTime = 0;
                tryBake();
            }
        } else {
            cookTime = 0;
        }

        boolean nowBurning = isBurning();
        if (wasBurning != nowBurning) {
            setChanged();
            level.setBlock(pos, state.setValue(WoodOvenBlock.LIT, nowBurning), Block.UPDATE_CLIENTS);
            level.sendBlockUpdated(pos, state, level.getBlockState(pos), Block.UPDATE_CLIENTS);
        }
    }

    private boolean tryBake() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return false;

        ItemStack result;

        // CASE 1: fully proofed panned dough
        if (input.is(ModItems.PAN.get()) &&
                input.has(ModDataComponentTypes.PROOFING_STATE.get()) &&
                input.has(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get())) {

            var processId = input.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
            var recipeOpt = getRecipe(processId);

            if (recipeOpt.isPresent() &&
                    input.get(ModDataComponentTypes.PROOFING_STATE.get()).stepIndex() >= recipeOpt.get().getSteps().size()) {

                result = bakeBreadFromPan(input);
                itemHandler.setStackInSlot(SLOT_OUTPUT, result);

                // ✅ Robust: clone the pan and strip dough components → preserves exact PAN_TYPE/model
                ItemStack panReturn = buildEmptyPanReturn(input);
                itemHandler.setStackInSlot(SLOT_PAN_RETURN, panReturn);

                // consume input
                itemHandler.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
                setChanged();
                return true;
            }
        }

        // CASE 2: plain dough
        if (input.is(ModItems.DOUGH.get())) {
            result = bakeBreadFromPlainDough(input);
            itemHandler.setStackInSlot(SLOT_OUTPUT, result);
            itemHandler.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
            setChanged();
            return true;
        }

        return false;
    }

    /**
     * Build an empty pan return by cloning the input pan (to preserve PAN_TYPE and model)
     * and removing all dough/proofing/bread-related components.
     */
    @SuppressWarnings("unchecked")
    private ItemStack buildEmptyPanReturn(ItemStack panDough) {
        // Start from the exact item so PAN_TYPE & any other cosmetic data are preserved
        ItemStack panReturn = panDough.copy();
        panReturn.setCount(1);

        // Strip dough-related components
        for (DataComponentType<?> component : List.of(
                ModDataComponentTypes.DOUGH_RECIPE.get(),
                ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                ModDataComponentTypes.BAKER_PERCENTAGES.get(),
                ModDataComponentTypes.PROOFING_STATE.get(),
                ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(),
                ModDataComponentTypes.BREAD_TYPE.get() // in case it was ever set on the pan
        )) {
            panReturn.remove((DataComponentType<Object>) component);
        }

        // Ensure PAN_TYPE remains and model reflects it; if missing, drop to default base model
        var panType = panDough.get(ModDataComponentTypes.PAN_TYPE.get());
        if (panType != null) {
            panReturn.set(ModDataComponentTypes.PAN_TYPE.get(), panType);
            panReturn.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(panType.getModelIndex()));
        } else {
            panReturn.remove(DataComponents.CUSTOM_MODEL_DATA);
        }

        return panReturn;
    }

    private boolean canCook() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);

        if (input.is(ModItems.PAN.get()) &&
                input.has(ModDataComponentTypes.PROOFING_STATE.get()) &&
                input.has(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get())) {

            ProofingStateComponent proof = input.get(ModDataComponentTypes.PROOFING_STATE.get());
            ResourceLocation recipeId = input.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());

            return getRecipe(recipeId)
                    .map(recipe -> proof.stepIndex() >= recipe.getSteps().size())
                    .orElse(false);
        }

        return input.is(ModItems.DOUGH.get());
    }

    private Optional<DoughProcessRecipe> getRecipe(ResourceLocation recipeId) {
        if (level == null) return Optional.empty();

        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> r.getDoughType().equals(recipeId))
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    private ItemStack bakeBreadFromPan(ItemStack panDough) {
        ItemStack bread = new ItemStack(ModItems.BREAD.get());

        for (DataComponentType<?> component : List.of(
                ModDataComponentTypes.DOUGH_RECIPE.get(),
                ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                ModDataComponentTypes.BAKER_PERCENTAGES.get()
        )) {
            if (panDough.has(component)) {
                bread.set((DataComponentType<Object>) component, panDough.get(component));
            }
        }

        // Optional: carry which pan it was baked in (for tooltip/scoring)
        if (panDough.has(ModDataComponentTypes.PAN_TYPE.get())) {
            bread.set(ModDataComponentTypes.PAN_TYPE.get(), panDough.get(ModDataComponentTypes.PAN_TYPE.get()));
        }

        if (panDough.has(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get())) {
            ResourceLocation recipeId = panDough.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
            BreadType.fromRecipeId(recipeId).ifPresent(breadType -> {
                bread.set(ModDataComponentTypes.BREAD_TYPE.get(), breadType);
                bread.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(breadType.getModelIndex()));
            });
        }

        return bread;
    }

    private ItemStack bakeBreadFromPlainDough(ItemStack dough) {
        ItemStack bread = new ItemStack(ModItems.BREAD.get());

        var bakerPct = dough.get(ModDataComponentTypes.BAKER_PERCENTAGES.get());
        var doughRecipe = dough.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        var weight = dough.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());

        if (bakerPct != null)
            bread.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(), bakerPct);

        if (doughRecipe != null) {
            bread.set(ModDataComponentTypes.DOUGH_RECIPE.get(), doughRecipe);

            ResourceLocation recipeId = doughRecipe.recipeId();
            BreadType breadType = BreadType.byId(recipeId.getPath()).orElse(BreadType.BAGUETTE);
            bread.set(ModDataComponentTypes.BREAD_TYPE.get(), breadType);
            bread.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(breadType.getModelIndex()));
        }

        if (weight != null)
            bread.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), weight);

        return bread;
    }

    private boolean isBurning() {
        return burnTime > 0;
    }

    @Override
    public void drops() {
        super.drops();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("woodoven.boulanger");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new WoodOvenMenu(id, inv, this, this.dataAccess);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("Inventory", itemHandler.serializeNBT(regs));
        tag.putInt("BurnTime",    burnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
        tag.putInt("CookTime",    cookTime);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        itemHandler.deserializeNBT(regs, tag.getCompound("Inventory"));
        burnTime    = tag.getInt("BurnTime");
        maxBurnTime = tag.getInt("MaxBurnTime");
        cookTime    = tag.getInt("CookTime");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        return saveWithoutMetadata(regs);
    }
}