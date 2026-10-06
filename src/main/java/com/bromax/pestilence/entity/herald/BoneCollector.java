package com.bromax.pestilence.entity.herald;

import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.block.NestBlock;
import com.bromax.pestilence.entity.Corpse;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.world.Tracker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Gathers the dead (up to three) and carries them to the nearest nest, which grows a stage for
 * each delivery. Killing it drops whatever bodies it was carrying, still waiting to rise.
 */
public class BoneCollector extends HeraldMob {
    private int carried;

    public BoneCollector(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(24, 0.26, 3);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(4, new SeekCorpseGoal(this, 1.0, 24, corpse -> {
            if (carried >= 3 || corpse.getSource() == null) return;
            carried++;
            playSound(SoundEvents.SKELETON_STEP, 1f, 0.6f);
            corpse.discard();
        }));
        goalSelector.addGoal(3, new DeliverGoal());
    }

    /** With a body on its back, walk to the nearest nest within 64 blocks and feed it. */
    private class DeliverGoal extends Goal {
        private BlockPos nest;

        DeliverGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (carried == 0 || getTarget() != null) return false;
            nest = Tracker.nearestNest(level(), blockPosition(), 64).orElse(null);
            return nest != null;
        }

        @Override
        public void tick() {
            if (blockPosition().distSqr(nest) > 9) {
                if (getNavigation().isDone()) getNavigation().moveTo(nest.getX() + 0.5, nest.getY(), nest.getZ() + 0.5, 1.0);
            } else if (level() instanceof ServerLevel level) {
                carried--;
                NestBlock.grow(level, nest);
                level.playSound(null, nest, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.HOSTILE, 1f, 0.6f);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            for (int i = 0; i < carried; i++) {
                Corpse.place(level, EntityType.VILLAGER, PestConfig.CORPSE_RISE_TICKS.get() / 2, getX() + random.nextGaussian() * 0.5,
                        getY(), getZ() + random.nextGaussian() * 0.5, random.nextFloat() * 360f);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("carried", carried);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        carried = tag.getInt("carried");
    }
}
