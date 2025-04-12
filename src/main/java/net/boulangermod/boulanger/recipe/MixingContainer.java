package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.block.entity.MixingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class MixingContainer implements RecipeInput {
    private final MixingBlockEntity be;

    public MixingContainer(MixingBlockEntity blockEntity) {
        this.be = blockEntity;
    }

    /**
     * Return all the ItemStacks that your recipes should see.
     * Exposes every slot in the block entity's handler.
     */
    public List<ItemStack> getItems() {
        ItemStackHandler handler = be.getItemHandler();
        return IntStream.range(0, handler.getSlots())
                .mapToObj(handler::getStackInSlot)
                .collect(Collectors.toList());
    }
}