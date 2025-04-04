package org.l3e.boulanger;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.l3e.boulanger.datagen.FlourDataRegistry;

@EventBusSubscriber(modid = Boulanger.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModDatapackRegistries {

    @SubscribeEvent
    public static void onNewRegistry(DataPackRegistryEvent.NewRegistry event) {
        // Debug output so you know this method is firing.
        System.out.println("Registering custom datapack registry for flour_data");
        event.dataPackRegistry(
                FlourDataRegistry.FLOUR_DATA_REGISTRY_KEY,
                FlourDataRegistry.CODEC,
                FlourDataRegistry.CODEC // use the same codec for network syncing if needed
        );
    }
}
