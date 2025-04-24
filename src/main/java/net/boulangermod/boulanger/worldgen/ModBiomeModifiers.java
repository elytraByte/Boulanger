package net.boulangermod.boulanger.worldgen;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.entity.ModEntities;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import java.util.List;

public class ModBiomeModifiers {
    private static final Logger LOGGER = LogUtils.getLogger();

    // worldgen feature keys
    public static final ResourceKey<BiomeModifier> ADD_WILD_WHEAT =
            registerKey("add_wild_wheat");
    public static final ResourceKey<BiomeModifier> ADD_PINE_TREE =
            registerKey("add_pine_tree");
    public static final ResourceKey<BiomeModifier> ADD_KAOLINITE_CLAY =
            registerKey("add_kaolinite_clay");

    // spawn keys
    public static final ResourceKey<BiomeModifier> ADD_HENS =
            registerKey("add_hens");
    public static final ResourceKey<BiomeModifier> ADD_COWS =
            registerKey("add_cows");

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(
                NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, name)
        );
    }

    /**
     * Called by your ModWorldGenProvider's RegistrySetBuilder
     */
    public static void bootstrap(BootstrapContext<BiomeModifier> ctx) {
        LOGGER.info("🪺 Bootstrapping biome modifiers (features + spawns) for {}", Boulanger.MODID);

        // look up registries
        var placedFeatures = ctx.lookup(Registries.PLACED_FEATURE);
        var biomes         = ctx.lookup(Registries.BIOME);

        // ─── FEATURES ──────────────────────────────────────────────────────────────

        // Wild wheat in Plains
        ctx.register(ADD_WILD_WHEAT, new BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(Biomes.PLAINS)),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.WILD_WHEAT)),
                GenerationStep.Decoration.VEGETAL_DECORATION
        ));

        // Pine trees in Pine & Taiga
        ctx.register(ADD_PINE_TREE, new BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(
                        biomes.getOrThrow(Biomes.WINDSWEPT_FOREST),
                        biomes.getOrThrow(Biomes.OLD_GROWTH_PINE_TAIGA),
                        biomes.getOrThrow(Biomes.TAIGA)
                ),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.PINE_TREE_PLACED_KEY)),
                GenerationStep.Decoration.VEGETAL_DECORATION
        ));

        // Kaolinite clay disks in rivers & swamps
        ctx.register(ADD_KAOLINITE_CLAY, new BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(
                        biomes.getOrThrow(Biomes.RIVER),
                        biomes.getOrThrow(Biomes.MANGROVE_SWAMP),
                        biomes.getOrThrow(Biomes.SWAMP)
                ),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.KAOLINITE_CLAY_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES
        ));


        // ─── SPAWNS ────────────────────────────────────────────────────────────────

        // Hens in all Overworld biomes
        ctx.register(ADD_HENS, new BiomeModifiers.AddSpawnsBiomeModifier(
                biomes.getOrThrow(Tags.Biomes.IS_OVERWORLD),
                List.of(new SpawnerData(ModEntities.HEN.get(), 10, 2, 4))
        ));

        // Holstein Friesian Cows in all Overworld biomes
        ctx.register(ADD_COWS, new BiomeModifiers.AddSpawnsBiomeModifier(
                biomes.getOrThrow(Tags.Biomes.IS_OVERWORLD),
                List.of(new SpawnerData(ModEntities.HOLSTEIN_FRIESAIN_COW.get(), 8, 1, 3))
        ));
    }

    /** Helper for hooking this into your ModWorldGenProvider */
    public static RegistrySetBuilder BUILDER() {
        return new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
                .add(Registries.PLACED_FEATURE,    ModPlacedFeatures::bootstrap)
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ModBiomeModifiers::bootstrap);
    }
}
