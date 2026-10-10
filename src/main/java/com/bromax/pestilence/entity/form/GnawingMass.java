package com.bromax.pestilence.entity.form;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.disease.Disease;
import com.bromax.pestilence.entity.PlagueRat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * Bubonic Form: rats and fleas holding one shape. Every quarter of its health it loses, part of it
 * breaks away as loose rats (more of them, and worse, as it grows).
 */
public class GnawingMass extends FormMob {
    private int shed;

    public GnawingMass(EntityType<? extends FormMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(40, 0.26, 6, 2);
    }

    @Override
    protected Disease disease() {
        return Disease.BUBONIC;
    }

    @Override
    protected void onHurt(ServerLevel level, DamageSource source) {
        int quarters = (int) ((1 - getHealth() / getMaxHealth()) * 4);
        while (shed < Math.min(3, quarters)) {
            shed++;
            for (int i = 0; i < 1 + tier(); i++) {
                EntityType<? extends PlagueRat> type = tier() >= GREATER && random.nextBoolean() ? ModRegistry.SEWER_RAT.get() : ModRegistry.PLAGUE_RAT.get();
                PlagueRat rat = type.create(level);
                if (rat == null) continue;
                rat.moveTo(getRandomX(0.6), getY() + 0.3, getRandomZ(0.6), random.nextFloat() * 360, 0);
                rat.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                if (getTarget() != null) rat.setTarget(getTarget());
                level.addFreshEntity(rat);
            }
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() * 0.5, getZ(), 15, 0.5, 0.3, 0.5, 0.05);
            level.playSound(null, blockPosition(), SoundEvents.SILVERFISH_HURT, SoundSource.HOSTILE, 1.5f, 0.6f);
        }
    }

    @Override
    protected void formTick(ServerLevel level) {}

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Shed", shed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        shed = tag.getInt("Shed");
    }

    @Override protected SoundEvent ambient() { return SoundEvents.SILVERFISH_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SILVERFISH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SILVERFISH_DEATH; }
}
