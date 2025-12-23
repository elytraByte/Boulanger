package net.boulangermod.boulanger;

import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Boulanger.MOD_ID);

    public static final Supplier<CreativeModeTab> BOULANGER_ITEMS_TAB =
            CREATIVE_MODE_TABS.register("boulanger_items_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.boulanger.boulanger_items_tab"))
                    .icon(() -> new ItemStack(ModItems.WHEAT_SEEDS.get()))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.WHEAT_SEEDS);
                        pOutput.accept(ModBlocks.KAOLINITE_CLAY);

                    }).build());


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
