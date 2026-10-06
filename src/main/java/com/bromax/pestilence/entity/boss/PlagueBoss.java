package com.bromax.pestilence.entity.boss;

import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.entity.herald.HeraldMob;
import com.bromax.pestilence.phase.PestilenceData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A boss of the Pestilence: a boss bar, never despawns, never counted as a natural spawn, and its
 * death beats the plague back (by default two phases; the Pale Rider breaks it entirely).
 */
public abstract class PlagueBoss extends HeraldMob {
    protected final ServerBossEvent bar;

    protected PlagueBoss(EntityType<? extends AfflictedMob> type, Level level, BossEvent.BossBarColor colour) {
        super(type, level);
        this.bar = new ServerBossEvent(getDisplayName(), colour, BossEvent.BossBarOverlay.NOTCHED_10);
        this.xpReward = 100;
        setPersistenceRequired();
    }

    @Override
    public int minPhase() {
        return Integer.MAX_VALUE;   // bosses never spawn naturally
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bar.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bar.removePlayer(player);
    }

    /** How many phases the Pestilence falls when this boss dies. */
    protected int phasesBroken() {
        return 2;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level && !isRemoved()) {
            PestilenceData.get(level.getServer()).fall(level.getServer(), phasesBroken());
            Component msg = Component.translatable("message." + Pestilence.MODID + ".boss_slain", getDisplayName()).withStyle(ChatFormatting.GOLD);
            level.getServer().getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(msg));
        }
    }

    /** Bosses linger long enough to be found: the outbreak doesn't end because they wandered off. */
    @Override
    public boolean isPersistenceRequired() {
        return true;
    }
}
