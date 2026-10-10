package com.bromax.pestilence.entity.form;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.block.NestBlockEntity;
import com.bromax.pestilence.disease.Disease;
import com.bromax.pestilence.entity.PhaseGated;
import com.bromax.pestilence.item.CenserItem;
import com.bromax.pestilence.phase.Phase;
import com.bromax.pestilence.phase.PestilenceData;
import com.bromax.pestilence.world.Tracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumMap;
import java.util.Map;

/**
 * A Form: the plague concentrated. Born when a Miasma Clot has taken in enough of the dead, never
 * spawned naturally. Grows from Lesser to Greater to Pestilent by killing (each growth gated by
 * phase and announced), hardens against whatever keeps hurting it (fire and smoke strip that), and
 * calls on the nearest nest when it is badly hurt or killed.
 */
public abstract class FormMob extends Monster implements GeoEntity, PhaseGated {
    public static final int LESSER = 1, GREATER = 2, PESTILENT = 3;
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(FormMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHURN = SynchedEntityData.defineId(FormMob.class, EntityDataSerializers.INT);
    private static final ResourceLocation TIER_HEALTH = ResourceLocation.fromNamespaceAndPath("bromaxpestilence", "form_tier_health");
    private static final ResourceLocation TIER_DAMAGE = ResourceLocation.fromNamespaceAndPath("bromaxpestilence", "form_tier_damage");
    private static final int CHURN_TICKS = 60;

    /** What it has learned to shrug off. Fire never counts. */
    public enum Harm { MELEE, PROJECTILE, MAGIC, EXPLOSION }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<Harm, Integer> strain = new EnumMap<>(Harm.class);
    private ServerBossEvent bar;
    private int kills;
    private boolean calledForHelp;

    protected FormMob(EntityType<? extends FormMob> type, Level level) {
        super(type, level);
        this.xpReward = 20;
        setPersistenceRequired();
    }

    protected static AttributeSupplier.Builder base(double health, double speed, double damage, double armour) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.ARMOR, armour)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TIER, LESSER);
        builder.define(CHURN, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
    }

    // ---- tiers -------------------------------------------------------------------------------

    public int tier() {
        return entityData.get(TIER);
    }

    /** Sets the tier and its stat scaling; health keeps its fraction. */
    public void setTier(int tier) {
        tier = Math.max(LESSER, Math.min(PESTILENT, tier));
        float fraction = getHealth() / Math.max(1f, getMaxHealth());
        entityData.set(TIER, tier);
        double hp = tier == LESSER ? 0 : tier == GREATER ? 0.6 : 1.4;
        double dmg = tier == LESSER ? 0 : tier == GREATER ? 0.3 : 0.7;
        var health = getAttribute(Attributes.MAX_HEALTH);
        var damage = getAttribute(Attributes.ATTACK_DAMAGE);
        if (health != null) {
            health.removeModifier(TIER_HEALTH);
            if (hp > 0) health.addPermanentModifier(new AttributeModifier(TIER_HEALTH, hp, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if (damage != null) {
            damage.removeModifier(TIER_DAMAGE);
            if (dmg > 0) damage.addPermanentModifier(new AttributeModifier(TIER_DAMAGE, dmg, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        setHealth(getMaxHealth() * fraction);
        refreshDimensions();
        updateBar();
    }

    public float tierScale() {
        return switch (tier()) {
            case GREATER -> 1.2f;
            case PESTILENT -> 1.4f;
            default -> 1f;
        };
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(tierScale());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (TIER.equals(key)) refreshDimensions();
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.bromaxpestilence.form_tier." + tier(), super.getTypeName());
    }

    public boolean isChurning() {
        return entityData.get(CHURN) > 0;
    }

    /** The phase each growth needs. */
    private static int phaseFor(int nextTier) {
        return nextTier == GREATER ? Phase.BLACK_DEATH : Phase.DANSE_MACABRE;
    }

    /** Called when it kills something; may begin to grow. */
    public void noteKill() {
        kills++;
        if (tier() >= PESTILENT || isChurning() || !(level() instanceof ServerLevel level)) return;
        int need = tier() == LESSER ? PestConfig.GREATER_KILLS.get() : PestConfig.PESTILENT_KILLS.get();
        if (kills >= need && PestilenceData.phase(level) >= phaseFor(tier() + 1)) {
            entityData.set(CHURN, CHURN_TICKS);
            level.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2f, 0.6f);
        }
    }

    private void finishGrowing(ServerLevel level) {
        Component before = getDisplayName();
        setTier(tier() + 1);
        kills = 0;
        heal(getMaxHealth() * 0.25f);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + getBbHeight() * 0.6, getZ(), 40, getBbWidth() * 0.6, getBbHeight() * 0.4, getBbWidth() * 0.6, 0.03);
        level.playSound(null, blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 2.5f, 0.5f);
        announce(level, Component.translatable("message.bromaxpestilence.form_grows", before, getDisplayName()).withStyle(ChatFormatting.DARK_RED));
        Plague.addPoints(this, PestConfig.POINTS_FORM_GROWS.get());
    }

    public static void announce(ServerLevel level, BlockPos pos, Component message) {
        level.players().stream().filter(p -> p.blockPosition().distSqr(pos) < 64 * 64).forEach(p -> p.sendSystemMessage(message));
    }

    private void announce(ServerLevel level, Component message) {
        announce(level, blockPosition(), message);
    }

    // ---- hardening ---------------------------------------------------------------------------

    private static Harm harmOf(DamageSource source) {
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return Harm.PROJECTILE;
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return Harm.EXPLOSION;
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) return Harm.MAGIC;
        if (source.getEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity()) return Harm.MELEE;
        return null;
    }

    /** Fire, lightning and consecrated steel strip what it has learned. */
    private static boolean strips(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_LIGHTNING)) return true;
        return source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
                && attacker.getMainHandItem().is(ModRegistry.CONSECRATED_CLEAVER.get());
    }

    public int hardening(Harm harm) {
        return Math.min(PestConfig.ADAPT_MAX_STACKS.get(), strain.getOrDefault(harm, 0) / 2);
    }

    private void strip(ServerLevel level) {
        if (strain.isEmpty()) return;
        strain.clear();
        level.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY() + getBbHeight() * 0.5, getZ(), 25, getBbWidth() * 0.5, getBbHeight() * 0.4, getBbWidth() * 0.5, 0.02);
        level.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1f, 1.4f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level() instanceof ServerLevel level && !isInvulnerableTo(source)) {
            if (strips(source)) {
                strip(level);
                if (source.is(DamageTypeTags.IS_FIRE)) amount *= 1.5f;
            } else {
                Harm harm = harmOf(source);
                if (harm != null && invulnerableTime <= 10) {
                    int before = hardening(harm);
                    amount *= (float) (1 - before * PestConfig.ADAPT_PER_STACK.get());
                    strain.merge(harm, 1, Integer::sum);
                    if (hardening(harm) > before) {
                        // Readable: a dull clank and a puff of grey dust each time it learns
                        level.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.8f, 0.6f);
                        level.sendParticles(ParticleTypes.ASH, getX(), getY() + getBbHeight() * 0.5, getZ(), 30, getBbWidth() * 0.5, getBbHeight() * 0.4, getBbWidth() * 0.5, 0.05);
                    }
                }
            }
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && level() instanceof ServerLevel level) {
            onHurt(level, source);
            if (!calledForHelp && isAlive() && getHealth() < getMaxHealth() * 0.5f) {
                calledForHelp = true;
                callTheNest(level);
            }
        }
        return hurt;
    }

    /** Hook for form-specific reactions to being hurt. */
    protected void onHurt(ServerLevel level, DamageSource source) {}

    /** The nearest nest sends help. Nothing comes from thin air: no nest, no help. */
    private void callTheNest(ServerLevel level) {
        Tracker.nearestNest(level, blockPosition(), 32).ifPresent(nest -> {
            NestBlockEntity.reinforce(level, nest, 1 + tier());
            level.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.2f, 1.4f);
        });
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            callTheNest(level);
            if (source.getEntity() instanceof Player) Plague.addPoints(this, PestConfig.POINTS_FORM_KILLED.get() * tier());
        }
    }

    // ---- ticking -----------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) return;
        int churn = entityData.get(CHURN);
        if (churn > 0) {
            getNavigation().stop();
            if (churn % 10 == 0) {
                level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + getBbHeight() * 0.5, getZ(), 12, getBbWidth() * 0.5, getBbHeight() * 0.4, getBbWidth() * 0.5, 0.02);
            }
            entityData.set(CHURN, churn - 1);
            if (churn == 1) finishGrowing(level);
        }
        if (tickCount % 20 == 0) {
            boolean smoke = Tracker.inBrazier(level, blockPosition()) || level.players().stream()
                    .anyMatch(p -> p.distanceToSqr(this) <= CenserItem.RADIUS * CenserItem.RADIUS && CenserItem.isSwinging(p));
            if (smoke) {
                strip(level);
                hurt(damageSources().magic(), 2f);
            }
            if (!strain.isEmpty() && strain.values().stream().anyMatch(v -> v >= 6)) {
                level.sendParticles(ParticleTypes.ASH, getX(), getY() + getBbHeight() * 0.6, getZ(), 4, getBbWidth() * 0.4, getBbHeight() * 0.3, getBbWidth() * 0.4, 0.01);
            }
        }
        formTick(level);
        if (bar != null) bar.setProgress(getHealth() / getMaxHealth());
    }

    /** Each line's own trick; not called while it churns. */
    protected abstract void formTick(ServerLevel level);

    protected boolean busy() {
        return isChurning();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(6) == 0) {
            level().addParticle(ModRegistry.FLY.get(), getRandomX(0.8), getY() + getBbHeight() * (0.4 + random.nextDouble() * 0.7), getRandomZ(0.8), 0, 0, 0);
        }
    }

    // ---- contact -----------------------------------------------------------------------------

    protected abstract Disease disease();

    @Override
    public boolean doHurtTarget(Entity target) {
        if (isChurning()) return false;
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            Plague.tryContactInfect(living, PestConfig.AFFLICTED_BITE_CHANCE.get() * (1 + 0.5 * (tier() - 1)), disease());
            onHit(living);
        }
        return hit;
    }

    protected void onHit(LivingEntity target) {}

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(ModRegistry.PLAGUE) && super.canBeAffected(effect);
    }

    @Override
    public int minPhase() {
        return PestConfig.COALESCE_PHASE.get();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ---- boss bar for the Pestilent tier -------------------------------------------------------

    private void updateBar() {
        if (level().isClientSide) return;
        if (tier() >= PESTILENT && bar == null) {
            bar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_6);
        } else if (bar != null) {
            bar.setName(getDisplayName());
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (bar != null) bar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (bar != null) bar.removePlayer(player);
    }

    // ---- save --------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Tier", tier());
        tag.putInt("Kills", kills);
        tag.putBoolean("CalledForHelp", calledForHelp);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Tier")) setTier(tag.getInt("Tier"));
        kills = tag.getInt("Kills");
        calledForHelp = tag.getBoolean("CalledForHelp");
    }

    // ---- sounds and animation ------------------------------------------------------------------

    @Override
    public float getVoicePitch() {
        return (0.65f - 0.08f * (tier() - 1)) + random.nextFloat() * 0.1f;
    }

    @Override
    protected float getSoundVolume() {
        return 1f + 0.25f * (tier() - 1);
    }

    protected abstract SoundEvent ambient();

    @Override
    protected SoundEvent getAmbientSound() {
        return ambient();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController(this));
        controllers.add(DefaultAnimations.genericAttackAnimation(this, DefaultAnimations.ATTACK_BITE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
