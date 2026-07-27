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

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(
                    Registries.MENU,
                    Boulanger.MOD_ID
            );

    public static final DeferredHolder<
            MenuType<?>,
            MenuType<ScaleBlockMenu>
            > SCALE_MENU =
            registerMenuType(
                    "scale_menu",
                    ScaleBlockMenu::new
            );

    public static final DeferredHolder<
            MenuType<?>,
            MenuType<MilligramScaleMenu>
            > MILLIGRAM_SCALE_MENU =
            registerMenuType(
                    "milligram_scale_menu",
                    MilligramScaleMenu::new
            );

    private ModMenuTypes() {
    }

    private static <T extends AbstractContainerMenu>
    DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(
            String name,
            IContainerFactory<T> factory
    ) {
        return MENUS.register(
                name,
                () -> IMenuTypeExtension.create(factory)
        );
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}