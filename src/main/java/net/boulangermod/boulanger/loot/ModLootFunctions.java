package net.boulangermod.boulanger.loot;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.util.CopyWheatVarietyFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModLootFunctions {
    private ModLootFunctions() {}

    public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, Boulanger.MOD_ID);

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<CopyWheatVarietyFunction>> COPY_WHEAT_VARIETY =
            LOOT_FUNCTIONS.register("copy_wheat_variety",
                    () -> new LootItemFunctionType<>(CopyWheatVarietyFunction.MAP_CODEC)
            );

    public static void register(IEventBus bus) {
        LOOT_FUNCTIONS.register(bus);
    }
}
