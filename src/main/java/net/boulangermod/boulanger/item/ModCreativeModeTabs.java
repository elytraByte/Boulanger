package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.fluid.ModFluids;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class ModCreativeModeTabs {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Boulanger.MODID);

    public static final Supplier<CreativeModeTab> BOULANGER_MAIN =
            CREATIVE_MODE_TABS.register("boulanger_main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.boulanger.boulanger_tab.main")) //translate !
                    .icon(() -> new ItemStack(ModItems.HARD_RED_SPRING_WHEAT.get()))
                    .displayItems((pParameters, pOutput) ->

                    {
                        //PINE VARIANTS
                        pOutput.accept(new ItemStack(ModBlocks.PINE_LOG.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PINE_WOOD.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.STRIPPED_PINE_LOG.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.STRIPPED_PINE_WOOD.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PINE_PLANKS.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PINE_STAIRS.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PINE_SLAB.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PINE_FENCE.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PINE_FENCE_GATE.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PINE_DOOR.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PINE_TRAPDOOR.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PINE_PRESSURE_PLATE.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PINE_BUTTON.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PINE_LEAVES.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PINE_SAPLING.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.SPLIT_PINE_LOGS.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.COPPER_CHANNEL.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.PINE_RESIN.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.KAOLINITE_CLAY.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.KAOLINITE_CLAY_BALL.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.PORCELAIN_MIX.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.BRICK_MOLD.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.WHITE_TILE.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.UNFIRED_PORCELAIN_BRICK.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.PORCELAIN_BRICK.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.BLUE_WHITE_TILE.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.L3E_TILE.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.LIGHT_BLUE_TILE.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.UNFIRED_LIGHT_BLUE_PORCELAIN_BRICK.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.LIGHT_BLUE_PORCELAIN_BRICK.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.BLUE_TILE.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.UNFIRED_BLUE_PORCELAIN_BRICK.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.BLUE_PORCELAIN_BRICK.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.BLACK_TILE.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.UNFIRED_BLACK_PORCELAIN_BRICK.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.BLACK_PORCELAIN_BRICK.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.WOODEN_BUCKET.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.BONE_ASH.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.GLASS_DUST.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.PCB.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.GILDED_PCB.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.DIORITE_BRICK.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.DIORITE_PLATE.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.REINFORCED_DIORITE_PLATE.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.IRON_WEDGE.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.SLEDGEHAMMER.get(), 1));






                        // TILE ENTITIES
                        pOutput.accept(new ItemStack(ModBlocks.TREE_TAP.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.MACHINE_HOUSING.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.STONE_MILL_BLOCK.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.SCALE_BLOCK.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.MIXING_BLOCK.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PROOFING_BOX.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.DOUGH_DIVIDER.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.BAKERS_TABLE.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.WOOD_OVEN.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.BATTERY.get(),1));
                        pOutput.accept(new ItemStack(ModBlocks.ENERGY_CABLE.get(),1));
                        pOutput.accept(new ItemStack(ModItems.GASIFIER_FILTER.get(),1));
                        pOutput.accept(new ItemStack(ModBlocks.SUGAR_REFINERY.get(),1));
                        pOutput.accept(new ItemStack(ModBlocks.WOOD_GASIFIER.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.MOTIVATOR.get(), 1));
                        pOutput.accept((new ItemStack(ModItems.MILLIGRAM_SCALE.get(), 1)));
                        pOutput.accept((new ItemStack(ModBlocks.IRON_FRAME.get(), 1)));
                        pOutput.accept((new ItemStack(ModBlocks.WOODGAS_ENGINE.get(), 1)));
                        pOutput.accept((new ItemStack(ModBlocks.GAS_TANK.get(), 1)));
                        pOutput.accept((new ItemStack(ModBlocks.FEED_THROUGH_BLOCK.get(), 1)));
                        pOutput.accept((new ItemStack(ModBlocks.WOODGAS_PIPE.get(), 1)));
                        pOutput.accept((new ItemStack(ModBlocks.WOODGAS_FLARE.get(), 1)));
                        pOutput.accept((new ItemStack(ModBlocks.WOODGAS_VALVE.get(), 1)));
                        pOutput.accept(new ItemStack(ModItems.HEN_SPAWN_EGG.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.HOLSTEIN_FRIESIAN_COW_SPAWN_EGG.get(), 1));



                    }).build());

    public static final Supplier<CreativeModeTab> BOULANGER_INGREDIENTS_BAKING =
            CREATIVE_MODE_TABS.register("boulanger_ingredients_baking", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.boulanger.boulanger_tab.ingredients_baking")) //translate !
                    .icon(() -> {
                        // Create an item stack of your FLOUR_ITEM
                        ItemStack iconStack = new ItemStack(ModItems.FLOUR_ITEM.get());
                        // Set the custom model data to your desired value, e.g., 1234
                        iconStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(17));
                        return iconStack;
                    })
                    .displayItems((pParameters, pOutput) -> {

                        for (FlourItemType type : FlourItemType.values()) {
                            ItemStack stack = new ItemStack(ModItems.FLOUR_ITEM.get());
                            stack.set(ModDataComponentTypes.FLOUR_TYPE.get(), type.toFlourType());
                            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.getModelIndex()));
                            pOutput.accept(stack);
                        }

                        for (WheatVariety variety : WheatVariety.values()) {
                            ItemStack variantStack = new ItemStack(ModItems.WHEAT_SEED.get());
                            // Set the corresponding wheat variety for the item.
                            variantStack.set(ModDataComponentTypes.WHEAT_VARIETY.get(), variety);
                            pOutput.accept(variantStack);
                        }

                        pOutput.accept(new ItemStack(ModItems.HARD_RED_SPRING_WHEAT.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.WHEAT_BERRIES.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.DOUGH.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.BROWN_SUGAR.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.POWDERED_SUGAR.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.MOLASSES.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.WHOLE_MILK.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.FANCY_EGG.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.EGG_YOLK.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.EGG_WHITE.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.WOODEN_BUCKET_OF_WHOLE_MILK.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.WOODEN_BUCKET_OF_SHELL_EGG.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.BREAD.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.SALT_KOSHER.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.BUTTER.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.EURO_BUTTER.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.EURO_BUTTER_BLEND.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.CANOLA_OIL.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.SOYBEAN_OIL.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.SAF_RED.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.SAF_GOLD.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.BREWERS_YEAST.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.FLEISCHMANN.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.FRESH_YEAST.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.RYE_SOUR_STARTER.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.SOURDOUGH_STARTER.get(), 1));
                        for (FiftyPoundBagType type : FiftyPoundBagType.values()) {
                            pOutput.accept(ModItems.createFlourBag(type));

                        }
                        pOutput.accept((new ItemStack(ModItems.PAN.get())));
// Optional: give variants a readable name in the tab
                        ItemStack loafPan = new ItemStack(ModItems.PAN.get());
                        loafPan.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(0));
                        loafPan.set(DataComponents.CUSTOM_NAME, Component.translatable("item.boulanger.pan.loaf")); // add lang key
                        pOutput.accept(loafPan);

                        ItemStack baguettePan = new ItemStack(ModItems.PAN.get());
                        baguettePan.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(4));
                        baguettePan.set(DataComponents.CUSTOM_NAME, Component.translatable("item.boulanger.pan.baguette")); // add lang key
                        pOutput.accept(baguettePan);




                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
