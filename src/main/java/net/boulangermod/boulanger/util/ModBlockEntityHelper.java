package net.boulangermod.boulanger.util;

import net.boulangermod.boulanger.screen.AbstractMachineMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntityHelper {

    public static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> registerBlockEntity(
            String name,
            Supplier<BlockEntityType<T>> factory,
            DeferredRegister<BlockEntityType<?>> register
    ) {
        return register.register(name, factory);
    }


    public static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenu(
            String name,
            MenuType.MenuSupplier<T> factory,
            DeferredRegister<MenuType<?>> register
    ) {
        return register.register(name, () -> new MenuType<>(factory, FeatureFlags.VANILLA_SET));
    }


}

