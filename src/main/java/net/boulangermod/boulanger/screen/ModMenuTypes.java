package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.item.MilligramScaleItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Boulanger.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<WoodOvenMenu>> WOOD_OVEN_MENU =
            registerMenuType("wood_oven_menu", WoodOvenMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<SugarRefineryMenu>> SUGAR_REFINERY_MENU =
            registerMenuType("sugar_refinery_menu", SugarRefineryMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<MixingBlockMenu>> MIXING_BLOCK_MENU =
            registerMenuType("mixing_block_menu", MixingBlockMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<ScaleBlockMenu>> SCALE_BLOCK_MENU =
            registerMenuType("scale_block_menu", ScaleBlockMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<StoneMillBlockMenu>> STONE_MILL_BLOCK_MENU =
            registerMenuType("stone_mill_block_menu", StoneMillBlockMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<WoodGasifierMenu>> WOOD_GASIFIER_MENU =
            registerMenuType("wood_gasifier_menu", WoodGasifierMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<ProofingBoxMenu>> PROOFING_BOX_MENU =
            registerMenuType("proofing_box_menu", ProofingBoxMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<BakersTableMenu>> BAKERS_TABLE_MENU =
            registerMenuType("bakers_table_menu", BakersTableMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<DoughDividerMenu>> DOUGH_DIVIDER_MENU =
            registerMenuType("dough_divider_menu", DoughDividerMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<MilligramScaleMenu>> MILLIGRAM_SCALE_MENU =
            registerMenuType(
                    "milligram_scale_menu",
                    (windowId, inv, buf) -> {
                        // pull the scale stack out of whichever hand holds it:
                        Player p = inv.player;
                        ItemStack main = p.getItemInHand(InteractionHand.MAIN_HAND);
                        ItemStack stack = main.getItem() instanceof MilligramScaleItem
                                ? main
                                : p.getItemInHand(InteractionHand.OFF_HAND);
                        return new MilligramScaleMenu(windowId, inv, stack);
                    }
            );

    private static <T extends AbstractContainerMenu>DeferredHolder<MenuType<?>,
            MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}