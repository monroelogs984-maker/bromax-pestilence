package com.bromax.pestilence.entity.herald;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.entity.MiasmaCloud;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A swelling sac that grows out of mature nests. It never moves. Strike it up close, or stand
 * beside it once it has swollen, and it bursts into a miasma cloud (bigger the more swollen).
 * Killed from range it just deflates, leaving its ichor.
 */
public class Pustule extends HeraldMob {
    private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(Pustule.class, EntityDataSerializers.INT);

    public Pustule(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(10, 0.0, 0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SIZE, 0);
    }

    public int size() {
        return entityData.get(SIZE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8f));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) return;
        if (tickCount % 600 == 0 && size() < 2) entityData.set(SIZE, size() + 1);
        if (size() >= 1 && tickCount % 10 == 0 && level.getNearestPlayer(getX(), getY(), getZ(), 2.5,
                p -> !((Player) p).isCreative() && !p.isSpectator()) != null) {
            burst(level);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean melee = source.getDirectEntity() != null && source.getDirectEntity() == source.getEntity() && !source.is(DamageTypeTags.IS_PROJECTILE);
        if (melee && level() instanceof ServerLevel level && !isRemoved()) {
            burst(level);
            return true;
        }
        return super.hurt(source, amount);
    }

    private void burst(ServerLevel level) {
        MiasmaCloud.spawn(level, getX(), getY(), getZ(), 2f + size(), 200);
        playSound(SoundEvents.SLIME_SQUISH, 1.5f, 0.5f);
        spawnAtLocation(new ItemStack(ModRegistry.PESTILENT_ICHOR.get()));
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("size", size());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SIZE, tag.getInt("size"));
    }

    @Override protected SoundEvent ambient() { return SoundEvents.SLIME_SQUISH_SMALL; }
    @Override protected SoundEvent hurt() { return SoundEvents.SLIME_HURT; }
    @Override protected SoundEvent death() { return SoundEvents.SLIME_DEATH; }
    @Override protected SoundEvent step() { return SoundEvents.SLIME_SQUISH_SMALL; }
}
