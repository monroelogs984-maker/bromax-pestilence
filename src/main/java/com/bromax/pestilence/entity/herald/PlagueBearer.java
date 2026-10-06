package com.bromax.pestilence.entity.herald;

import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.disease.Disease;
import com.bromax.pestilence.entity.Corpse;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * "Bring out your dead." A hulking figure hauling a sack of bodies and ringing a handbell you hear
 * long before you see it. It leaves a corpse behind every half minute near the living, and spills
 * more when it falls. Burn them.
 */
public class PlagueBearer extends HeraldMob {
    private static final List<EntityType<?>> BODIES = List.of(EntityType.VILLAGER, EntityType.COW, EntityType.PIG, EntityType.SHEEP);

    public PlagueBearer(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(40, 0.2, 5).add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.ATTACK_KNOCKBACK, 0.8);
    }

    @Override
    protected Disease contactDisease() {
        Disease d = super.contactDisease();
        return d == Disease.ROT ? d : Disease.BUBONIC;
    }

    @Override
    protected boolean hasFlies() {
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) return;
        if (tickCount % 80 == 0) level.playSound(null, blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.5f, 1.6f);
        if (tickCount % 600 == 0 && level.getNearestPlayer(this, 32) != null) dropBody(level);
    }

    private void dropBody(ServerLevel level) {
        EntityType<?> body = BODIES.get(random.nextInt(BODIES.size()));
        Corpse.place(level, body, PestConfig.CORPSE_RISE_TICKS.get() / 2, getX() + random.nextGaussian(), getY(), getZ() + random.nextGaussian(), random.nextFloat() * 360f);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) for (int i = 0; i < 2 + random.nextInt(2); i++) dropBody(level);
    }

    @Override
    public void setTarget(net.minecraft.world.entity.LivingEntity target) {
        super.setTarget(target instanceof Player || target instanceof net.minecraft.world.entity.npc.AbstractVillager ? target : null);
    }
}
