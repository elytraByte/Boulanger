package net.boulangermod.boulanger;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.entity.HolsteinFriesianCowEntity;
import net.boulangermod.boulanger.entity.HenEntity;
import net.boulangermod.boulanger.entity.ModEntities;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Boulanger.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEventSubscriber {

    private static final Logger LOGGER = LogUtils.getLogger();

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

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        LOGGER.info("📢 RegisterSpawnPlacementsEvent: registering HEN and COW");
        // Hens: ground-based peaceful creatures
        event.register(
                ModEntities.HEN.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        // Holstein Friesian Cows: similarly
        event.register(
                ModEntities.HOLSTEIN_FRIESAIN_COW.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );;
    }
}
