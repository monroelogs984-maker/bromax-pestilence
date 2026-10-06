package com.bromax.pestilence.entity.boss;

import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.entity.Corpse;
import com.bromax.pestilence.entity.MiasmaCloud;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.world.Tracker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Master of the Lazaretto. A towering surgeon in an iron beak, with a cleaver the size of a door.
 * Its blows cleave everyone around its target; it leaps on those who keep away; every twenty
 * seconds it calls the dead in the crypt to rise at once (or brings two more). Below half health
 * it breathes rings of miasma. Inside a lit Herb Brazier's circle it takes double damage.
 */
public class Gravewarden extends PlagueBoss {
    private static final List<EntityType<?>> DEAD = List.of(EntityType.VILLAGER, EntityType.VILLAGER, EntityType.COW, EntityType.PIG);

    public Gravewarden(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level, BossEvent.BossBarColor.WHITE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(300, 0.25, 10).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 6)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.STEP_HEIGHT, 1.1);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            for (LivingEntity other : level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3),
                    e -> e != target && e != this && !(e instanceof AfflictedMob))) {
                other.hurt(damageSources().mobAttack(this), 6f);
            }
            playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, Tracker.inBrazier(level(), blockPosition()) ? amount * 2 : amount);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level) || getTarget() == null) return;
        LivingEntity target = getTarget();
        if (tickCount % 400 == 0) raiseTheDead(level);
        if (tickCount % 300 == 150 && onGround() && distanceToSqr(target) > 36) {
            Vec3 leap = target.position().subtract(position());
            setDeltaMovement(leap.x * 0.18, 0.9, leap.z * 0.18);
            playSound(SoundEvents.RAVAGER_ROAR, 1.5f, 0.7f);
        }
        if (fallDistance == 0 && tickCount % 300 == 175) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4), e -> e != this && !(e instanceof AfflictedMob))) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                e.push((e.getX() - getX()) * 0.3, 0.4, (e.getZ() - getZ()) * 0.3);
                e.hurtMarked = true;
            }
            level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 3, 1, 0.2, 1, 0);
        }
        if (getHealth() < getMaxHealth() / 2 && tickCount % 200 == 0) {
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2 + tickCount * 0.01;
                MiasmaCloud.spawn(level, getX() + Math.cos(a) * 5, getY(), getZ() + Math.sin(a) * 5, 2.5f, 160);
            }
        }
    }

    private void raiseTheDead(ServerLevel level) {
        List<Corpse> corpses = level.getEntitiesOfClass(Corpse.class, new AABB(blockPosition()).inflate(24), c -> !c.isBurning());
        playSound(SoundEvents.ZOMBIE_VILLAGER_CURE, 2f, 0.5f);
        if (corpses.isEmpty()) {
            for (int i = 0; i < 2; i++) {
                Corpse.place(level, DEAD.get(random.nextInt(DEAD.size())), PestConfig.CORPSE_RISE_TICKS.get() - 100,
                        getX() + random.nextGaussian() * 4, getY(), getZ() + random.nextGaussian() * 4, random.nextFloat() * 360f);
            }
        } else {
            corpses.forEach(c -> c.hasten(PestConfig.CORPSE_RISE_TICKS.get()));
        }
    }

    @Override protected SoundEvent ambient() { return SoundEvents.WARDEN_AMBIENT; }
    @Override protected SoundEvent hurt() { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected SoundEvent death() { return SoundEvents.WARDEN_DEATH; }
    @Override protected SoundEvent step() { return SoundEvents.IRON_GOLEM_STEP; }
}
