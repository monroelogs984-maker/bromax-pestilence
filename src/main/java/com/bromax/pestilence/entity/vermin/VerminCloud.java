package com.bromax.pestilence.entity.vermin;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.disease.Disease;
import com.bromax.pestilence.item.CenserItem;
import com.bromax.pestilence.world.Tracker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/**
 * A cloud of tiny vermin, drawn only as specks. Swatting does nothing: only fire, an explosion, or
 * the smoke of a lit brazier or censer kills it.
 * FLEAS drift to the nearest host and jump on (bubonic plague; the Doctor's Garb keeps them off).
 * FLIES hang over the dead where they rose, and spoil the meat of anyone who comes close (Rotting).
 */
public class VerminCloud extends PathfinderMob {
    public enum Kind { FLEAS, FLIES }

    private final Kind kind;
    private BlockPos home;

    public VerminCloud(EntityType<? extends PathfinderMob> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        setNoGravity(true);
        noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 2).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public void tick() {
        super.tick();
        if (home == null) home = blockPosition();
        if (level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                level().addParticle(ModRegistry.FLY.get(), getRandomX(0.6), getY() + random.nextDouble() * 0.8, getRandomZ(0.6), 0, 0, 0);
            }
            return;
        }
        int lifetime = kind == Kind.FLEAS ? 1200 : 2400;
        if (tickCount > lifetime || tickCount % 20 == 0 && smoked()) {
            discard();
            return;
        }
        if (kind == Kind.FLEAS) seekHost(); else hover();
    }

    private boolean smoked() {
        return Tracker.inBrazier(level(), blockPosition()) || level().players().stream()
                .anyMatch(p -> p.distanceToSqr(this) <= CenserItem.RADIUS * CenserItem.RADIUS && CenserItem.isSwinging(p));
    }

    private void seekHost() {
        LivingEntity host = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(12), Plague::canCatch).stream()
                .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (host == null) {
            setDeltaMovement(getDeltaMovement().scale(0.8));
        } else {
            Vec3 to = host.position().add(0, host.getBbHeight() * 0.5, 0).subtract(position());
            setDeltaMovement(to.normalize().scale(0.12));
            if (to.lengthSqr() < 1.5) {
                Plague.tryContactInfect(host, 0.6, Disease.BUBONIC);
                discard();
                return;
            }
        }
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
    }

    private void hover() {
        Vec3 drift = Vec3.atCenterOf(home).add(Math.sin(tickCount * 0.05) * 1.5, 0.5 + Math.sin(tickCount * 0.11) * 0.3, Math.cos(tickCount * 0.05) * 1.5);
        setPos(drift);
        if (tickCount % 40 == 0) {
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(2.5), p -> !p.isCreative() && !p.isSpectator())) {
                p.addEffect(new MobEffectInstance(ModRegistry.ROTTING, 600, 0));
            }
            level().playSound(null, blockPosition(), SoundEvents.BEE_LOOP, SoundSource.HOSTILE, 0.4f, 1.6f);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.IS_FIRE) && !source.is(DamageTypeTags.IS_EXPLOSION) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {}

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.putLong("home", home.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("home")) home = BlockPos.of(tag.getLong("home"));
    }
}
