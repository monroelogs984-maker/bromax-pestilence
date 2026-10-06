package com.bromax.pestilence.entity;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.entity.herald.MiasmaWraith;
import com.bromax.pestilence.entity.vermin.VerminCloud;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Thirty seconds of herb smoke from a fumigation bomb. Anyone inside is safe from coughs and
 * miasma (see Plague.airExposure); miasma, fleas and flies inside are cleared; wraiths are torn.
 */
public class FumigationCloud extends Entity {
    public static final double RADIUS = 4;
    private static final int DURATION = 600;

    public FumigationCloud(EntityType<? extends FumigationCloud> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static void spawn(ServerLevel level, double x, double y, double z) {
        FumigationCloud cloud = ModRegistry.FUMIGATION_CLOUD.get().create(level);
        if (cloud == null) return;
        cloud.moveTo(x, y, z);
        level.addFreshEntity(cloud);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            for (int i = 0; i < 4; i++) {
                double a = random.nextDouble() * Math.PI * 2, d = Math.sqrt(random.nextDouble()) * RADIUS;
                level().addParticle(i == 0 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.WHITE_ASH,
                        getX() + Math.cos(a) * d, getY() + random.nextDouble(), getZ() + Math.sin(a) * d, 0, 0.01, 0);
            }
            return;
        }
        if (tickCount > DURATION) {
            discard();
            return;
        }
        if (tickCount % 10 != 0) return;
        AABB area = new AABB(blockPosition()).inflate(RADIUS);
        level().getEntitiesOfClass(MiasmaCloud.class, area).forEach(Entity::discard);
        level().getEntitiesOfClass(VerminCloud.class, area).forEach(Entity::discard);
        for (MiasmaWraith w : level().getEntitiesOfClass(MiasmaWraith.class, area)) w.hurt(damageSources().magic(), 3f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
