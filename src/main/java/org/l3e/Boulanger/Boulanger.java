package org.l3e.Boulanger;

;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
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
import org.l3e.Boulanger.block.entity.ModBlockEntities;
import org.l3e.Boulanger.item.ModItems;
import org.l3e.Boulanger.recipe.ModRecipes;
import org.l3e.Boulanger.screen.*;
import org.slf4j.Logger;


// The value here should match an entry in the META-INF/mods.toml file
@Mod(Boulanger.MOD_ID)
public class Boulanger {
    public static final String MOD_ID = "boulanger";

    private static final Logger LOGGER = LogUtils.getLogger();

    public Boulanger() {

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModRecipes.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);
    }


    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    private void addCreative(CreativeModeTabEvent.BuildContents event) {
        if(event.getTab() == ModCreativeModeTabs.BOULANGER_TAB) {
            event.accept(ModItems.BREAK_FLOUR);
            event.accept(ModItems.BRAN);
            event.accept(ModItems.WHEAT_CHAFF);
            event.accept(ModItems.MIDDLINGS_FLOUR);
            event.accept(ModItems.STRAIGHT_FLOUR);
            event.accept(ModItems.PATENT_FLOUR);
            event.accept(ModItems.FIRST_CLEAR_FLOUR);
            event.accept(ModItems.SECOND_CLEAR_FLOUR);
            event.accept(ModItems.WHOLE_WHEAT_FLOUR);
            event.accept(ModItems.PAPER_BAG);
            event.accept(ModItems.AP_FLOUR_SMALL);
            event.accept(ModItems.B_FLOUR_SMALL);
            event.accept(ModItems.HG_FLOUR_SMALL);
            event.accept(ModItems.LR_FLOUR_SMALL);
            event.accept(ModItems.P_FLOUR_SMALL);
            event.accept(ModItems.WW_FLOUR_SMALL);
            event.accept(ModItems.WILD_YEAST);
            event.accept(ModItems.BREWERS_YEAST);
            event.accept(ModItems.FLEISCHEMANNS_YEAST);
            event.accept(ModItems.SAF_RED);
            event.accept(ModItems.SAF_GOLD);
            event.accept(ModItems.HARD_RED_SPRING_WHEAT_ITEM);
            event.accept(ModItems.HARD_RED_SPRING_WHEAT_SEEDS);
            event.accept(ModItems.HARD_RED_WINTER_WHEAT_ITEM);
            event.accept(ModItems.HARD_RED_WINTER_WHEAT_SEEDS);
            event.accept(ModItems.WHEAT_BERRIES);
            event.accept(ModBlocks.STONE_MILL);
            event.accept(ModBlocks.THRESHING_MACHINE);
            event.accept(ModBlocks.SEPARATOR);
            event.accept(ModBlocks.ASPIRATOR);
            event.accept(ModBlocks.DESTONER);
            event.accept(ModItems.GRINDING_STONE);
            event.accept(ModItems.GRINDING_STONE_ASSEMBLY);
            event.accept(ModBlocks.CHECKERED_BAKERY_TILE);
            event.accept(ModBlocks.BLUE_BAKERY_TILE);
            event.accept(ModBlocks.WHITE_BAKERY_TILE);
        }
    }


    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HARD_RED_WINTER_WHEAT_CROP.get(), RenderType.cutout());

            MenuScreens.register(ModMenuTypes.STONE_MILL_MENU.get(), StoneMillScreen::new);
            MenuScreens.register(ModMenuTypes.THRESHING_MACHINE_MENU.get(), ThreshingMachineScreen::new);
            MenuScreens.register(ModMenuTypes.SEPARATOR_MENU.get(), SeparatorScreen::new);
            MenuScreens.register(ModMenuTypes.ASPIRATOR_MENU.get(), AspiratorScreen::new);
            MenuScreens.register(ModMenuTypes.DESTONER_MENU.get(), DestonerScreen::new);
        }
    }
}
