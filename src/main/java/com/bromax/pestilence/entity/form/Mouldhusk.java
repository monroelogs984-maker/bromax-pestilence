package com.bromax.pestilence.entity.form;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.disease.Disease;
import com.bromax.pestilence.phase.Phase;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Form of the Rot: a slow fungal husk. It swells for a second and a half, then bursts spores that
 * can give the Rot to anything living nearby. Only condenses from the Danse Macabre on.
 */
public class Mouldhusk extends FormMob {
    private static final int SWELL = 30;
    private int cooldown = 100;
    private int swell;

    public Mouldhusk(EntityType<? extends FormMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(55, 0.18, 7, 4).add(Attributes.KNOCKBACK_RESISTANCE, 0.7);
    }

    @Override
    protected Disease disease() {
        return Disease.ROT;
    }

    @Override
    public int minPhase() {
        return Phase.DANSE_MACABRE;
    }

    public boolean isSwelling() {
        return swell > 0;
    }

    @Override
    protected void formTick(ServerLevel level) {
        if (busy()) return;
        if (swell > 0) {
            getNavigation().stop();
            if (swell % 5 == 0) {
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + getBbHeight() * 0.7, getZ(), 6, 0.5, 0.4, 0.5, 0.0);
            }
            if (--swell == 0) burst(level);
            return;
        }
        if (--cooldown > 0) return;
        LivingEntity target = getTarget();
        if (target != null && distanceToSqr(target) < 8 * 8) {
            swell = SWELL;
            level.playSound(null, blockPosition(), SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.HOSTILE, 1.5f, 0.5f);
        }
    }

    private void burst(ServerLevel level) {
        cooldown = 160 - 20 * tier();
        double radius = 3.5 + tier();
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1, getZ(), 80, radius * 0.5, 1.0, radius * 0.5, 0.0);
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 1, getZ(), 40, radius * 0.5, 0.8, radius * 0.5, 0.0);
        level.playSound(null, blockPosition(), SoundEvents.PUFFER_FISH_BLOW_OUT, SoundSource.HOSTILE, 2f, 0.4f);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
                e -> e != this && !(e instanceof FormMob) && Plague.canCatch(e))) {
            Plague.tryContactInfect(e, 0.2 + 0.1 * tier(), Disease.ROT);
            if (e instanceof Player) e.addEffect(new MobEffectInstance(ModRegistry.ROTTING, 200, 0));
        }
    }

    @Override protected SoundEvent ambient() { return SoundEvents.SNIFFER_IDLE; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SNIFFER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SNIFFER_DEATH; }
}
