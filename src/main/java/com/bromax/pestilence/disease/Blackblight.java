package com.bromax.pestilence.disease;

import com.bromax.pestilence.ModDamage;
import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.entity.PlagueDoctor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The Blackblight, the secret ninth phase's sickness: ten stages, and the tenth is death. Only
 * people catch it. No mask, garb or amulet keeps it out entirely; Theriac sets it back two stages
 * (curing it in the first two); killing the Black Bloom cures everyone.
 */
public final class Blackblight {
    public static final int DEATH_STAGE = 10;

    private Blackblight() {}

    public static boolean has(LivingEntity entity) {
        return entity.hasEffect(ModRegistry.BLACKBLIGHT);
    }

    /** 1 to 9, or 0 if not afflicted. */
    public static int stage(LivingEntity entity) {
        MobEffectInstance effect = entity.getEffect(ModRegistry.BLACKBLIGHT);
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    public static boolean canCatch(LivingEntity entity) {
        if (!entity.isAlive() || entity.level().isClientSide || has(entity) || entity instanceof PlagueDoctor) return false;
        if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        return Plague.isPerson(entity);
    }

    /** Catch it, at stage 1. Returns whether it took. */
    public static boolean infect(LivingEntity entity) {
        if (!canCatch(entity)) return false;
        setStage(entity, 1);
        return true;
    }

    public static void setStage(LivingEntity entity, int stage) {
        entity.removeEffect(ModRegistry.BLACKBLIGHT);
        if (stage <= 0) {
            notify(entity, Component.translatable("message." + Pestilence.MODID + ".blackblight.eased_away").withStyle(ChatFormatting.DARK_GREEN));
            return;
        }
        if (stage >= DEATH_STAGE) {
            kill(entity);
            return;
        }
        entity.addEffect(new MobEffectInstance(ModRegistry.BLACKBLIGHT, PestConfig.BLACKBLIGHT_STAGE_TICKS.get(), stage - 1));
        Component warning = Component.translatable("message." + Pestilence.MODID + ".blackblight.stage", stage, DEATH_STAGE)
                .withStyle(stage >= 8 ? ChatFormatting.DARK_RED : ChatFormatting.DARK_GRAY, ChatFormatting.BOLD);
        notify(entity, warning);
        if (stage == DEATH_STAGE - 1) notify(entity, Component.translatable("message." + Pestilence.MODID + ".blackblight.last").withStyle(ChatFormatting.RED));
        if (entity.level() instanceof ServerLevel level) {
            level.playSound(null, entity.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1f, 0.5f + stage * 0.05f);
        }
    }

    /** The stage ran out: on to the next (and at the tenth, death). Called from the entity tick. */
    public static void advance(LivingEntity entity) {
        setStage(entity, stage(entity) + 1);
    }

    /** Theriac: back two stages. */
    public static void ease(LivingEntity entity) {
        setStage(entity, stage(entity) - 2);
    }

    private static void kill(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SQUID_INK, entity.getX(), entity.getY(0.5), entity.getZ(), 60, 0.4, 0.7, 0.4, 0.05);
            level.playSound(null, entity.blockPosition(), SoundEvents.WARDEN_DEATH, SoundSource.PLAYERS, 1f, 0.6f);
        }
        entity.hurt(ModDamage.blackblight(entity.level()), Float.MAX_VALUE);
    }

    /** People within a few blocks of someone far gone (stage 4+) may catch it; a mask or censer only halves the risk. */
    public static void spread(ServerLevel level, LivingEntity carrier) {
        if (stage(carrier) < 4) return;
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, carrier.getBoundingBox().inflate(3), e -> e != carrier && canCatch(e))) {
            if (level.random.nextDouble() < PestConfig.BLACKBLIGHT_SPREAD.get() * Math.max(0.5, Plague.airExposure(other))) infect(other);
        }
    }

    /** The Black Bloom is dead: everyone everywhere is cured. */
    public static void cureAll(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living && has(living)) living.removeEffect(ModRegistry.BLACKBLIGHT);
            }
        }
    }

    private static void notify(LivingEntity entity, Component message) {
        if (entity instanceof Player player) player.sendSystemMessage(message);
    }
}
