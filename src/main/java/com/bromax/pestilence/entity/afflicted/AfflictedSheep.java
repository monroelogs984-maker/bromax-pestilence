package com.bromax.pestilence.entity.afflicted;

import com.bromax.pestilence.entity.MiasmaCloud;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** Its rotten fleece sheds a puff of miasma when struck (at most every 3 seconds). Use reach, or a mask. */
public class AfflictedSheep extends AfflictedMob {
    private int puffCooldown;

    public AfflictedSheep(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(16, 0.23, 3);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && puffCooldown <= 0 && isAlive() && level() instanceof ServerLevel level) {
            MiasmaCloud.spawn(level, getX(), getY(), getZ(), 1.5f, 60);
            puffCooldown = 60;
        }
        return hurt;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (puffCooldown > 0) puffCooldown--;
    }

    @Override protected SoundEvent ambient() { return SoundEvents.SHEEP_AMBIENT; }
    @Override protected SoundEvent hurt() { return SoundEvents.SHEEP_HURT; }
    @Override protected SoundEvent death() { return SoundEvents.SHEEP_DEATH; }
    @Override protected SoundEvent step() { return SoundEvents.SHEEP_STEP; }
}
