package org.l3e.boulanger.worldgen;


import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.l3e.boulanger.Boulanger;

public class ModBiomeModifiers {

    public static final ResourceKey<BiomeModifier> ADD_WILD_WHEAT =registerKey("add_wild_wheat");
    public static final ResourceKey<BiomeModifier> ADD_PINE_TREE =registerKey("add_pine_tree");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);


        context.register(ADD_WILD_WHEAT,
                new BiomeModifiers.AddFeaturesBiomeModifier(
                        HolderSet.direct(biomes.getOrThrow(Biomes.PLAINS)),
                        HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.WILD_WHEAT)), GenerationStep.Decoration.VEGETAL_DECORATION));


        context.register(ADD_PINE_TREE,
                new BiomeModifiers.AddFeaturesBiomeModifier(HolderSet.direct(biomes.getOrThrow(Biomes.WOODED_BADLANDS), biomes.getOrThrow(Biomes.BADLANDS)),
                        HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.PINE_TREE_PLACED_KEY)),
                        GenerationStep.Decoration.VEGETAL_DECORATION));
    }


    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, name));
    }
}
