package com.bromax.pestilence.entity.vermin;

import com.bromax.pestilence.entity.PlagueRat;
import com.bromax.pestilence.phase.Phase;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Digs itself in when it has nothing to chase, showing only a trickle of earth, and bursts out to
 * ambush anyone who comes within four blocks. Buried, only an explosion can reach it.
 */
public class BurrowerRat extends PlagueRat {
    private static final EntityDataAccessor<Boolean> BURROWED = SynchedEntityData.defineId(BurrowerRat.class, EntityDataSerializers.BOOLEAN);
    private int idle;

    public BurrowerRat(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public int minPhase() {
        return Phase.RAT_SEASON;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BURROWED, false);
    }

    public boolean isBurrowed() {
        return entityData.get(BURROWED);
    }

    @Override
    public boolean isImmobile() {
        return isBurrowed() || super.isImmobile();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) return;
        BlockState below = level.getBlockState(blockPosition().below());
        if (isBurrowed()) {
            getNavigation().stop();
            if (tickCount % 10 == 0) dirt(level, 2);
            Player prey = level.getNearestPlayer(this, 4);
            if (prey != null && !prey.isCreative() && !prey.isSpectator()) {
                entityData.set(BURROWED, false);
                dirt(level, 20);
                playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.5f, 1.2f);
                Vec3 leap = prey.position().subtract(position()).normalize().scale(0.6);
                setDeltaMovement(leap.x, 0.45, leap.z);
                setTarget(prey);
            }
        } else if (getTarget() == null && onGround() && (below.is(BlockTags.DIRT) || below.is(BlockTags.BASE_STONE_OVERWORLD))) {
            if (++idle > 200) {
                idle = 0;
                entityData.set(BURROWED, true);
                dirt(level, 12);
                playSound(SoundEvents.ROOTED_DIRT_PLACE, 1f, 1f);
            }
        } else {
            idle = 0;
        }
    }

    private void dirt(ServerLevel level, int count) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(blockPosition().below())),
                getX(), getY() + 0.1, getZ(), count, 0.2, 0.05, 0.2, 0.05);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isBurrowed() && !source.is(DamageTypeTags.IS_EXPLOSION) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("burrowed", isBurrowed());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(BURROWED, tag.getBoolean("burrowed"));
    }
}
