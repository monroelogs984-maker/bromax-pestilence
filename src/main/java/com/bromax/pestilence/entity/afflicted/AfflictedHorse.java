package com.bromax.pestilence.entity.afflicted;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Big, tough and hits like a cart: its kick throws you up and back. Steps over fences. */
public class AfflictedHorse extends AfflictedMob {
    public AfflictedHorse(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(30, 0.27, 6).add(Attributes.ATTACK_KNOCKBACK, 1.5).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.STEP_HEIGHT, 1.1);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            target.push(0, 0.35, 0);
            target.hurtMarked = true;
        }
        return hit;
    }

    @Override protected SoundEvent ambient() { return SoundEvents.HORSE_AMBIENT; }
    @Override protected SoundEvent hurt() { return SoundEvents.HORSE_HURT; }
    @Override protected SoundEvent death() { return SoundEvents.HORSE_DEATH; }
    @Override protected SoundEvent step() { return SoundEvents.HORSE_STEP; }
}
