package net.boulangermod.boulanger.entity;

import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class HolsteinFriesianCowEntity extends Cow {
    public HolsteinFriesianCowEntity(EntityType<? extends Cow> type, Level level) {
        super(type, level);
    }

    /** Copy vanilla cow attributes (health/speed/etc) or tweak here if desired */
    public static AttributeSupplier.Builder createAttributes() {
        return Cow.createAttributes()
                // e.g. tweak values:
                // .add(Attributes.MAX_HEALTH, 12.0D)
                // .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.spawnAtLocation(ModItems.WHOLE_MILK.get(), 1);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.COW_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.COW_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COW_DEATH;
    }

    /** Breed baby heffers */
    @Nullable
    @Override
    public HolsteinFriesianCowEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.HOLSTEIN_FRIESAIN_COW.get().create(level);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        // If the player is holding your wooden bucket and this is an adult Heffer…
        if (held.is(ModItems.WOODEN_BUCKET.get()) && !this.isBaby()) {
            // Play the milking sound
            this.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);

            // Consume the empty wooden bucket (unless in creative)
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }

            // Give them a wooden bucket of milk
            ItemStack milk = new ItemStack(ModItems.WOODEN_BUCKET_OF_WHOLE_MILK.get());
            // Try to put it in their inventory, or drop on ground if full
            if (!player.getInventory().add(milk)) {
                player.drop(milk, false);
            }

            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Otherwise, fall back to default behavior (breeding, etc.)
        return super.mobInteract(player, hand);
    }
}
