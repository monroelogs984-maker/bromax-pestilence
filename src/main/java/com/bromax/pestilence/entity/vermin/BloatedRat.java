package com.bromax.pestilence.entity.vermin;

import com.bromax.pestilence.entity.MiasmaCloud;
import com.bromax.pestilence.entity.PlagueRat;
import com.bromax.pestilence.phase.Phase;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Swollen fit to split. Close to its prey it swells for a second and a half (you can see it, and
 * hear it) and bursts into a miasma cloud. It bursts when killed, too: kill it at range.
 */
public class BloatedRat extends PlagueRat {
    private static final EntityDataAccessor<Integer> SWELL = SynchedEntityData.defineId(BloatedRat.class, EntityDataSerializers.INT);
    private static final int FUSE = 30;
    private static final RawAnimation SWELLING = RawAnimation.begin().thenLoop("misc.swell");

    public BloatedRat(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PlagueRat.createAttributes().add(Attributes.MAX_HEALTH, 8).add(Attributes.MOVEMENT_SPEED, 0.24);
    }

    @Override
    public int minPhase() {
        return Phase.RAT_SEASON;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SWELL, 0);
    }

    public boolean isSwelling() {
        return entityData.get(SWELL) > 0;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = getTarget();
        int swell = entityData.get(SWELL);
        if (target != null && distanceToSqr(target) < 6.25) {
            if (swell == 0) playSound(SoundEvents.PUFFER_FISH_BLOW_UP, 1f, 0.6f);
            entityData.set(SWELL, ++swell);
            if (swell >= FUSE && level() instanceof ServerLevel level) burst(level);
        } else if (swell > 0) {
            entityData.set(SWELL, swell - 1);
        }
    }

    private void burst(ServerLevel level) {
        MiasmaCloud.spawn(level, getX(), getY(), getZ(), 3f, 200);
        playSound(SoundEvents.SLIME_SQUISH, 1.5f, 0.6f);
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) MiasmaCloud.spawn(level, getX(), getY(), getZ(), 3f, 200);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<>(this, "swell", 0, state -> isSwelling() ? state.setAndContinue(SWELLING) : PlayState.STOP));
    }
}
