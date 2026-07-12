package net.boulangermod.boulanger;

import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.block.PineResinLogBlock;
import net.boulangermod.boulanger.content.flour.FiftyPoundBagType;
import net.boulangermod.boulanger.content.flour.FlourItemType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.content.WheatVariety;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Boulanger.MOD_ID);

    public static final Supplier<CreativeModeTab> BOULANGER_ITEMS_TAB =
            CREATIVE_MODE_TABS.register("boulanger_items_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.boulanger.boulanger_items_tab"))
                    .icon(() -> wheatBushelStack(WheatVariety.HARD_RED_WINTER))
                    .displayItems((pParameters, pOutput) -> {
                        for (WheatVariety v : WheatVariety.values()) {
                            pOutput.accept(wheatSeedStack(v));
                            pOutput.accept(wheatBushelStack(v));
                        }
                        pOutput.accept(ModItems.WHEAT_BERRIES);
                        pOutput.accept(ModBlocks.KAOLINITE_CLAY);
                        pOutput.accept(ModItems.KAOLINITE_CLAY_BALL);
                        pOutput.accept(ModItems.BRICK_MOLD);
                        pOutput.accept(ModItems.SPLIT_PINE_LOGS);
                        pOutput.accept(ModItems.PINE_RESIN);
                        pOutput.accept((ModBlocks.PINE_LOG));
                        pOutput.accept(resinLogStack());
                        pOutput.accept(ModBlocks.PINE_WOOD);
                        pOutput.accept(ModBlocks.STRIPPED_PINE_LOG);
                        pOutput.accept(ModBlocks.STRIPPED_PINE_WOOD);
                        pOutput.accept(ModBlocks.PINE_LEAVES);
                        pOutput.accept(ModBlocks.PINE_SAPLING);
                        pOutput.accept(ModBlocks.PINE_PLANKS);
                        pOutput.accept(ModBlocks.PINE_SLAB);
                        pOutput.accept(ModBlocks.PINE_DOOR);
                        pOutput.accept(ModBlocks.PINE_TRAPDOOR);
                        pOutput.accept(ModBlocks.PINE_FENCE);
                        pOutput.accept(ModBlocks.PINE_FENCE_GATE);
                        pOutput.accept(ModBlocks.PINE_PRESSURE_PLATE);
                        pOutput.accept(ModBlocks.PINE_BUTTON);
                        pOutput.accept(ModBlocks.PNEUMATIC_DUCT);
                        pOutput.accept(ModItems.BLIND_FLANGE);
                        pOutput.accept(ModBlocks.AIR_COMPRESSOR);
                        pOutput.accept(ModBlocks.AIR_TANK);
                        pOutput.accept(ModBlocks.VALVE_DUCT);
                        pOutput.accept(ModBlocks.ONE_WAY_VALVE_DUCT);

                    }).build());


    public static final Supplier<CreativeModeTab> BOULANGER_INGREDIENTS_BAKING =
            CREATIVE_MODE_TABS.register("boulanger_ingredients_baking", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.boulanger.boulanger_tab.ingredients_baking")) //translate !
                    .icon(() -> {
                        ItemStack iconStack = new ItemStack(ModItems.FLOUR_ITEM.get());
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

                        for (FiftyPoundBagType type : FiftyPoundBagType.values()) {
                            pOutput.accept(ModItems.createFlourBag(type));

                        }
                    }).build());




    private static ItemStack wheatSeedStack(WheatVariety variety) {
        ItemStack stack = new ItemStack(ModItems.WHEAT_SEEDS.get());
        stack.set(ModDataComponentTypes.WHEAT_VARIETY.get(), variety);
        return stack;
    }

    private static ItemStack wheatBushelStack(WheatVariety variety) {
        ItemStack stack = new ItemStack(ModItems.WHEAT_BUSHEL.get());
        stack.set(ModDataComponentTypes.WHEAT_VARIETY.get(), variety);
        return stack;
    }

    private static ItemStack resinLogStack() {
        ItemStack stack = new ItemStack(ModBlocks.PINE_LOG.get());

        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Resin Pine Log"));

        int maxResin = PineResinLogBlock.RESIN_REMAINING.getPossibleValues().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(1);

        BlockItemStateProperties props = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
                .with(PineResinLogBlock.RESIN_REMAINING, maxResin)
                .with(PineResinLogBlock.HAS_RESIN, true);

        stack.set(DataComponents.BLOCK_STATE, props);
        return stack;
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }

}
