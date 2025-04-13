package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;

import java.util.function.Supplier;

public class ModCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Boulanger.MODID);

    public static final Supplier<CreativeModeTab> BOULANGER_MAIN =
            CREATIVE_MODE_TABS.register("boulanger_main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.boulanger.boulanger_tab")) //translate !
                    .icon(() -> new ItemStack(ModItems.HARD_RED_SPRING_WHEAT.get()))
                    .displayItems((pParameters, pOutput) ->

                    {

                        pOutput.accept(new ItemStack(ModItems.DOUGH.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.BREAD.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.SALT_KOSHER.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.BUTTER.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.EURO_BUTTER.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.EURO_BUTTER_BLEND.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.SAF_RED.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.SAF_GOLD.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.FLEISCHMANN.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.FRESH_YEAST.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.WOOD_GASIFIER.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.STONE_MILL_BLOCK.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.WOOD_OVEN.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.MIXING_BLOCK.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.SCALE_BLOCK.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PINE_LOG.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.STRIPPED_PINE_LOG.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.STRIPPED_PINE_WOOD.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PINE_PLANKS.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PINE_LEAVES.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.PINE_SAPLING.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.SPLIT_PINE_LOGS.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.IRON_WEDGE.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.SLEDGEHAMMER.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.KAOLINITE_CLAY.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.KAOLINITE_CLAY_BALL.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.PORCELAIN_MIX.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.UNFIRED_PORCELAIN_BRICK.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.PORCELAIN_BRICK.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.BONE_ASH.get(), 1));
                        pOutput.accept(new ItemStack(ModItems.GLASS_DUST.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.L3E_TILE.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.BLUE_TILE.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.DARK_BLUE_TILE.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.DARK_BLUE_WHITE_TILE.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.BLACK_TILE.get(), 1));
                        pOutput.accept(new ItemStack(ModBlocks.WHITE_TILE.get(), 1));



                    }).build());

    public static final Supplier<CreativeModeTab> BOULANGER_FLOUR =
            CREATIVE_MODE_TABS.register("boulanger_flour", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.boulanger.boulanger_tab")) //translate !
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
//                        pOutput.accept(new ItemStack(ModItems.WHEAT_SEED.get(), 1));


                    }).build());


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
