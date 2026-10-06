package com.bromax.pestilence.entity.boss;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.disease.Disease;
import com.bromax.pestilence.entity.PlagueRat;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.phase.Phase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Rats knotted together by their tails, crowned in bone. It squeals up plague rats, rams its prey,
 * and at two-thirds and one-third health tears loose a swarm and a pack of sewer rats. Fire
 * hurts it half again as much.
 */
public class RatKing extends PlagueBoss {
    private int splits;

    public RatKing(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(120, 0.3, 5).add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.ARMOR, 4).add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected Disease contactDisease() {
        return Disease.BUBONIC;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 1.5f : amount);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) return;
        float hp = getHealth() / getMaxHealth();
        if (splits == 0 && hp < 0.66f || splits == 1 && hp < 0.33f) {
            splits++;
            playSound(SoundEvents.SILVERFISH_DEATH, 2f, 0.5f);
            spawn(level, ModRegistry.RAT_SWARM.get(), 1);
            spawn(level, ModRegistry.SEWER_RAT.get(), 3);
        }
        if (getTarget() == null) return;
        if (tickCount % 200 == 0 && level.getEntitiesOfClass(PlagueRat.class, new AABB(blockPosition()).inflate(16)).size() < 6) {
            playSound(SoundEvents.SILVERFISH_AMBIENT, 2f, 0.6f);
            spawn(level, ModRegistry.PLAGUE_RAT.get(), 2);
        }
        if (tickCount % 160 == 0 && onGround() && distanceToSqr(getTarget()) > 9) {
            Vec3 ram = getTarget().position().subtract(position()).normalize().scale(1.1);
            setDeltaMovement(ram.x, 0.3, ram.z);
            playSound(SoundEvents.RAVAGER_ROAR, 0.8f, 1.8f);
        }
    }

    private void spawn(ServerLevel level, EntityType<? extends Mob> type, int count) {
        for (int i = 0; i < count; i++) {
            Mob mob = type.create(level);
            if (mob == null) continue;
            mob.moveTo(getX() + random.nextGaussian(), getY(), getZ() + random.nextGaussian(), random.nextFloat() * 360f, 0f);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            mob.setTarget(getTarget());
            level.addFreshEntity(mob);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("splits", splits);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        splits = tag.getInt("splits");
    }

    @Override protected SoundEvent ambient() { return SoundEvents.SILVERFISH_AMBIENT; }
    @Override protected SoundEvent hurt() { return SoundEvents.SILVERFISH_HURT; }
    @Override protected SoundEvent death() { return SoundEvents.SILVERFISH_DEATH; }
    @Override protected SoundEvent step() { return SoundEvents.SILVERFISH_STEP; }

    @Override
    public float getVoicePitch() {
        return 0.5f;
    }

    /** Spawned once by a Rat Warren reaching stage IV (from Rat Season). */
    public static int minimumPhase() {
        return Phase.RAT_SEASON;
    }
}
