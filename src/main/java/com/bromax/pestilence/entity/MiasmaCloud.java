package com.bromax.pestilence.entity;

import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Plague;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import com.bromax.pestilence.ModRegistry;

/** A lingering cloud of foul air. Unmasked people and animals standing in it may catch the plague. */
public class MiasmaCloud extends Entity {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(MiasmaCloud.class, EntityDataSerializers.FLOAT);
    private static final int COLOR = 0x6b7330;

    private int duration = 200;

    public MiasmaCloud(EntityType<? extends MiasmaCloud> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static void spawn(ServerLevel level, double x, double y, double z, float radius, int duration) {
        MiasmaCloud cloud = ModRegistry.MIASMA_CLOUD.get().create(level);
        if (cloud == null) return;
        cloud.entityData.set(RADIUS, radius);
        cloud.duration = duration;
        cloud.moveTo(x, y, z);
        level.addFreshEntity(cloud);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 2f);
    }

    public float radius() {
        return entityData.get(RADIUS);
    }

    @Override
    public void tick() {
        super.tick();
        float r = radius();
        if (level().isClientSide) {
            int count = Mth.ceil(r * r * 1.5f);
            for (int i = 0; i < count; i++) {
                double a = random.nextDouble() * Mth.TWO_PI, d = Math.sqrt(random.nextDouble()) * r;
                level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, COLOR),
                        getX() + Math.cos(a) * d, getY() + random.nextDouble() * 1.2, getZ() + Math.sin(a) * d, 0, 0, 0);
            }
            return;
        }
        if (tickCount >= duration || tickCount % 20 == 1 && com.bromax.pestilence.world.Tracker.inBrazier(level(), blockPosition())) {
            discard();
            return;
        }
        if (tickCount % 20 == 0) {
            AABB box = new AABB(getX() - r, getY() - 0.5, getZ() - r, getX() + r, getY() + 2, getZ() + r);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.distanceToSqr(getX(), e.getY(), getZ()) <= r * r && Plague.canCatch(e) && !Plague.wearsMask(e))) {
                if (random.nextDouble() < PestConfig.MIASMA_CHANCE.get()) Plague.infect(e);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
