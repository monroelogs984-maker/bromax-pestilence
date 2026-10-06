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
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putDouble("points", points);
        tag.putInt("phase", phase);
        tag.putLong("lastDay", lastDay);
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

    public void set(MinecraftServer server, double value) {
        points = Math.max(0, Math.min(value, threshold(Phase.MAX) * 1.25));
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
            add(server, PestConfig.DAILY_GROWTH.get());
        }
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
