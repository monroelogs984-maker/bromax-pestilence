package com.bromax.pestilence.entity.afflicted;

import com.bromax.pestilence.Plague;
import com.bromax.pestilence.disease.Disease;
import com.bromax.pestilence.effect.PlagueEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;

/**
 * A villager the plague took. Reaches out for the living, opens doors, and never stops coughing:
 * every few seconds anyone unmasked within three blocks may catch pneumonic plague from it.
 */
public class AfflictedVillager extends AfflictedMob {
    public AfflictedVillager(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation nav) nav.setCanOpenDoors(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(20, 0.23, 3);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new OpenDoorGoal(this, false));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 100 != 0 || !(level() instanceof ServerLevel level)) return;
        PlagueEffect.cough(level, this);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3),
                e -> Plague.canCatch(e) && !Plague.airProtected(e))) {
            if (random.nextDouble() < 0.15) Plague.infect(e, Disease.PNEUMONIC);
        }
    }

    @Override protected SoundEvent ambient() { return SoundEvents.ZOMBIE_VILLAGER_AMBIENT; }
    @Override protected SoundEvent hurt() { return SoundEvents.VILLAGER_HURT; }
    @Override protected SoundEvent death() { return SoundEvents.VILLAGER_DEATH; }
    @Override protected SoundEvent step() { return SoundEvents.ZOMBIE_VILLAGER_STEP; }
}
