package com.bromax.pestilence.entity.form;

import com.bromax.pestilence.entity.MiasmaCloud;
import com.bromax.pestilence.disease.Disease;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Pneumonic Form: a hooded column of foul air. Draws a long, loud breath (a full second, with the
 * air visibly pulled in) before it breathes a line of miasma clouds toward its target.
 */
public class ChokingShade extends FormMob {
    private static final int INHALE = 20;
    private int cooldown = 80;
    private int inhale;

    public ChokingShade(EntityType<? extends FormMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(34, 0.23, 4, 0);
    }

    @Override
    protected Disease disease() {
        return Disease.PNEUMONIC;
    }

    @Override
    protected void formTick(ServerLevel level) {
        if (busy()) return;
        LivingEntity target = getTarget();
        if (inhale > 0) {
            getNavigation().stop();
            if (target != null) getLookControl().setLookAt(target);
            Vec3 look = getLookAngle();
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX() + look.x * 3, getEyeY() + look.y * 3, getZ() + look.z * 3,
                    2, 0.6, 0.4, 0.6, -0.03);
            if (--inhale == 0) breathe(level);
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (target != null && target.isAlive() && distanceToSqr(target) < 10 * 10 && hasLineOfSight(target)) {
            inhale = INHALE;
            level.playSound(null, blockPosition(), SoundEvents.WARDEN_SNIFF, SoundSource.HOSTILE, 1.6f, 0.5f);
        }
    }

    private void breathe(ServerLevel level) {
        cooldown = 120 - 20 * tier();
        Vec3 look = getLookAngle();
        float radius = 1.2f + 0.4f * tier();
        int clouds = 2 + tier();
        for (int i = 1; i <= clouds; i++) {
            double d = i * 1.8;
            MiasmaCloud.spawn(level, getX() + look.x * d, getEyeY() - 0.8 + look.y * d, getZ() + look.z * d, radius, 120);
        }
        level.playSound(null, blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.6f, 0.5f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BreathCooldown", cooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        cooldown = tag.getInt("BreathCooldown");
    }

    @Override protected SoundEvent ambient() { return SoundEvents.VEX_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.VEX_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.VEX_DEATH; }
}
