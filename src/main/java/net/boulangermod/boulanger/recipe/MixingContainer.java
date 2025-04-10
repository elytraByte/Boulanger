package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.block.entity.MixingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

public abstract class MixingContainer implements RecipeInput {
    private final MixingBlockEntity be;

    public MixingContainer(MixingBlockEntity blockEntity) {
        this.be = blockEntity;
    }

    /**
     * Return all the ItemStacks that your recipes should see.
     * Here we expose every slot in the block entity's handler:
     */
    public List<ItemStack> getItems() {
        int slots = be.getItemHandler().getSlots();
        return IntStream.range(0, slots)
                .mapToObj(be.getItemHandler()::getStackInSlot)  // getStackInSlot is available on ItemStackHandler
                .collect(Collectors.toList());
    }
}
