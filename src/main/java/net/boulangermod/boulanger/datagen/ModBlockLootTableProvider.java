package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.block.crop.BoulangerWheatCrop;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.content.WheatVariety;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.util.CopyWheatVarietyFunction;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {

    public ModBlockLootTableProvider(HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
    }

    @Override
    protected LootTable.Builder createCropDrops(Block cropBlock,
                                                Item grownCropItem,
                                                Item seedsItem,
                                                LootItemCondition.Builder dropGrownCropCondition) {

        return applyExplosionDecay(
                cropBlock,
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0F))
                                .add(LootItem.lootTableItem(seedsItem)
                                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                                        .apply(CopyWheatVarietyFunction.builder())
                                )
                        )

                        .withPool(LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0F))
                                .when(dropGrownCropCondition)
                                .add(LootItem.lootTableItem(seedsItem)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 2.0F)))
                                        .apply(CopyWheatVarietyFunction.builder())
                                )
                        )

                        .withPool(LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0F))
                                .when(dropGrownCropCondition)
                                .add(LootItem.lootTableItem(grownCropItem)
                                        .apply(CopyWheatVarietyFunction.builder())
                                )
                        )
        );
    }


    protected LootTable.Builder createWildWheatDrops(Block wildWheatBlock, Item seedsItem) {
        LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F));

        for (WheatVariety var : WheatVariety.values()) {
            pool.add(LootItem.lootTableItem(seedsItem)
                    .setWeight(1)
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                    .apply(SetComponentsFunction.setComponent(ModDataComponentTypes.WHEAT_VARIETY.get(), var))
            );
        }

        return applyExplosionDecay(wildWheatBlock, LootTable.lootTable().withPool(pool));
    }

    @Override
    protected void generate() {


        this.add(ModBlocks.KAOLINITE_CLAY.get(),
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1f))
                                .add(LootItem.lootTableItem(ModItems.KAOLINITE_CLAY_BALL.get())
                                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(4)))
                                )
                        )
        );
        dropSelf(ModBlocks.STRIPPED_PINE_LOG.get());
        dropSelf(ModBlocks.STRIPPED_PINE_WOOD.get());
        dropSelf(ModBlocks.PINE_LOG.get());
        dropSelf(ModBlocks.PINE_PLANKS.get());
        dropSelf(ModBlocks.PINE_WOOD.get());
        dropSelf(ModBlocks.PINE_SAPLING.get());
        this.add(ModBlocks.PINE_LEAVES.get(),
                block -> createLeavesDrops(
                        block,
                        ModBlocks.PINE_SAPLING.get(),
                        NORMAL_LEAVES_SAPLING_CHANCES
                )
        );

        dropSelf(ModBlocks.PINE_FENCE.get());
        dropSelf(ModBlocks.TREE_TAP.get());
        dropSelf(ModBlocks.PINE_FENCE_GATE.get());
        dropSelf(ModBlocks.PINE_SLAB.get());
        dropSelf(ModBlocks.PINE_STAIRS.get());
        dropSelf(ModBlocks.PINE_BUTTON.get());
        dropSelf(ModBlocks.PINE_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.PINE_DOOR.get());
        dropSelf(ModBlocks.PINE_TRAPDOOR.get());

        Block crop = ModBlocks.WHEAT_BUSHEL_BLOCK.get();
        this.add(crop, createCropDrops(
                crop,
                ModItems.WHEAT_BUSHEL.get(),
                ModItems.WHEAT_SEEDS.get(),
                LootItemBlockStatePropertyCondition.hasBlockStateProperties(crop)
                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                .hasProperty(BoulangerWheatCrop.AGE, 7)
                        )
        ));

        Block wild = ModBlocks.WILD_WHEAT.get();
        this.add(wild, createWildWheatDrops(wild, ModItems.WHEAT_SEEDS.get()));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
