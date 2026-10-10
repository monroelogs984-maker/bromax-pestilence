package com.bromax.pestilence.entity.form;

import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.disease.Disease;
import com.bromax.pestilence.world.Blight;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * Septicemic Form: a lean, soot-dark shape, quick on its feet. Its blows sap strength (and, once
 * Pestilent, slow), and the ground it crosses blights behind it (the same reversible Blight as nests).
 */
public class Blackened extends FormMob {
    public Blackened(EntityType<? extends FormMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(30, 0.32, 5, 1);
    }

    @Override
    protected Disease disease() {
        return Disease.SEPTICEMIC;
    }

    @Override
    protected void onHit(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100 + 40 * tier(), tier() >= GREATER ? 1 : 0));
        if (tier() >= PESTILENT) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
    }

    @Override
    protected void formTick(ServerLevel level) {
        if (PestConfig.BLIGHT_ENABLED.get() && tickCount % 20 == 0 && walkDist != walkDistO) {
            Blight.spread(level, blockPosition(), 1, tier());
        }
    }

    @Override protected SoundEvent ambient() { return SoundEvents.HUSK_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.HUSK_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.HUSK_DEATH; }
}
