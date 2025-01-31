package org.l3e.boulanger.worldgen.tree;

import net.minecraft.world.level.block.grower.TreeGrower;
import org.l3e.boulanger.Boulanger;
import org.l3e.boulanger.worldgen.ModConfiguredFeatures;

import java.util.Optional;

public class ModTreeGrowers {
    public static final TreeGrower PINE = new TreeGrower(Boulanger.MODID + "pine",
            Optional.empty(), Optional.of(ModConfiguredFeatures.PINE_TREE_KEY), Optional.empty());
}
