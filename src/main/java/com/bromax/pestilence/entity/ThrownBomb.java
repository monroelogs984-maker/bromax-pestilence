package com.bromax.pestilence.entity;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.entity.vermin.RatSwarm;
import com.bromax.pestilence.entity.vermin.VerminCloud;
import com.bromax.pestilence.world.Blight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

/**
 * A thrown bomb. QUICKLIME bursts in a white cloud: blight within three blocks is cleansed, corpses
 * dissolve, vermin clouds die, and the plague's creatures are burned (4) and slowed.
 * FUMIGATION leaves a cloud of herb smoke that works as a brazier for thirty seconds.
 */
public class ThrownBomb extends ThrowableItemProjectile {
    public ThrownBomb(EntityType<? extends ThrownBomb> type, Level level) {
        super(type, level);
    }

    public ThrownBomb(Level level, LivingEntity thrower) {
        super(ModRegistry.PLAGUE_BOMB.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModRegistry.QUICKLIME_BOMB.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel level)) return;
        if (getItem().is(ModRegistry.FUMIGATION_BOMB.get())) {
            FumigationCloud.spawn(level, getX(), getY(), getZ());
            level.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1f, 0.6f);
        } else {
            quicklime(level);
        }
        discard();
    }

    private void quicklime(ServerLevel level) {
        BlockPos c = blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-3, -2, -3), c.offset(3, 2, 3))) Blight.heal(level, p);
        AABB area = new AABB(c).inflate(3);
        level.getEntitiesOfClass(Corpse.class, area).forEach(Corpse::dissolve);
        level.getEntitiesOfClass(VerminCloud.class, area).forEach(VerminCloud::discard);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e instanceof AfflictedMob || e instanceof PlagueRat || e instanceof RatSwarm)) {
            e.hurt(damageSources().thrown(this, getOwner()), 4f);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        }
        level.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY() + 0.5, getZ(), 80, 1.5, 0.6, 1.5, 0.05);
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 20, 1.2, 0.3, 1.2, 0.02);
        level.playSound(null, c, SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.5f, 0.6f);
    }
}
