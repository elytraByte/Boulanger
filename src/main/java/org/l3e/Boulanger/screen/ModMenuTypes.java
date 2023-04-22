package org.l3e.Boulanger.screen;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.l3e.Boulanger.Boulanger;
import org.l3e.Boulanger.recipe.DiscSeparatorRecipe;

import javax.swing.*;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Boulanger.MOD_ID);

    public static final RegistryObject<MenuType<StoneMillMenu>> STONE_MILL_MENU =
            registerMenuType(StoneMillMenu::new, "stone_mill_menu");
    public static final RegistryObject<MenuType<ThreshingMachineMenu>> THRESHING_MACHINE_MENU =
            registerMenuType(ThreshingMachineMenu::new, "threshing_machine_menu");
    public static final RegistryObject<MenuType<SeparatorMenu>> SEPARATOR_MENU =
            registerMenuType(SeparatorMenu::new, "separator_menu");
    public static final RegistryObject<MenuType<AspiratorMenu>> ASPIRATOR_MENU =
            registerMenuType(AspiratorMenu::new, "aspirator_menu");
    public static final RegistryObject<MenuType<DestonerMenu>> DESTONER_MENU =
            registerMenuType(DestonerMenu::new, "destoner_menu");
    public static final RegistryObject<MenuType<DiscSeparatorMenu>> DISC_SEPARATOR_MENU =
            registerMenuType(DiscSeparatorMenu::new, "disc_separator_menu");

    private static <T extends AbstractContainerMenu>RegistryObject<MenuType<T>> registerMenuType(IContainerFactory<T> factory, String name) {
        return MENUS.register(name, () -> IForgeMenuType.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
