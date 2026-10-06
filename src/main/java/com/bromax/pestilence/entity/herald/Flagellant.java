package com.bromax.pestilence.entity.herald;

import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** Whips itself bloody in a fight; each lash gives every Herald and Afflicted within 8 blocks Strength and Speed. Kill it first. */
public class Flagellant extends HeraldMob {
    public Flagellant(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(18, 0.3, 3);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (getTarget() == null || tickCount % 100 != 0 || !(level() instanceof ServerLevel level)) return;
        swing(net.minecraft.world.InteractionHand.OFF_HAND);
        if (getHealth() > 4) hurt(damageSources().generic(), 1f);
        level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1f, 1.5f);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY(0.7), getZ(), 3, 0.2, 0.2, 0.2, 0.1);
        for (AfflictedMob ally : level.getEntitiesOfClass(AfflictedMob.class, getBoundingBox().inflate(8))) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0));
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, ally.getX(), ally.getEyeY() + 0.4, ally.getZ(), 1, 0, 0, 0, 0);
        }
    }
}
