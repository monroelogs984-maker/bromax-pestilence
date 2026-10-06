package com.bromax.pestilence.entity.afflicted;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Hunts in packs. When it finds prey it howls (the warning), and every Afflicted within 16 blocks
 * gets Speed and joins the hunt. The howl has a 20 second cooldown.
 */
public class AfflictedWolf extends AfflictedMob {
    private int howlCooldown;

    public AfflictedWolf(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(18, 0.32, 4);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        boolean newPrey = target != null && getTarget() == null;
        super.setTarget(target);
        if (newPrey && howlCooldown <= 0 && !level().isClientSide) howl(target);
    }

    private void howl(LivingEntity prey) {
        howlCooldown = 400;
        playSound(SoundEvents.WOLF_HOWL, 2f, 0.7f);
        for (AfflictedMob pack : level().getEntitiesOfClass(AfflictedMob.class, getBoundingBox().inflate(16))) {
            pack.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
            if (pack.getTarget() == null && pack != this) pack.setTarget(prey);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (howlCooldown > 0) howlCooldown--;
    }

    @Override protected SoundEvent ambient() { return SoundEvents.WOLF_GROWL; }
    @Override protected SoundEvent hurt() { return SoundEvents.WOLF_HURT; }
    @Override protected SoundEvent death() { return SoundEvents.WOLF_DEATH; }
    @Override protected SoundEvent step() { return SoundEvents.WOLF_STEP; }
}
