package com.bromax.pestilence.entity.herald;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.disease.Disease;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.item.CenserItem;
import com.bromax.pestilence.world.Tracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Foul air given shape. Floats; arrows pass straight through it; its touch carries pneumonic
 * plague and Delirium. Fire hurts it double, and the smoke of a lit brazier or censer tears it apart.
 */
public class MiasmaWraith extends HeraldMob {
    public MiasmaWraith(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(16, 0.25, 3).add(Attributes.FLYING_SPEED, 0.35);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected Disease contactDisease() {
        Disease d = super.contactDisease();
        return d == Disease.ROT ? d : Disease.PNEUMONIC;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(ModRegistry.DELIRIUM, 400, 0));
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return false;
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 2 : amount);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 20 != 0 || !(level() instanceof ServerLevel level)) return;
        boolean smoke = Tracker.inBrazier(level, blockPosition()) || level.players().stream()
                .anyMatch(p -> p.distanceToSqr(this) <= CenserItem.RADIUS * CenserItem.RADIUS && CenserItem.isSwinging(p));
        if (smoke) hurt(damageSources().magic(), 4f);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override protected SoundEvent ambient() { return SoundEvents.VEX_AMBIENT; }
    @Override protected SoundEvent hurt() { return SoundEvents.VEX_HURT; }
    @Override protected SoundEvent death() { return SoundEvents.VEX_DEATH; }
}
