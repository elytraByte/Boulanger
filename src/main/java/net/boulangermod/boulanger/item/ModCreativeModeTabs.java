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

    public static final Supplier<CreativeModeTab> BOULANGER_TAB =
            CREATIVE_MODE_TABS.register("boulanger_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.boulanger.boulanger_tab")) //translate !
                    .icon(() -> new ItemStack(ModItems.HARD_RED_SPRING_WHEAT.get()))
                    .displayItems((pParameters, pOutput) ->

                    {
                        for (FlourItemType type : FlourItemType.values()) {
                            ItemStack stack = new ItemStack(ModItems.FLOUR_ITEM.get());
                            stack.set(ModDataComponentTypes.FLOUR_TYPE.get(), type.toFlourType());
                            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.getModelIndex()));
                            pOutput.accept(stack);
                        }



//                        pOutput.accept(new ItemStack(ModItems.SALT_KOSHER.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.HARD_RED_SPRING_WHEAT.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.WHEAT_BERRIES.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.BRAN.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.BREAK_FLOUR.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.MIDDLINGS_FLOUR.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.PATENT_FLOUR.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.RYE_FLOUR.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.SEMOLINA_FLOUR.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.WHOLE_WHEAT_FLOUR.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.ALL_PURPOSE_FLOUR.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.BREAD_FLOUR.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.HIGH_GLUTEN_FLOUR.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.VITAL_WHEAT_GLUTEN.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.FIFTY_POUND_FLOUR.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.BUTTER.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.EURO_BUTTER.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.EURO_BUTTER_BLEND.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.SAF_RED.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.SAF_GOLD.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.FLEISCHMANN.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.FRESH_YEAST.get(), 1));
//                        pOutput.accept(new ItemStack(ModItems.DOUGH.get(), 1));
//                        pOutput.accept(new ItemStack(ModBlocks.WOOD_GASIFIER.get(), 1));


                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
