package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.Boulanger;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
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

    public static final DeferredHolder<MenuType<?>, MenuType<MixingBlockMenu>> MIXING_BLOCK_MENU =
            registerMenuType("mixing_block_menu", MixingBlockMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<ScaleBlockMenu>> SCALE_BLOCK_MENU =
            registerMenuType("scale_block_menu", ScaleBlockMenu::new);

    private static <T extends AbstractContainerMenu>DeferredHolder<MenuType<?>,
            MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}