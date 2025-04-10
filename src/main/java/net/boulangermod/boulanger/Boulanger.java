package net.boulangermod.boulanger;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.network.StartMixingPacket;
import net.boulangermod.boulanger.recipe.ModMixingRecipes;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.screen.MixingBlockScreen;
import net.boulangermod.boulanger.screen.ModMenuTypes;
import net.boulangermod.boulanger.screen.WoodOvenScreen;
import net.boulangermod.boulanger.screen.ScaleBlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.item.ModCreativeModeTabs;
import net.boulangermod.boulanger.item.ModItems;
import org.slf4j.Logger;

//this is a comment for the commit/text commit/push

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Boulanger.MODID)
public class Boulanger {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "boulanger";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Boulanger(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        ModCreativeModeTabs.register(modEventBus);

        ModBlocks.register(modEventBus);

        ModItems.register(modEventBus);

        ModBlockEntities.register(modEventBus);

        ModMenuTypes.register(modEventBus);

        ModDataComponentTypes.register(modEventBus);

        ModRecipeSerializers.register(modEventBus);


        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));

        ModMixingRecipes.registerDefaults();
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());


        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {

            event.register(ModMenuTypes.WOOD_OVEN_MENU.get(), WoodOvenScreen::new);
            event.register(ModMenuTypes.MIXING_BLOCK_MENU.get(), MixingBlockScreen::new);
            event.register(ModMenuTypes.SCALE_BLOCK_MENU.get(), ScaleBlockScreen::new);

        }
    }
}
