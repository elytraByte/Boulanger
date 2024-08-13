package org.l3e.boulanger.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.l3e.boulanger.Boulanger;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Boulanger.MODID);

    public static final DeferredItem<Item> FLOUR_AP = ITEMS.registerSimpleItem("ap_flour");
    public static final DeferredItem<Item> YEAST_BREWERS= ITEMS.registerSimpleItem("yeast_brewers");
    public static final DeferredItem<Item> SALT_KOSHER = ITEMS.registerItem("salt_kosher", SaltKosher::new, new Item.Properties());
    public static final DeferredItem<Item> DOUGH = ITEMS.registerSimpleItem("dough");
    public static final DeferredItem<Item> FLOUR_WW = ITEMS.registerSimpleItem("ww_flour");

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
