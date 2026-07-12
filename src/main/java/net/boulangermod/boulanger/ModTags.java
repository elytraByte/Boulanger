package net.boulangermod.boulanger;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    private ModTags() {}

    public static final class Blocks {
        private Blocks() {}

        public static final TagKey<Block> PNEUMATIC_CONNECTABLE =
                TagKey.create(Registries.BLOCK,
                        ResourceLocation.fromNamespaceAndPath(Boulanger.MOD_ID, "pneumatic_connectable"));
    }
}
