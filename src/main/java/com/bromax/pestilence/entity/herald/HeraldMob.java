package com.bromax.pestilence.entity.herald;

import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.phase.Phase;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The Pestilence's human servants. Hostile, immune to the plague, and each one does a job for it.
 * They appear from the Heralds phase, at night and around mature nests.
 */
public abstract class HeraldMob extends AfflictedMob {
    protected HeraldMob(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    @Override
    public int minPhase() {
        return Phase.HERALDS;
    }

    @Override
    protected boolean hasFlies() {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 0.8f + random.nextFloat() * 0.1f;
    }

    @Override protected SoundEvent ambient() { return SoundEvents.PILLAGER_AMBIENT; }
    @Override protected SoundEvent hurt() { return SoundEvents.PILLAGER_HURT; }
    @Override protected SoundEvent death() { return SoundEvents.PILLAGER_DEATH; }
    @Override protected SoundEvent step() { return SoundEvents.ZOMBIE_STEP; }
}
