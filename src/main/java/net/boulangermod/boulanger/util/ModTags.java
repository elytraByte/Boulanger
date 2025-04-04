package net.boulangermod.boulanger.util;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;
import net.boulangermod.boulanger.Boulanger;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> MB_MASTER = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "mb_master"));
        public static final TagKey<Block> MB_SLAVE = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "mb_slave"));
    }
}