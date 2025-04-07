package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.block.crops.HardRedSpringWheatCrop;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
    }


    @Override
    protected void generate() {
        dropSelf(ModBlocks.WOOD_GASIFIER.get());


//        LootItemCondition.Builder lootItemConditionBuilder = LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get())
//                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(HardRedSpringWheatCrop.AGE, 7));
//
//        this.add(ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get(), this.createCropDrops(ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get(),
//                ModItems.HARD_RED_SPRING_WHEAT_SEEDS.get(),ModItems.HARD_RED_SPRING_WHEAT_SEEDS.asItem(), lootItemConditionBuilder));

        this.dropSelf(ModBlocks.PINE_LOG.get());
        this.dropSelf(ModBlocks.PINE_WOOD.get());
        this.dropSelf(ModBlocks.PINE_PLANKS.get());
        this.dropSelf(ModBlocks.PINE_SAPLING.get());
        this.dropSelf(ModBlocks.STRIPPED_PINE_LOG.get());
        this.dropSelf(ModBlocks.STRIPPED_PINE_WOOD.get());
        this.add(ModBlocks.PINE_SAPLING.get(), block -> createLeavesDrops(block, ModBlocks.PINE_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));
        this.dropSelf(ModBlocks.WILD_WHEAT.get());
        this.dropSelf(ModBlocks.PINE_LEAVES.get());
        this.add(ModBlocks.KAOLINITE_CLAY.get(),
                createSilkTouchDispatchTable(
                        ModBlocks.KAOLINITE_CLAY.get(),
                        this.applyExplosionDecay(
                                ModBlocks.KAOLINITE_CLAY.get(),
                                LootItem.lootTableItem(ModItems.KAOLINITE_CLAY_BALL.get())
                                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(4)))
                        )
                )
        );



    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
