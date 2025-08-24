package net.boulangermod.boulanger.event;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.entity.ModVillagers;
import net.boulangermod.boulanger.trade.BakerOffers;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;

@EventBusSubscriber(modid = Boulanger.MODID) // ← defaults to GAME bus
public class ModVillagerEvents {

    @SubscribeEvent
    public static void addBakerTrades(VillagerTradesEvent event) {
        if (event.getType() != ModVillagers.BAKER.value()) return;

        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

        trades.get(1).add((t, r) -> {
            // Build the exact baguette (has DOUGH_RECIPE with FlourType for each flour entry)
            ItemStack bread = BakerOffers.baguetteBread().copyWithCount(1);

            // Match ALL components on that stack (pan type, bread type, grams, baker %, dough recipe, etc.)
            DataComponentPredicate predicate = DataComponentPredicate.allOf(bread.getComponents());

            // Holder<Item> for the cost item
            var itemHolder =
                    bread.getItem().builtInRegistryHolder(); // works in Mojmaps 1.21+
            // If your mappings don’t have builtInRegistryHolder():
            // BuiltInRegistries.ITEM.wrapAsHolder(bread.getItem());

            // Cost uses: (item, count, predicate, displayStack)
            ItemCost breadCost = new ItemCost(itemHolder, 1, predicate, bread);

            return new MerchantOffer(
                    breadCost,                         // player gives THIS baguette (with full components)
                    new ItemStack(Items.EMERALD, 6),   // villager pays emeralds
                    12, 2, 0.05f
            );
        });

    }
    private static <T> void copyIfPresent(ItemStack src,
                                          DataComponentPatch.Builder pb,
                                          net.minecraft.core.component.DataComponentType<T> type) {
        T v = src.get(type);
        if (v != null) pb.set(type, v);
    }

}
