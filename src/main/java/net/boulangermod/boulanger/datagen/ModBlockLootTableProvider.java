package net.boulangermod.boulanger.datagen;

import com.mojang.serialization.ListBuilder;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WheatVarietyRecord;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.item.WheatVariety;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetCustomDataFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.boulangermod.boulanger.block.ModBlocks;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.apache.commons.lang3.builder.Builder;

import java.util.Arrays;
import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
    }

    private WheatVariety currentVariety;


    @Override
    protected LootTable.Builder createCropDrops(Block cropBlock, Item grownCropItem, Item seedsItem, LootItemCondition.Builder dropGrownCropCondition) {
        return applyExplosionDecay(
                cropBlock,
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1f))
                                .add(LootItem.lootTableItem(seedsItem)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                                        .apply(SetComponentsFunction.setComponent(
                                                ModDataComponentTypes.WHEAT_VARIETY.get(),
                                                currentVariety
                                        ))
                                )
                        )
                        .withPool(LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1f))
                                .add(LootItem.lootTableItem(grownCropItem))
                        )
        );
    }

    protected LootTable.Builder createWildWheatDrops(Block cropBlock, Item seedsItem) {
        LootPool.Builder pool = LootPool.lootPool()
                // Exactly one roll
                .setRolls(ConstantValue.exactly(1f))
                // No explosion decay on the pool itself; we’ll wrap later
                ;

        // Add one entry per variety, each weight=1
        for (WheatVariety var : WheatVariety.values()) {
            pool.add(
                    LootItem.lootTableItem(seedsItem)
                            // Always exactly one seed
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1f)))
                            // Tag it with the variety
                            .apply(SetComponentsFunction.setComponent(
                                    ModDataComponentTypes.WHEAT_VARIETY.get(),
                                    var
                            ))
            );
        }

        // Wrap with explosion decay so TNT respects it
        return applyExplosionDecay(
                cropBlock,
                LootTable.lootTable().withPool(pool)
        );
    }



    @Override
    protected void generate() {
        dropSelf(ModBlocks.WOOD_GASIFIER.get());
        dropSelf(ModBlocks.PINE_LOG.get());
        dropSelf(ModBlocks.PINE_WOOD.get());
        dropSelf(ModBlocks.PINE_PLANKS.get());
        dropSelf(ModBlocks.MIXING_BLOCK.get());
        dropSelf(ModBlocks.SCALE_BLOCK.get());
        dropSelf(ModBlocks.WOOD_OVEN.get());
        dropSelf(ModBlocks.BLACK_TILE.get());
        dropSelf(ModBlocks.BLUE_TILE.get());
        dropSelf(ModBlocks.DARK_BLUE_TILE.get());
        dropSelf(ModBlocks.DARK_BLUE_WHITE_TILE.get());
        dropSelf(ModBlocks.WHITE_TILE.get());
        dropSelf(ModBlocks.L3E_TILE.get());
        dropSelf(ModBlocks.PINE_SAPLING.get());
        dropSelf(ModBlocks.STRIPPED_PINE_LOG.get());
        dropSelf(ModBlocks.STRIPPED_PINE_WOOD.get());
        this.add(ModBlocks.PINE_SAPLING.get(), block -> createLeavesDrops(block, ModBlocks.PINE_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));
        dropSelf(ModBlocks.PINE_LEAVES.get());
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

        for (WheatVariety var : WheatVariety.values()) {
            this.currentVariety = var;
            Block crop = ModBlocks.HARD_RED_SPRING_WHEAT_CROP.get();
            this.add(crop, createCropDrops(
                    crop,
                    ModItems.HARD_RED_SPRING_WHEAT.get(),
                    ModItems.WHEAT_SEED.get(),
                    LootItemRandomChanceCondition.randomChance(0.0f)
            ));
        }

        // Wild wheat block:
        Block wild = ModBlocks.WILD_WHEAT.get();
        this.add(wild, createWildWheatDrops(wild, ModItems.WHEAT_SEED.get()));






    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
