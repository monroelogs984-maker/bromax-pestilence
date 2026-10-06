package com.bromax.pestilence.entity.afflicted;

import com.bromax.pestilence.Pestilence;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Below half health it shrieks and goes into a frenzy: much faster for the rest of the fight. */
public class AfflictedPig extends AfflictedMob {
    private static final AttributeModifier FRENZY = new AttributeModifier(Pestilence.id("frenzy"), 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

    public AfflictedPig(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(14, 0.27, 3);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && getHealth() < getMaxHealth() / 2 && !speed.hasModifier(FRENZY.id())) {
            speed.addTransientModifier(FRENZY);
            playSound(SoundEvents.PIG_DEATH, 1.2f, 1.6f);
            ((ServerLevel) level()).sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getEyeY() + 0.3, getZ(), 4, 0.3, 0.2, 0.3, 0);
        }
    }

    @Override protected SoundEvent ambient() { return SoundEvents.PIG_AMBIENT; }
    @Override protected SoundEvent hurt() { return SoundEvents.PIG_HURT; }
    @Override protected SoundEvent death() { return SoundEvents.PIG_DEATH; }
    @Override protected SoundEvent step() { return SoundEvents.PIG_STEP; }
}
