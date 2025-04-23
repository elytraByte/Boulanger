package net.boulangermod.boulanger.entity;

import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;   // <<-- CORRECT package!

public class HenEntity extends Chicken {
    public HenEntity(EntityType<? extends Chicken> type, Level level) {
        super(type, level);
    }

    /**
     * Called from your common‐bus EntityAttributeCreationEvent subscriber.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Chicken.createAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    // Intercept the single-arg ItemLike egg drop:
    @Override
    public ItemEntity spawnAtLocation(ItemLike itemIn) {
        if (itemIn.asItem() == Items.EGG) {
            return super.spawnAtLocation(ModItems.FANCY_EGG.get());
        }
        return super.spawnAtLocation(itemIn);
    }

    // Intercept the two-arg ItemLike egg drop (egg + count):
    @Override
    public ItemEntity spawnAtLocation(ItemLike itemIn, int count) {
        if (itemIn.asItem() == Items.EGG) {
            return super.spawnAtLocation(ModItems.FANCY_EGG.get(), count);
        }
        return super.spawnAtLocation(itemIn, count);
    }

    // Intercept the ItemStack-based drop:
    @Override
    public ItemEntity spawnAtLocation(ItemStack stack) {
        if (stack.getItem() == Items.EGG) {
            return super.spawnAtLocation(new ItemStack(ModItems.FANCY_EGG.get()));
        }
        return super.spawnAtLocation(stack);
    }
}
