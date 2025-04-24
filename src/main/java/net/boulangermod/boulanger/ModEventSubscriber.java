package net.boulangermod.boulanger;

import net.boulangermod.boulanger.entity.HolsteinFriesianCowEntity;
import net.boulangermod.boulanger.entity.HenEntity;
import net.boulangermod.boulanger.entity.ModEntities;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = Boulanger.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEventSubscriber {
    @SubscribeEvent
    public static void createDefaultAttributes(EntityAttributeCreationEvent event) {
        // Hen attributes
        AttributeSupplier henAttrs = HenEntity
                .createAttributes()
                .build();
        event.put(ModEntities.HEN.get(), henAttrs);

        // Heffer attributes
        AttributeSupplier hefferAttrs = HolsteinFriesianCowEntity
                .createAttributes()
                .build();
        event.put(ModEntities.HOLSTEIN_FRIESAIN_COW.get(), hefferAttrs);
    }
}
