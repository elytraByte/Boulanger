package net.boulangermod.boulanger.event;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.component.DoughRecipeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.PanTypeComponent;
import net.boulangermod.boulanger.entity.ModVillagers;
import net.boulangermod.boulanger.trade.BakerOffers;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;

@EventBusSubscriber(modid = Boulanger.MODID)
public class ModVillagerEvents {

    @SubscribeEvent
    public static void addBakerTrades(VillagerTradesEvent event) {
        if (event.getType() != ModVillagers.BAKER.value()) return;

        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();


        trades.get(1).add((t, r) -> {
            if (!(t.level() instanceof ServerLevel level)) return null;
            ItemStack display = BakerOffers.baguetteBread(level).copyWithCount(1);
            return makeBreadBuyOffer(display, 6, 12, 2);

        });

        trades.get(1).add((t, r) -> {
            if (!(t.level() instanceof ServerLevel level)) return null;
            ItemStack display = BakerOffers.wholeWheatBread(level).copyWithCount(1);
            return makeBreadBuyOffer(display, 6, 12, 2);

        });

        trades.get(1).add((t, r) -> {
            if (!(t.level() instanceof ServerLevel level)) return null;
            ItemStack display = BakerOffers.banhMiBread(level).copyWithCount(1);
            return makeBreadBuyOffer(display, 6, 12, 2);

        });
    }

    private static MerchantOffer makeBreadBuyOffer(ItemStack displayBread,
                                                   int emeralds, int maxUses, int villagerXp) {
        ensureBreadVisuals(displayBread); // sets CMD from BreadType if missing

        var builder = DataComponentPredicate.builder();

        var breadType = displayBread.get(ModDataComponentTypes.BREAD_TYPE.get());
        if (breadType != null) {
            builder.expect(ModDataComponentTypes.BREAD_TYPE.get(), breadType);
            builder.expect(DataComponents.CUSTOM_MODEL_DATA,
                    new CustomModelData(breadType.getModelIndex()));
        }

        var pan = displayBread.get(ModDataComponentTypes.PAN_TYPE.get());
        if (pan != null) {
            // pan is canonical now thanks to PanTypeComponent constructor
            builder.expect(ModDataComponentTypes.PAN_TYPE.get(), pan);
        }

        // Do NOT expect DOUGH_PROCESS_TYPE or DOUGH_RECIPE here.

        ItemCost cost = new ItemCost(
                displayBread.getItem().builtInRegistryHolder(),
                displayBread.getCount(),
                builder.build(),
                displayBread.copy()
        );
        return new MerchantOffer(cost, new ItemStack(Items.EMERALD, emeralds), maxUses, villagerXp, 0.05F);
    }

    private static void ensureBreadVisuals(ItemStack stack) {
        var type = stack.get(ModDataComponentTypes.BREAD_TYPE.get());
        if (type != null) {
            int expected = type.getModelIndex();
            var cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
            if (cmd == null || cmd.value() != expected) {
                stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(expected));
            }
        }
    }
}