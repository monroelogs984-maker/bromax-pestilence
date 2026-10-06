package com.bromax.pestilence.entity.herald;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.entity.PlagueRat;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Never fights itself: it keeps its distance and whistles up plague rats (two at a time, up to four). Rush it. */
public class Ratcatcher extends HeraldMob {
    public Ratcatcher(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(20, 0.3, 1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 7f, 1.0, 1.3));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (getTarget() == null || tickCount % 240 != 0 || !(level() instanceof ServerLevel level)) return;
        if (level.getEntitiesOfClass(PlagueRat.class, new AABB(blockPosition()).inflate(16)).size() >= 4) return;
        level.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.HOSTILE, 1.5f, 1.8f);
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        for (int i = 0; i < 2; i++) {
            PlagueRat rat = ModRegistry.PLAGUE_RAT.get().create(level);
            if (rat == null) continue;
            rat.moveTo(getX() + random.nextGaussian(), getY(), getZ() + random.nextGaussian(), random.nextFloat() * 360f, 0f);
            rat.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            rat.setTarget(getTarget());
            level.addFreshEntity(rat);
        }
    }
}
