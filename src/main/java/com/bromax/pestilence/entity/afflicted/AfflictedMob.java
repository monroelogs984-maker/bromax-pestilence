package com.bromax.pestilence.entity.afflicted;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.disease.Disease;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;

/**
 * A plague-risen animal: hostile, immune to the plague, and its blows can pass it on. Flies follow
 * it, so it is readable from a distance. Each form adds its own trick on top.
 */
public abstract class AfflictedMob extends Monster implements GeoEntity {
    private static Map<EntityType<?>, EntityType<? extends AfflictedMob>> forms;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected AfflictedMob(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    /** The Afflicted form a plague-killed animal rises as, or null if it has none. */
    @Nullable
    public static EntityType<? extends AfflictedMob> formOf(@Nullable EntityType<?> type) {
        if (forms == null) {
            forms = Map.of(
                    EntityType.COW, ModRegistry.AFFLICTED_COW.get(),
                    EntityType.MOOSHROOM, ModRegistry.AFFLICTED_COW.get(),
                    EntityType.PIG, ModRegistry.AFFLICTED_PIG.get(),
                    EntityType.SHEEP, ModRegistry.AFFLICTED_SHEEP.get(),
                    EntityType.CHICKEN, ModRegistry.AFFLICTED_CHICKEN.get(),
                    EntityType.HORSE, ModRegistry.AFFLICTED_HORSE.get(),
                    EntityType.DONKEY, ModRegistry.AFFLICTED_HORSE.get(),
                    EntityType.MULE, ModRegistry.AFFLICTED_HORSE.get(),
                    EntityType.WOLF, ModRegistry.AFFLICTED_WOLF.get());
        }
        return type == null ? null : forms.get(type);
    }

    protected static AttributeSupplier.Builder base(double health, double speed, double damage) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, chaseSpeed(), true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
    }

    protected double chaseSpeed() {
        return 1.1;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) Plague.tryContactInfect(living, PestConfig.AFFLICTED_BITE_CHANCE.get(), Disease.SEPTICEMIC);
        return hit;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(ModRegistry.PLAGUE) && super.canBeAffected(effect);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(8) == 0) {
            level().addParticle(ModRegistry.FLY.get(), getRandomX(0.8), getY() + getBbHeight() * (0.5 + random.nextDouble() * 0.7),
                    getRandomZ(0.8), 0, 0, 0);
        }
    }

    protected abstract SoundEvent ambient();

    protected abstract SoundEvent hurt();

    protected abstract SoundEvent death();

    protected abstract SoundEvent step();

    @Override
    protected SoundEvent getAmbientSound() {
        return ambient();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return hurt();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return death();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(step(), 0.15f, 0.8f);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7f;
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
