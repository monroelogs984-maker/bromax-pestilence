package com.bromax.pestilence.entity.afflicted;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Weak alone, comes in flocks and pecks fast. Flutters down from any height. */
public class AfflictedChicken extends AfflictedMob {
    public AfflictedChicken(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(6, 0.3, 2);
    }

    @Override
    protected double chaseSpeed() {
        return 1.4;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        Vec3 motion = getDeltaMovement();
        if (!onGround() && motion.y < 0) setDeltaMovement(motion.multiply(1, 0.6, 1));
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override protected SoundEvent ambient() { return SoundEvents.CHICKEN_AMBIENT; }
    @Override protected SoundEvent hurt() { return SoundEvents.CHICKEN_HURT; }
    @Override protected SoundEvent death() { return SoundEvents.CHICKEN_DEATH; }
    @Override protected SoundEvent step() { return SoundEvents.CHICKEN_STEP; }
}
