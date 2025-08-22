package net.boulangermod.boulanger.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;
import net.boulangermod.boulanger.Boulanger;
import net.minecraft.world.level.material.Fluid;

public class ModTags {
    public static final TagKey<Fluid> WOOD_GAS =
            TagKey.create(Registries.FLUID,
                    ResourceLocation.fromNamespaceAndPath("boulanger", "wood_gas"));
    private ModTags() {}
}