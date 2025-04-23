package net.boulangermod.boulanger.entity;

import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class HefferEntity extends Cow {
    public HefferEntity(EntityType<? extends Cow> type, Level level) {
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
    public HefferEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.HEFFER.get().create(level);
    }
}
