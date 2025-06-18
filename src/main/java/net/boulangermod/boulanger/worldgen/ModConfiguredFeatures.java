package net.boulangermod.boulanger.worldgen;


import net.boulangermod.boulanger.worldgen.tree.ResinPineTrunkPlacer;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;

public class ModConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> WILD_WHEAT_KEY = registerKey("wild_wheat_key");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PINE_TREE_KEY = registerKey("pine_tree_key");
    public static final ResourceKey<ConfiguredFeature<?, ?>> KAOLINITE_PATCH_KEY = registerKey("kaolinite_patch_key");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {

        // Wild wheat patch
        register(context, WILD_WHEAT_KEY, Feature.RANDOM_PATCH,
                new RandomPatchConfiguration(
                        96, 10, 1,
                        PlacementUtils.filtered(Feature.SIMPLE_BLOCK,
                                new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.WILD_WHEAT.get())),
                                BlockPredicate.matchesBlocks(Blocks.SHORT_GRASS)
                        )
                )
        );

        // Pine tree with random resin logs
        register(context, PINE_TREE_KEY, Feature.TREE,
                new TreeConfiguration.TreeConfigurationBuilder(
                        BlockStateProvider.simple(ModBlocks.PINE_LOG.get().defaultBlockState()),
                        // Use our custom trunk placer instead of StraightTrunkPlacer:
                        new ResinPineTrunkPlacer(8, 5, 5),
                        BlockStateProvider.simple(ModBlocks.PINE_LEAVES.get().defaultBlockState()),
                        new SpruceFoliagePlacer(ConstantInt.of(3), ConstantInt.of(5), ConstantInt.of(4)),
                        new TwoLayersFeatureSize(0, 3, 5)
                )
                        .dirt(BlockStateProvider.simple(Blocks.GRASS_BLOCK))
                        .forceDirt()
                        .build()
        );

        // Kaolinite patch
        register(context, KAOLINITE_PATCH_KEY, Feature.DISK,
                new DiskConfiguration(
                        RuleBasedBlockStateProvider.simple(ModBlocks.KAOLINITE_CLAY.get()),
                        BlockPredicate.matchesBlocks(Blocks.DIRT, Blocks.SAND),
                        UniformInt.of(1, 4), 2
                )
        );
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(
            BootstrapContext<ConfiguredFeature<?, ?>> context,
            ResourceKey<ConfiguredFeature<?, ?>> key,
            F feature,
            FC configuration
    ) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
