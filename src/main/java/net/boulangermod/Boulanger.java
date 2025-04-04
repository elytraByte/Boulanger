package net.boulangermod;

import net.boulangermod.block.ModBlocks;
import net.boulangermod.block.entity.ModBlockEntities;
import net.boulangermod.component.ModDataComponentTypes;
import net.boulangermod.item.ModCreativeModeTabs;
import net.boulangermod.item.ModItems;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Boulanger.MODID)
public class Boulanger
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "boulanger";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Boulanger(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (Boulanger) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        ModItems.register(modEventBus);

        ModBlocks.register(modEventBus);

//        ModBlockEntities.register(modEventBus);

        ModCreativeModeTabs.register(modEventBus);

        ModDataComponentTypes.register(modEventBus);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

//        if (Config.logDirtBlock)
//            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
//
//        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
//
//        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if(event.getTab() == ModCreativeModeTabs.BOULANGER_TAB) {

            //Items
            event.accept(ModItems.WHEAT_BERRIES);
            event.accept(ModItems.BRAN);
            event.accept(ModItems.BREAK_FLOUR);
            event.accept(ModItems.MIDDLINGS_FLOUR);
            event.accept(ModItems.PATENT_FLOUR);
            event.accept(ModItems.SEMOLINA_FLOUR);
            event.accept(ModItems.FIFTY_POUND_FLOUR);
            event.accept(ModItems.WHOLE_WHEAT_FLOUR);
            event.accept(ModItems.RYE_FLOUR);
            event.accept(ModItems.BREAD_FLOUR);
            event.accept(ModItems.CAKE_FLOUR);
            event.accept(ModItems.HIGH_GLUTEN_FLOUR);
            event.accept(ModItems.WHITE_WHOLE_WHEAT_FLOUR);
            event.accept(ModItems.BUTTER);
            event.accept(ModItems.EURO_BUTTER);
            event.accept(ModItems.EURO_BUTTER_BLEND);
            event.accept(ModItems.SAF_RED);
            event.accept(ModItems.SAF_GOLD);
            event.accept(ModItems.FRESH_YEAST);
            event.accept(ModItems.FLEISCHMANN);
            event.accept(ModItems.HARD_RED_SPRING_WHEAT);
            event.accept(ModItems.SALT_KOSHER);
            event.accept(ModItems.DOUGH);
            //event.accept(ModItems.HARD_RED_SPRING_WHEAT_SEEDS);

            //Blocks
//            event.accept(ModBlocks.WOOD_GASIFIER);
//            event.accept(ModBlocks.MIXING_TABLE);
            event.accept(ModBlocks.HARD_RED_SPRING_WHEAT_CROP);
            event.accept(ModBlocks.WILD_WHEAT);
            event.accept(ModBlocks.PINE_LOG);
            event.accept(ModBlocks.PINE_WOOD);
            event.accept(ModBlocks.STRIPPED_PINE_LOG);
            event.accept(ModBlocks.STRIPPED_PINE_WOOD);
            event.accept(ModBlocks.PINE_PLANKS);
            event.accept(ModBlocks.PINE_LEAVES);
            event.accept(ModBlocks.PINE_SAPLING);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {

            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get(), RenderType.cutout());

            // GUIs
            //MenuScreens.register(ModMenuTypes.STONE_MILL_MENU.get(), StoneMillScreen::new);

            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
