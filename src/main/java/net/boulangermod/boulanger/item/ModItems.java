package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.Boulanger;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Boulanger.MOD_ID);

    public static final DeferredItem<Item> WHEAT_SEEDS = ITEMS.registerSimpleItem("wheat_seeds");


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
