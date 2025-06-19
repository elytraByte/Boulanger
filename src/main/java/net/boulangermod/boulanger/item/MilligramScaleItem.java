package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.screen.MilligramScaleMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class MilligramScaleItem extends Item implements MenuProvider {
    public MilligramScaleItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            player.openMenu(this);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("item.boulanger.milligram_scale");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        // now your menu can itself call ScaleLogic.transfer(...) when the user clicks “weigh”!
        ItemStack stack = player.getItemInHand(player.getUsedItemHand());
        return new MilligramScaleMenu(id, inv, stack);
    }
}