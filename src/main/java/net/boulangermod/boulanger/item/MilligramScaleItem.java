package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.screen.MilligramScaleMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MilligramScaleItem extends Item {
    public static final int MAX_CAPACITY_MILLIGRAMS =
            5_000;

    public MilligramScaleItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack scale = player.getItemInHand(hand);

        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (
                                    containerId,
                                    inventory,
                                    menuPlayer
                            ) -> new MilligramScaleMenu(
                                    containerId,
                                    inventory,
                                    hand
                            ),
                            Component.translatable(
                                    "item.boulanger.milligram_scale"
                            )
                    ),
                    buffer -> buffer.writeEnum(hand)
            );
        }

        return InteractionResultHolder.sidedSuccess(
                scale,
                level.isClientSide()
        );
    }
}