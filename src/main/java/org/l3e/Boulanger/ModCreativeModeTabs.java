package org.l3e.Boulanger;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.l3e.Boulanger.Boulanger;
import org.l3e.Boulanger.item.ModItems;

@Mod.EventBusSubscriber(modid = Boulanger.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCreativeModeTabs {
    public static CreativeModeTab BOULANGER_TAB;

    @SubscribeEvent
    public static void registerCreativeModeTabs(CreativeModeTabEvent.Register event) {
        BOULANGER_TAB = event.registerCreativeModeTab(new ResourceLocation(Boulanger.MOD_ID, "boulanger_tab"),
                builder -> builder.icon(() -> new ItemStack(ModItems.AP_FLOUR_SMALL.get()))
                        .title(Component.translatable("creativemodetab.boulanger")));
    }
}
