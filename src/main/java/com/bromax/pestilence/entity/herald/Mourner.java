package com.bromax.pestilence.entity.herald;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A veiled, weeping figure. Anyone within 10 blocks of it is gripped by Dread: no sprinting, heavy feet. */
public class Mourner extends HeraldMob {
    public Mourner(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(24, 0.2, 4);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 40 != 0 || !(level() instanceof ServerLevel level)) return;
        if (tickCount % 120 == 0) level.playSound(null, blockPosition(), SoundEvents.GHAST_AMBIENT, SoundSource.HOSTILE, 0.6f, 1.6f);
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(10), p -> !p.isCreative() && !p.isSpectator())) {
            p.addEffect(new MobEffectInstance(ModRegistry.DREAD, 60, 0, true, true));
        }
    }
}
