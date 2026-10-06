package com.bromax.pestilence.effect;

import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.Plague;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;

/**
 * Sickness: the one visible effect for every disease. Amplifier is the stage (0 Incubation, 1, 2).
 * Each stage costs one heart of max health; the disease itself (see {@link com.bromax.pestilence.disease.Disease})
 * decides the symptoms. Stage changes, recovery and animal deaths are driven by PlagueEvents.
 */
public class PlagueEffect extends MobEffect {
    public PlagueEffect() {
        super(MobEffectCategory.HARMFUL, 0x4b5320);
        addAttributeModifier(Attributes.MAX_HEALTH, Pestilence.id("plague_health"), -2.0, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int stage) {
        if (entity.level() instanceof ServerLevel level) Plague.disease(entity).symptoms(level, entity, stage, Plague.isPerson(entity));
        return true;
    }

    /** A wet cough: a low panda sneeze and a puff of sneeze particles in front of the face. */
    public static void cough(ServerLevel level, LivingEntity entity) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PANDA_SNEEZE,
                entity.getSoundSource(), 0.6f, 0.5f + entity.getRandom().nextFloat() * 0.15f);
        Vec3 look = entity.getLookAngle();
        level.sendParticles(ParticleTypes.SNEEZE, entity.getX() + look.x * 0.5, entity.getEyeY() - 0.15,
                entity.getZ() + look.z * 0.5, 6, 0.1, 0.05, 0.1, 0.02);
    }

    /** Milk does nothing for the plague. */
    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance instance) {}
}
