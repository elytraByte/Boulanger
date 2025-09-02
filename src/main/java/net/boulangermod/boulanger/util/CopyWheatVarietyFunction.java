// CopyWheatVarietyFunction.java
package net.boulangermod.boulanger.util;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.block.crops.BoulangerWheatCrop;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.item.WheatVariety;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

public class CopyWheatVarietyFunction extends LootItemConditionalFunction {
    public static final MapCodec<CopyWheatVarietyFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            commonFields(instance)               // reads/writes the "conditions" list
                    .apply(instance, CopyWheatVarietyFunction::new)
    );

    private CopyWheatVarietyFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext ctx) {
        BlockState state = ctx.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state != null && state.hasProperty(BoulangerWheatCrop.VARIETY)) {
            WheatVariety var = state.getValue(BoulangerWheatCrop.VARIETY);
            stack.applyComponentsAndValidate(
                    DataComponentPatch.builder()
                            .set(ModDataComponentTypes.WHEAT_VARIETY.get(), var)
                            .build()
            );
        }
        return stack;
    }

    @Override
    public LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
        return MyModLootFunctions.copyVarietyType();
    }

    public static LootItemConditionalFunction.Builder<?> builder() {
        return simpleBuilder(CopyWheatVarietyFunction::new);
    }

}
