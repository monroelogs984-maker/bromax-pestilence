package com.bromax.pestilence.entity.afflicted;

import com.bromax.pestilence.entity.MiasmaCloud;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Slow, heavy and bloated: when it dies it bursts into a miasma cloud. Don't kill it in a crowd. */
public class AfflictedCow extends AfflictedMob {
    public AfflictedCow(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(24, 0.2, 4).add(Attributes.KNOCKBACK_RESISTANCE, 0.5).add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) MiasmaCloud.spawn(level, getX(), getY(), getZ(), 3f, 200);
    }

    @Override protected SoundEvent ambient() { return SoundEvents.COW_AMBIENT; }
    @Override protected SoundEvent hurt() { return SoundEvents.COW_HURT; }
    @Override protected SoundEvent death() { return SoundEvents.COW_DEATH; }
    @Override protected SoundEvent step() { return SoundEvents.COW_STEP; }
}
