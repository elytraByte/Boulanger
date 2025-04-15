package net.boulangermod.boulanger.worldgen;


import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.boulangermod.boulanger.Boulanger;

public class  ModBiomeModifiers {

    public static final ResourceKey<BiomeModifier> ADD_WILD_WHEAT =registerKey("add_wild_wheat");
    public static final ResourceKey<BiomeModifier> ADD_PINE_TREE =registerKey("add_pine_tree");
    public static final ResourceKey<BiomeModifier> ADD_KAOLINITE_CLAY = registerKey("add_kaolinite_clay");


    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);


        context.register(ADD_WILD_WHEAT,
                new BiomeModifiers.AddFeaturesBiomeModifier(
                        HolderSet.direct(biomes.getOrThrow(Biomes.PLAINS)),
                        HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.WILD_WHEAT)), GenerationStep.Decoration.VEGETAL_DECORATION));


        context.register(ADD_PINE_TREE,
                new BiomeModifiers.AddFeaturesBiomeModifier(HolderSet.direct(biomes.getOrThrow(Biomes.WINDSWEPT_FOREST), biomes.getOrThrow(Biomes.OLD_GROWTH_PINE_TAIGA), biomes.getOrThrow((Biomes.TAIGA))),
                        HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.PINE_TREE_PLACED_KEY)),
                        GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_KAOLINITE_CLAY,
                new BiomeModifiers.AddFeaturesBiomeModifier(
                        HolderSet.direct(
                                biomes.getOrThrow(Biomes.RIVER),
                                biomes.getOrThrow(Biomes.MANGROVE_SWAMP),
                                biomes.getOrThrow(Biomes.SWAMP)
                        ),
                        HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.KAOLINITE_CLAY_PLACED_KEY)),
                        GenerationStep.Decoration.UNDERGROUND_ORES // DISK uses this step like clay
                ));
    }


    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, name));
    }
}
