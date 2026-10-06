package com.bromax.pestilence.phase;

import com.bromax.pestilence.PestConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.List;

/**
 * World-wide Plague Points, stored with the Overworld. The phase is derived from the points and
 * the configured thresholds; it rises and falls with them.
 */
public class PestilenceData extends SavedData {
    private static final String NAME = "bromaxpestilence_phase";
    private static final Factory<PestilenceData> FACTORY = new Factory<>(PestilenceData::new, PestilenceData::load, null);

    private double points;
    private int phase;
    private long lastDay = -1;
    private int halt = -1;
    private java.util.UUID rider;
    private long riderSince;

    public static PestilenceData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    /** Current phase, or 0 on the client / without a server. */
    public static int phase(Level level) {
        MinecraftServer server = level.getServer();
        return server == null ? 0 : get(server).phase;
    }

    private static PestilenceData load(CompoundTag tag, HolderLookup.Provider registries) {
        PestilenceData data = new PestilenceData();
        data.points = tag.getDouble("points");
        data.phase = tag.getInt("phase");
        data.lastDay = tag.contains("lastDay") ? tag.getLong("lastDay") : -1;
        if (tag.hasUUID("rider")) data.rider = tag.getUUID("rider");
        data.riderSince = tag.getLong("riderSince");
        data.halt = tag.contains("halt") ? tag.getInt("halt") : -1;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putDouble("points", points);
        tag.putInt("phase", phase);
        tag.putLong("lastDay", lastDay);
        if (rider != null) tag.putUUID("rider", rider);
        tag.putLong("riderSince", riderSince);
        tag.putInt("halt", halt);
        return tag;
    }

    public double points() {
        return points;
    }

    public int phase() {
        return phase;
    }

    public void add(MinecraftServer server, double amount) {
        set(server, points + amount);
    }

    /** The highest phase the Pestilence may reach: the config/preset cap, or where a player halted it. */
    public int cap() {
        return halt >= 0 ? Math.min(halt, PestConfig.maxPhase()) : PestConfig.maxPhase();
    }

    public int halted() {
        return halt;
    }

    public void halt(MinecraftServer server, int atPhase) {
        halt = atPhase;
        set(server, points);
    }

    public void resume() {
        halt = -1;
        setDirty();
    }

    public void set(MinecraftServer server, double value) {
        points = Math.max(0, Math.min(value, threshold(Phase.MAX) * 1.25));
        if (cap() < Phase.MAX) points = Math.min(points, threshold(cap() + 1) - 0.01);
        int newPhase = phaseFor(points);
        if (newPhase != phase) {
            int old = phase;
            phase = newPhase;
            Phase.announce(server, old, newPhase);
        }
        setDirty();
    }

    /** Called every server tick: adds the daily growth once per new Minecraft day. */
    public void tickDay(MinecraftServer server) {
        long day = server.overworld().getDayTime() / 24000L;
        if (lastDay < 0) {
            lastDay = day;
            setDirty();
        } else if (day > lastDay) {
            lastDay = day;
            add(server, PestConfig.dailyGrowth());
        }
    }

    /** A boss has fallen: the Pestilence drops `phases` phases (to the start of the lower one). */
    public void fall(MinecraftServer server, int phases) {
        set(server, threshold(Math.max(0, phase - phases)));
        if (phase == 0) rider = null;
    }

    /** The Pale Rider currently abroad, remembered for three days so it is not summoned twice while out of sight. */
    @org.jetbrains.annotations.Nullable
    public java.util.UUID rider(long gameTime) {
        if (rider != null && gameTime - riderSince > 72000) rider = null;
        return rider;
    }

    public void setRider(java.util.UUID id, long gameTime) {
        rider = id;
        riderSince = gameTime;
        setDirty();
    }

    public static double threshold(int phase) {
        if (phase <= 0) return 0;
        List<? extends Integer> list = PestConfig.PHASE_THRESHOLDS.get();
        return list.get(Math.min(phase, list.size()) - 1);
    }

    private static int phaseFor(double points) {
        int p = 0;
        for (int i = 1; i <= Phase.MAX; i++) {
            if (points >= threshold(i)) p = i;
        }
        return p;
    }
}
