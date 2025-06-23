package net.boulangermod.boulanger;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.client.renderer.WoodGasifierRenderer;
import net.boulangermod.boulanger.command.RecipeWeightsCommand;
import net.boulangermod.boulanger.entity.HolsteinFriesianCowRenderer;
import net.boulangermod.boulanger.entity.HenRenderer;
import net.boulangermod.boulanger.entity.ModEntities;
import net.boulangermod.boulanger.fluid.ModFluids;
import net.boulangermod.boulanger.item.ModCreativeModeTabs;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.screen.*;
import net.boulangermod.boulanger.util.MyModLootFunctions;
import net.boulangermod.boulanger.worldgen.tree.ModTrunkPlacers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.item.ModItems;
import org.slf4j.Logger;

@Mod(Boulanger.MODID)
public class Boulanger {
    public static final String MODID = "boulanger";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Boulanger(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        ModCreativeModeTabs.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModDataComponentTypes.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);
        MyModLootFunctions.register(modEventBus);
        ModEntities.register(modEventBus);
        ModTrunkPlacers.TRUNK_PLACERS.register(modEventBus);
        ModFluids.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);



        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
        Config.items.forEach(item -> LOGGER.info("ITEM >> {}", item));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.HEN.get(), HenRenderer::new);
            event.registerEntityRenderer(ModEntities.HOLSTEIN_FRIESAIN_COW.get(), HolsteinFriesianCowRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.WOOD_GASIFIER_BE.get(),
                    WoodGasifierRenderer::new);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.WOOD_OVEN_MENU.get(), WoodOvenScreen::new);
            event.register(ModMenuTypes.MIXING_BLOCK_MENU.get(), MixingBlockScreen::new);
            event.register(ModMenuTypes.SCALE_BLOCK_MENU.get(), ScaleBlockScreen::new);
            event.register(ModMenuTypes.STONE_MILL_BLOCK_MENU.get(), StoneMillBlockScreen::new);
            event.register(ModMenuTypes.WOOD_GASIFIER_MENU.get(), WoodGasifierScreen::new);
            event.register(ModMenuTypes.PROOFING_BOX_MENU.get(), ProofingBoxScreen::new);
            event.register(ModMenuTypes.BAKERS_TABLE_MENU.get(), BakersTableScreen::new);
            event.register(ModMenuTypes.DOUGH_DIVIDER_MENU.get(), DoughDividerScreen::new);
            event.register(ModMenuTypes.MILLIGRAM_SCALE_MENU.get(), MilligramScaleScreen::new);

        }
    }

    //
    // —— FORGE‐BUS CLIENT TOOLTIP HANDLER ——
    // Appends “Weight: X g” for every vanilla sugar stack.
    //
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ForgeClientEvents {
        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (stack.getItem() == Items.SUGAR) {
                event.getToolTip().add(
                        Component.literal("Weight: 113 g")
                                .withStyle(ChatFormatting.GREEN)
                );
            }

        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // pass the dispatcher into your command’s constructor:
        new RecipeWeightsCommand(event.getDispatcher());
    }
}
