package net.boulangermod.boulanger.worldgen;

import net.boulangermod.boulanger.util.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;

import java.util.List;

public class ModPlacedFeatures {

    public static final ResourceKey<PlacedFeature> WILD_WHEAT = registerKey("wild_wheat");
    public static final ResourceKey<PlacedFeature> PINE_TREE_PLACED_KEY = registerKey("pine_tree_placed");
    public static final ResourceKey<PlacedFeature> KAOLINITE_CLAY_PLACED_KEY = registerKey("kaolinite_clay_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        register(
                context,
                WILD_WHEAT,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.WILD_WHEAT_KEY),
                List.of(
                        InSquarePlacement.spread(),
                        PlacementUtils.HEIGHTMAP,
                        BiomeFilter.biome())
        );

        register(context, PINE_TREE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.PINE_TREE_KEY),
                VegetationPlacements.treePlacement(
                        PlacementUtils.countExtra(6,0.1f,10),
                        Blocks.SPRUCE_SAPLING));

        register(
                context,
                KAOLINITE_CLAY_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.KAOLINITE_PATCH_KEY),
                List.of(
                        RarityFilter.onAverageOnceEvery(1),                     // Like vanilla clay
                        InSquarePlacement.spread(),
                        PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                        BiomeFilter.biome()
                )
        );

    }


    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, name));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}