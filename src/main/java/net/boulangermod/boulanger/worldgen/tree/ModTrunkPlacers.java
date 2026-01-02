package net.boulangermod.boulanger.worldgen.tree;

import net.boulangermod.boulanger.Boulanger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

public class ModTrunkPlacers {
    public static final DeferredRegister<TrunkPlacerType<?>> TRUNK_PLACERS =
            DeferredRegister.create(Registries.TRUNK_PLACER_TYPE, Boulanger.MOD_ID);

    public static final DeferredHolder<
            TrunkPlacerType<?>,
            TrunkPlacerType<ResinPineTrunkPlacer>
            > RESIN_PINE_TRUNK_PLACER = TRUNK_PLACERS.register(
            "resin_pine_trunk_placer",
            () -> new TrunkPlacerType<>(ResinPineTrunkPlacer.CODEC)  // your MapCodec from before
    );

    public static void register(IEventBus bus) {
        TRUNK_PLACERS.register(bus);
    }
}