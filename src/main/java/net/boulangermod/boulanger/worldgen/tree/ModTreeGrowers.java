package net.boulangermod.boulanger.worldgen.tree;

import net.minecraft.world.level.block.grower.TreeGrower;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.worldgen.ModConfiguredFeatures;

import java.util.Optional;

public class ModTreeGrowers {
    public static final TreeGrower PINE = new TreeGrower(Boulanger.MODID + "pine",
            Optional.empty(), Optional.of(ModConfiguredFeatures.PINE_TREE_KEY), Optional.empty());
}
