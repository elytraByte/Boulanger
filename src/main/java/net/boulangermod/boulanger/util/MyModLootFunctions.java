// MyModLootFunctions.java
package net.boulangermod.boulanger.util;

import net.boulangermod.boulanger.Boulanger;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class MyModLootFunctions {
    public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, Boulanger.MODID);

    // register and keep the DeferredHolder
    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<CopyWheatVarietyFunction>>
            COPY_WHEAT_VARIETY = LOOT_FUNCTIONS.register("copy_wheat_variety",
            key -> new LootItemFunctionType<>(CopyWheatVarietyFunction.MAP_CODEC));

    public static void register(IEventBus bus) {
        LOOT_FUNCTIONS.register(bus);
    }

    /** helper to unwrap the DeferredHolder during loot‐table generation */
    public static LootItemFunctionType<CopyWheatVarietyFunction> copyVarietyType() {
        return COPY_WHEAT_VARIETY.value();
    }
}
