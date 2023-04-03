package org.l3e.Boulanger;

;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.l3e.Boulanger.block.ModBlocks;
import org.l3e.Boulanger.item.ModCreativeModeTabs;
import org.l3e.Boulanger.item.ModItems;
import org.slf4j.Logger;


// The value here should match an entry in the META-INF/mods.toml file
@Mod(Boulanger.MOD_ID)
public class Boulanger {
    public static final String MOD_ID = "boulanger";

    private static final Logger LOGGER = LogUtils.getLogger();

    public Boulanger() {

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);
    }


    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    private void addCreative(CreativeModeTabEvent.BuildContents event) {
        if(event.getTab() == ModCreativeModeTabs.BOULANGER_TAB) {
            event.accept(ModItems.AP_FLOUR_SMALL);
            event.accept(ModItems.B_FLOUR_SMALL);
            event.accept(ModItems.HG_FLOUR_SMALL);
            event.accept(ModItems.LR_FLOUR_SMALL);
            event.accept(ModItems.P_FLOUR_SMALL);
            event.accept(ModItems.WW_FLOUR_SMALL);
            event.accept(ModItems.SAF_RED);
        }
    }


    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HARD_RED_SPRING_WHEAT.get(), RenderType.cutout());
        }
    }
}
