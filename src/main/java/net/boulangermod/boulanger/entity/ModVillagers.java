package net.boulangermod.boulanger.entity;

import com.google.common.collect.ImmutableSet;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(BuiltInRegistries.POINT_OF_INTEREST_TYPE, Boulanger.MODID);

    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(BuiltInRegistries.VILLAGER_PROFESSION, Boulanger.MODID);

    // Job site: your Baker's Table block
    public static final Holder<PoiType> BAKER_POI = POI_TYPES.register("baker_poi",
            () -> new PoiType(
                    ImmutableSet.copyOf(ModBlocks.BAKERS_TABLE.get().getStateDefinition().getPossibleStates()),
                    1, // ticket count
                    1  // search distance
            ));

    public static final Holder<VillagerProfession> BAKER = PROFESSIONS.register("baker",
            () -> new VillagerProfession(
                    "baker",
                    poi -> poi.value() == BAKER_POI.value(), // jobsite matcher
                    poi -> poi.value() == BAKER_POI.value(), // secondary/job acquisition matcher
                    ImmutableSet.of(),                        // held items
                    ImmutableSet.of(),                        // requested items
                    SoundEvents.VILLAGER_WORK_FARMER          // work sound (closest vanilla fit)
            ));

    public static void register(IEventBus bus) {
        POI_TYPES.register(bus);
        PROFESSIONS.register(bus);
    }

    private ModVillagers() {}
}