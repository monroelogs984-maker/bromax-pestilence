package com.bromax.pestilence.world;

import com.bromax.pestilence.PestConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Server-side index of loaded nests and lit braziers per dimension, so blight healing, brazier
 * protection and nest spacing never have to scan blocks.
 */
public final class Tracker {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> NESTS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<BlockPos>> BRAZIERS = new HashMap<>();

    private Tracker() {}

    public static void addNest(Level level, BlockPos pos) {
        NESTS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
    }

    public static void removeNest(Level level, BlockPos pos) {
        Set<BlockPos> set = NESTS.get(level.dimension());
        if (set != null) set.remove(pos);
    }

    public static void setBrazier(Level level, BlockPos pos, boolean lit) {
        Set<BlockPos> set = BRAZIERS.computeIfAbsent(level.dimension(), k -> new HashSet<>());
        if (lit) set.add(pos.immutable()); else set.remove(pos);
    }

    public static boolean nestWithin(Level level, BlockPos pos, double range) {
        return countNests(level, pos, range) > 0;
    }

    public static int countNests(Level level, BlockPos pos, double range) {
        Set<BlockPos> set = NESTS.get(level.dimension());
        if (set == null) return 0;
        double r2 = range * range;
        int n = 0;
        for (BlockPos p : set) if (p.distSqr(pos) <= r2) n++;
        return n;
    }

    /** Inside the protected circle of any lit Herb Brazier. */
    public static boolean inBrazier(Level level, BlockPos pos) {
        Set<BlockPos> set = BRAZIERS.get(level.dimension());
        if (set == null || set.isEmpty()) return false;
        double r2 = Math.pow(PestConfig.BRAZIER_RADIUS.get(), 2);
        for (BlockPos p : set) if (p.distSqr(pos) <= r2) return true;
        return false;
    }

    public static void clear() {
        NESTS.clear();
        BRAZIERS.clear();
    }
}
