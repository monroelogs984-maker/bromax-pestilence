package com.bromax.pestilence.entity.form;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.block.NestBlock;
import com.bromax.pestilence.disease.Disease;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.item.CenserItem;
import com.bromax.pestilence.phase.Phase;
import com.bromax.pestilence.phase.PestilenceData;
import com.bromax.pestilence.world.Coalescence;
import com.bromax.pestilence.world.Tracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * Plague fog that has gathered into a knot: what the Afflicted and the unburned dead melt into.
 * Slow and weak, it drifts toward other clots and Afflicted and takes them in, growing with each.
 * At enough mass it churns for five seconds (loudly) and condenses into a Lesser Form, whose kind
 * depends on the nearest nest. Fire hurts it double; brazier or censer smoke tears it apart.
 */
public class MiasmaClot extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> MASS = SynchedEntityData.defineId(MiasmaClot.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHURN = SynchedEntityData.defineId(MiasmaClot.class, EntityDataSerializers.INT);
    private static final int CHURN_TICKS = 100;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MiasmaClot(EntityType<? extends MiasmaClot> type, Level level) {
        super(type, level);
        this.xpReward = 3;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60)
                .add(Attributes.MOVEMENT_SPEED, 0.13)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    /** A new clot of the given mass at a position. */
    public static MiasmaClot create(ServerLevel level, double x, double y, double z, int mass) {
        MiasmaClot clot = ModRegistry.MIASMA_CLOT.get().create(level);
        if (clot == null) return null;
        clot.moveTo(x, y, z, level.random.nextFloat() * 360, 0);
        clot.finalizeSpawn(level, level.getCurrentDifficultyAt(clot.blockPosition()), MobSpawnType.CONVERSION, null);
        clot.setMass(mass);
        clot.setHealth(clot.healthFor(mass));
        level.addFreshEntity(clot);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.5, z, 30, 0.6, 0.4, 0.6, 0.02);
        level.playSound(null, clot.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 2f, 0.5f);
        Plague.addPoints(level, PestConfig.POINTS_COALESCE.get());
        return clot;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MASS, 1);
        builder.define(CHURN, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
    }

    public int mass() {
        return entityData.get(MASS);
    }

    public void setMass(int mass) {
        entityData.set(MASS, Math.max(1, mass));
        refreshDimensions();
    }

    private float healthFor(int mass) {
        return Math.min(getMaxHealth(), 4 + 5f * mass);
    }

    /** Render and hitbox scale. */
    public float sizeScale() {
        return Math.min(2.2f, 0.7f + 0.15f * mass());
    }

    public boolean isChurning() {
        return entityData.get(CHURN) > 0;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(sizeScale());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (MASS.equals(key)) refreshDimensions();
    }

    /** Takes another clot or Afflicted into itself. */
    public void absorb(ServerLevel level, LivingEntity other, int mass) {
        setMass(mass() + mass);
        heal(other.getHealth() * 0.5f);
        level.sendParticles(ParticleTypes.SQUID_INK, other.getX(), other.getY() + other.getBbHeight() * 0.5, other.getZ(), 20, 0.4, 0.4, 0.4, 0.02);
        level.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.5f, 0.6f);
        Plague.addPoints(level, PestConfig.POINTS_COALESCE.get());
        other.discard();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) return;
        int churn = entityData.get(CHURN);
        if (churn > 0) {
            getNavigation().stop();
            if (churn % 10 == 0) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + getBbHeight() * 0.5, getZ(), 10, getBbWidth() * 0.4, getBbHeight() * 0.3, getBbWidth() * 0.4, 0.04);
            }
            if (churn % 40 == 0) level.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2f, 0.5f);
            entityData.set(CHURN, churn - 1);
            if (churn == 1) condense(level);
            return;
        }
        if (tickCount % 20 != 0) return;

        // Brazier and censer smoke tear it apart.
        if (Tracker.inBrazier(level, blockPosition()) || level.players().stream()
                .anyMatch(p -> p.distanceToSqr(this) <= CenserItem.RADIUS * CenserItem.RADIUS && CenserItem.isSwinging(p))) {
            hurt(damageSources().magic(), 4f);
        }
        // Foul air: those close by breathe it.
        double reach = getBbWidth() * 0.5 + 1.5;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(reach), Plague::canCatch)) {
            if (random.nextDouble() < 0.08 * Plague.airExposure(e)) Plague.infect(e, Disease.PNEUMONIC);
        }
        // Take in what it touches; otherwise drift toward the nearest thing it could take in.
        List<MiasmaClot> clots = level.getEntitiesOfClass(MiasmaClot.class, getBoundingBox().inflate(16),
                c -> c != this && c.isAlive() && !c.isChurning());
        for (MiasmaClot other : clots) {
            if (other.distanceTo(this) < (getBbWidth() + other.getBbWidth()) * 0.5 + 1.0 && mass() >= other.mass()) {
                absorb(level, other, other.mass());
                break;
            }
        }
        for (AfflictedMob mob : level.getEntitiesOfClass(AfflictedMob.class, getBoundingBox().inflate(1.2), Coalescence::canJoin)) {
            absorb(level, mob, 1);
        }
        if (mass() >= PestConfig.CLOT_MASS_FOR_FORM.get()) {
            entityData.set(CHURN, CHURN_TICKS);
            FormMob.announce(level, blockPosition(), Component.translatable("message.bromaxpestilence.clot_condensing").withStyle(ChatFormatting.DARK_RED));
            return;
        }
        LivingEntity goal = clots.isEmpty() ? null : clots.get(0);
        if (goal == null) {
            goal = level.getNearestEntity(AfflictedMob.class, net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat().selector(Coalescence::canJoin),
                    this, getX(), getY(), getZ(), getBoundingBox().inflate(16));
        }
        if (goal == null) return;
        // Pathfinding counts "next to it" as arrived and collision keeps them apart, so close in
        // directly over the last few blocks.
        if (distanceToSqr(goal) < 4 * 4) {
            getNavigation().stop();
            getMoveControl().setWantedPosition(goal.getX(), goal.getY(), goal.getZ(), 1.0);
        } else {
            getNavigation().moveTo(goal, 1.0);
        }
    }

    /** The clot becomes a Lesser Form. The nearest nest decides which; the Rot takes some from the Danse Macabre. */
    private void condense(ServerLevel level) {
        EntityType<? extends FormMob> type = lineFor(level, blockPosition());
        FormMob form = type.create(level);
        if (form == null) return;
        form.moveTo(getX(), getY(), getZ(), getYRot(), 0);
        form.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.CONVERSION, null);
        level.addFreshEntity(form);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 60, 1, 0.8, 1, 0.05);
        level.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.2f, 0.6f);
        FormMob.announce(level, blockPosition(), Component.translatable("message.bromaxpestilence.form_born", form.getDisplayName()).withStyle(ChatFormatting.DARK_RED));
        Plague.addPoints(level, PestConfig.POINTS_FORM_BORN.get());
        discard();
    }

    public static EntityType<? extends FormMob> lineFor(ServerLevel level, BlockPos pos) {
        if (PestilenceData.phase(level) >= Phase.DANSE_MACABRE && level.random.nextFloat() < 0.4f) return ModRegistry.MOULDHUSK.get();
        var nest = Tracker.nearestNest(level, pos, 32);
        if (nest.isPresent() && level.getBlockState(nest.get()).getBlock() instanceof NestBlock block) {
            return switch (block.kind()) {
                case WARREN -> ModRegistry.GNAWING_MASS.get();
                case WELL -> ModRegistry.CHOKING_SHADE.get();
                case PIT -> ModRegistry.BLACKENED.get();
            };
        }
        return switch (level.random.nextInt(3)) {
            case 0 -> ModRegistry.GNAWING_MASS.get();
            case 1 -> ModRegistry.CHOKING_SHADE.get();
            default -> ModRegistry.BLACKENED.get();
        };
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) amount *= 2;
        return super.hurt(source, amount);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(ModRegistry.PLAGUE) && super.canBeAffected(effect);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getRandomX(0.6), getY() + getBbHeight() * random.nextDouble(), getRandomZ(0.6), 0, 0.01, 0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Mass", mass());
        tag.putInt("Churn", entityData.get(CHURN));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setMass(tag.getInt("Mass"));
        entityData.set(CHURN, tag.getInt("Churn"));
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SOUL_ESCAPE.value(); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.WOOL_BREAK; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.FIRE_EXTINGUISH; }

    @Override
    public float getVoicePitch() {
        return 0.5f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController(this));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
