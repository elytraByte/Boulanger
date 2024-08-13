package org.l3e.boulanger.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.l3e.boulanger.Boulanger;
import org.l3e.boulanger.block.ModBlocks;

import java.util.function.Supplier;

public class ModCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Boulanger.MODID);

    public static final Supplier<CreativeModeTab> BOULANGER_TAB =
            CREATIVE_MODE_TABS.register("boulanger_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.boulanger.boulanger_tab")) //translate !
                    .icon(() -> new ItemStack(ModItems.FLOUR_AP.get()))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.FLOUR_AP);
                        pOutput.accept(ModItems.FLOUR_WW);
                        pOutput.accept(ModItems.YEAST_BREWERS);
                        pOutput.accept(ModItems.SALT_KOSHER);
                        pOutput.accept(ModItems.DOUGH);
                        pOutput.accept(ModBlocks.MIXER);
                        pOutput.accept(ModBlocks.WOOD_GASIFIER);

                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
