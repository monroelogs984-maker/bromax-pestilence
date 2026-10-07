package com.bromax.pestilence.effect;

import com.bromax.pestilence.Pestilence;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;

/**
 * The Blackblight. Amplifier is the stage minus one (stage 1 to 9); reaching stage 10 kills (see
 * disease/Blackblight). Each stage takes half a heart, and the body fails piece by piece: dark
 * spells from stage 3, weakness from 5, slowness and withering from 7.
 */
public class BlackblightEffect extends MobEffect {
    public BlackblightEffect() {
        super(MobEffectCategory.HARMFUL, 0x14100f);
        addAttributeModifier(Attributes.MAX_HEALTH, Pestilence.id("blackblight_health"), -1.0, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) return true;
        int stage = amplifier + 1;
        level.sendParticles(ParticleTypes.SQUID_INK, entity.getX(), entity.getY(0.6), entity.getZ(), stage, 0.25, 0.4, 0.25, 0.01);
        if (stage >= 3 && entity.getRandom().nextInt(12 - stage) == 0) entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));
        if (stage >= 5) entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, stage >= 8 ? 1 : 0, false, false));
        if (stage >= 7) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, stage - 7, false, false));
            if (entity.getRandom().nextInt(3) == 0) entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, false, false));
        }
        return true;
    }

    /** Nothing cures it but Theriac (which only slows it) and the Black Bloom's death. */
    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance instance) {}
}
