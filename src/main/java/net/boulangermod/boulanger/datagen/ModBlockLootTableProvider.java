package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.block.crops.HardRedSpringWheatCrop;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.item.WheatVariety;
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
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    public ModBlockLootTableProvider(HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
    }

    /**
     * Override the vanilla helper so we can inject our CopyWheatVarietyFunction
     * and only run the seed pool when the crop is mature.
     */
    @Override
    protected LootTable.Builder createCropDrops(Block cropBlock,
                                                Item grownCropItem,
                                                Item seedsItem,
                                                LootItemCondition.Builder dropGrownCropCondition) {
        return applyExplosionDecay(
                cropBlock,
                LootTable.lootTable()
                        // Seed pool: only if mature
                        .withPool(LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1f))
                                .when(dropGrownCropCondition)                                // maturity check
                                .add(LootItem.lootTableItem(seedsItem)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1f, 3f)))
                                        .apply(CopyWheatVarietyFunction.builder())
                                )
                        )
                        // Wheat item pool: only if mature
                        .withPool(LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1f))
                                .when(dropGrownCropCondition)
                                .add(LootItem.lootTableItem(grownCropItem))
                        )
        );
    }

    /**
     * Wild wheat: one seed of a random variety.
     */
    protected LootTable.Builder createWildWheatDrops(Block cropBlock, Item seedsItem) {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1f));

        // each variety equally likely
        float weight = 1f / WheatVariety.values().length;
        for (WheatVariety var : WheatVariety.values()) {
            pool.add(LootItem.lootTableItem(seedsItem)
                    .when(LootItemRandomChanceCondition.randomChance(weight))
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1f)))
                    .apply(net.minecraft.world.level.storage.loot.functions.SetComponentsFunction.setComponent(
                            ModDataComponentTypes.WHEAT_VARIETY.get(), var
                    ))
            );
        }

        return applyExplosionDecay(
                cropBlock,
                LootTable.lootTable().withPool(pool)
        );
    }

    @Override
    protected void generate() {
        // all your simple drops
        dropSelf(ModBlocks.WOOD_GASIFIER.get());
        dropSelf(ModBlocks.WOOD_OVEN.get());
        dropSelf(ModBlocks.MIXING_BLOCK.get());
        dropSelf(ModBlocks.SCALE_BLOCK.get());
        dropSelf(ModBlocks.STONE_MILL_BLOCK.get());
        dropSelf(ModBlocks.KAOLINITE_CLAY.get());
        dropSelf(ModBlocks.BLACK_TILE.get());
        dropSelf(ModBlocks.BLUE_TILE.get());
        dropSelf(ModBlocks.DARK_BLUE_TILE.get());
        dropSelf(ModBlocks.DARK_BLUE_WHITE_TILE.get());
        dropSelf(ModBlocks.L3E_TILE.get());
        dropSelf(ModBlocks.WHITE_TILE.get());
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

        // your custom wheat crop:
        Block crop = ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get();
        this.add(crop, createCropDrops(
                crop,
                ModItems.HARD_RED_SPRING_WHEAT.get(),
                ModItems.WHEAT_SEED.get(),
                LootItemBlockStatePropertyCondition.hasBlockStateProperties(crop)
                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                .hasProperty(HardRedSpringWheatCrop.AGE, 7)
                        )
        ));


        // wild wheat:
        Block wild = ModBlocks.WILD_WHEAT.get();
        this.add(wild, createWildWheatDrops(wild, ModItems.WHEAT_SEED.get()));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream()
                .map(Holder::value)
                ::iterator;
    }
}
