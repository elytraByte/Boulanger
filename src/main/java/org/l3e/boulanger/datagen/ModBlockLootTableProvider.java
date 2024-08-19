package org.l3e.boulanger.datagen;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.l3e.boulanger.block.ModBlocks;
import org.l3e.boulanger.block.crops.HardRedSpringWheatCrop;
import org.l3e.boulanger.item.ModItems;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
    }


    @Override
    protected void generate() {
        dropSelf(ModBlocks.WOOD_GASIFIER.get());
        dropSelf(ModBlocks.MIXER.get());


        LootItemCondition.Builder lootItemConditionBuilder = LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get())
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(HardRedSpringWheatCrop.AGE, 7));
        this.add(ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get(), this.createCropDrops(ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get(),
                ModItems.HARD_RED_SPRING_WHEAT_SEEDS.get(),ModItems.HARD_RED_SPRING_WHEAT_SEEDS.asItem(), lootItemConditionBuilder));

        LootTable.Builder createWildWheatDrops(Block pBlock) {
            return this.
        }


    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
