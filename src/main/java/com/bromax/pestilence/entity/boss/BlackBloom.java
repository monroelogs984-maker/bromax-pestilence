package com.bromax.pestilence.entity.boss;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.disease.Blackblight;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.entity.herald.Pustule;
import com.bromax.pestilence.phase.PestilenceData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Black Bloom: the Blackblight's heart, rooted where the Heart of the Pestilence was burned.
 * It never moves. Every few seconds it pulses, giving the Blackblight to those near; it raises
 * pustules and blackens the earth around it, wider the longer it lives. Fire hurts it double.
 * Its death ends the Blackblight and wipes the Pestilence back to nothing.
 */
public class BlackBloom extends PlagueBoss {
    public static final double PULSE_RADIUS = 16;

    public BlackBloom(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        this.xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(500, 0.0, 0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 6.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {}

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean hasFlies() {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) return;
        if (tickCount % 100 == 0) pulse(level);
        if (tickCount % 300 == 0 && level.getEntitiesOfClass(Pustule.class, getBoundingBox().inflate(10)).size() < 4) raisePustule(level);
        if (tickCount % 20 == 0) blacken(level);
    }

    private void pulse(ServerLevel level) {
        swing(InteractionHand.MAIN_HAND);
        level.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT, getSoundSource(), 4f, 0.5f);
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48;
            level.sendParticles(ParticleTypes.SQUID_INK, getX() + Math.cos(a) * 3, getY() + 0.3, getZ() + Math.sin(a) * 3, 1,
                    Math.cos(a) * 0.4, 0, Math.sin(a) * 0.4, 0.4);
        }
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(PULSE_RADIUS))) {
            if (random.nextFloat() < 0.35f) Blackblight.infect(player);
        }
    }

    private void raisePustule(ServerLevel level) {
        Pustule pustule = ModRegistry.PUSTULE.get().create(level);
        if (pustule == null) return;
        BlockPos at = blockPosition().offset(random.nextInt(13) - 6, 0, random.nextInt(13) - 6);
        at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
        pustule.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        pustule.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.MOB_SUMMONED, null);
        level.addFreshEntity(pustule);
        level.sendParticles(ParticleTypes.SQUID_INK, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.02);
    }

    /** Blacken a few blocks of earth, out to a radius that grows with the Bloom's age (up to 24). */
    private void blacken(ServerLevel level) {
        int radius = Math.min(24, 3 + tickCount / 400);
        for (int i = 0; i < 4; i++) {
            BlockPos column = blockPosition().offset(random.nextInt(2 * radius + 1) - radius, 0, random.nextInt(2 * radius + 1) - radius);
            BlockPos top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();
            BlockState state = level.getBlockState(top);
            if (state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND) || state.is(BlockTags.SAND)) {
                level.setBlockAndUpdate(top, ModRegistry.BLIGHTED_SOIL.get().defaultBlockState());
            } else if (state.is(BlockTags.LEAVES)) {
                level.setBlockAndUpdate(top, ModRegistry.WITHERED_LEAVES.get().defaultBlockState());
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) amount *= 2;
        return super.hurt(source, amount);
    }

    /** It takes the whole Pestilence down with it. */
    @Override
    protected int phasesBroken() {
        return 0;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level && !isRemoved()) {
            PestilenceData.get(level.getServer()).cleanse(level.getServer());
            Blackblight.cureAll(level.getServer());
            Component msg = Component.translatable("message." + Pestilence.MODID + ".blackblight.ended").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            level.getServer().getPlayerList().getPlayers().forEach(p -> {
                p.sendSystemMessage(msg);
                p.playNotifySound(SoundEvents.BELL_RESONATE, net.minecraft.sounds.SoundSource.AMBIENT, 1f, 1.4f);
            });
        }
    }

    @Override protected SoundEvent ambient() { return SoundEvents.SCULK_CATALYST_BLOOM; }
    @Override protected SoundEvent hurt() { return SoundEvents.SCULK_BLOCK_BREAK; }
    @Override protected SoundEvent death() { return SoundEvents.WARDEN_DEATH; }
    @Override protected SoundEvent step() { return SoundEvents.SCULK_BLOCK_STEP; }
}
