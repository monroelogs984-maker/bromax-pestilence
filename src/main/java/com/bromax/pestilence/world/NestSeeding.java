package com.bromax.pestilence.world;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.block.NestBlock;
import com.bromax.pestilence.block.NestKind;
import com.bromax.pestilence.phase.Phase;
import com.bromax.pestilence.phase.PestilenceData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where nests come from: periodic rolls near players (from phase 3), stage IV nests seeding new
 * ones, and Plague Pits opening where three corpses have risen close together (from phase 4).
 * Nests never form near a player's respawn point or inside a lit brazier's circle.
 */
public final class NestSeeding {
    private static final Map<ResourceKey<Level>, Deque<Rise>> RISES = new HashMap<>();

    private record Rise(BlockPos pos, long time) {}

    private NestSeeding() {}

    public static void roll(ServerLevel level, ServerPlayer player) {
        int phase = PestilenceData.phase(level);
        if (phase < Phase.RAT_SEASON) return;
        RandomSource random = player.getRandom();
        if (random.nextDouble() >= PestConfig.NEST_SEED_CHANCE_PER_PHASE.get() * (phase - 2)) return;
        if (Tracker.countNests(level, player.blockPosition(), 64) >= 1 + phase / 2) return;
        NestKind kind = pickKind(phase, random);
        if (kind == null) return;
        for (int attempt = 0; attempt < 10; attempt++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            double dist = 32 + random.nextDouble() * 32;
            BlockPos pos = surface(level, player.getX() + Mth.cos(angle) * dist, player.getZ() + Mth.sin(angle) * dist);
            if (pos != null && canPlace(level, pos, PestConfig.NEST_SPACING.get()) && farFromHome(player, pos)) {
                place(level, pos, kind);
                return;
            }
        }
    }

    /** A stage IV nest plants a young one of its own kind 16 to 32 blocks away. */
    public static void seedNear(ServerLevel level, BlockPos origin, NestKind kind) {
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < 6; attempt++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            double dist = 16 + random.nextDouble() * 16;
            BlockPos pos = surface(level, origin.getX() + Mth.cos(angle) * dist, origin.getZ() + Mth.sin(angle) * dist);
            if (pos != null && canPlace(level, pos, 16) && level.players().stream().allMatch(p -> farFromHome(p, pos))) {
                place(level, pos, kind);
                return;
            }
        }
    }

    /** Called when a corpse rises: three risings within 12 blocks over two days open a Plague Pit. */
    public static void corpseRose(ServerLevel level, BlockPos pos) {
        if (PestilenceData.phase(level) < Phase.OUTBREAK) return;
        Deque<Rise> rises = RISES.computeIfAbsent(level.dimension(), k -> new ArrayDeque<>());
        long now = level.getGameTime();
        rises.removeIf(r -> now - r.time > 48000);
        rises.add(new Rise(pos.immutable(), now));
        List<Rise> near = new ArrayList<>();
        for (Rise r : rises) if (r.pos.distSqr(pos) <= 144) near.add(r);
        if (near.size() < 3 || Tracker.nestWithin(level, pos, 24)) return;
        BlockPos at = surface(level, pos.getX() + 0.5, pos.getZ() + 0.5);
        if (at != null && canPlace(level, at, 16)) {
            place(level, at, NestKind.PIT);
            rises.removeAll(near);
        }
    }

    /** Place a young nest on open ground within `radius` of a spot (used when a village falls). */
    public static void placeNear(ServerLevel level, BlockPos centre, NestKind kind, int radius) {
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < 16; attempt++) {
            BlockPos pos = surface(level, centre.getX() + random.nextInt(2 * radius + 1) - radius, centre.getZ() + random.nextInt(2 * radius + 1) - radius);
            if (pos != null && !Tracker.nestWithin(level, pos, 8)) {
                place(level, pos, kind);
                return;
            }
        }
    }

    @Nullable
    private static NestKind pickKind(int phase, RandomSource random) {
        int total = 0;
        for (NestKind k : NestKind.values()) if (phase >= k.phase) total += k.weight;
        if (total == 0) return null;
        int roll = random.nextInt(total);
        for (NestKind k : NestKind.values()) {
            if (phase < k.phase) continue;
            if ((roll -= k.weight) < 0) return k;
        }
        return null;
    }

    /** The air block on top of natural, solid, dry ground at (x, z), or null. */
    @Nullable
    private static BlockPos surface(ServerLevel level, double x, double z) {
        int bx = Mth.floor(x), bz = Mth.floor(z);
        BlockPos pos = new BlockPos(bx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz), bz);
        if (!level.isLoaded(pos)) return null;
        BlockState ground = level.getBlockState(pos.below());
        if (!ground.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) return null;
        if (!(ground.is(BlockTags.DIRT) || ground.is(BlockTags.BASE_STONE_OVERWORLD) || ground.is(BlockTags.SAND)
                || ground.is(ModRegistry.BLIGHTED_SOIL.get()))) return null;
        BlockState here = level.getBlockState(pos);
        return here.isAir() || here.canBeReplaced() && here.getFluidState().isEmpty() ? pos : null;
    }

    private static boolean canPlace(ServerLevel level, BlockPos pos, int spacing) {
        return !Tracker.inBrazier(level, pos) && !Tracker.nestWithin(level, pos, spacing);
    }

    private static boolean farFromHome(ServerPlayer player, BlockPos pos) {
        BlockPos home = player.getRespawnPosition();
        boolean sameDim = player.getRespawnDimension() == Level.OVERWORLD;
        return home == null || !sameDim || home.distSqr(pos) > 32 * 32;
    }

    private static void place(ServerLevel level, BlockPos pos, NestKind kind) {
        level.setBlock(pos, block(kind).defaultBlockState().setValue(NestBlock.STAGE, 1), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.5f, 0.5f);
        Component msg = Component.translatable("message." + Pestilence.MODID + ".nest_formed." + kind.id)
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().distSqr(pos) <= 96 * 96) p.displayClientMessage(msg, true);
        }
    }

    public static Block block(NestKind kind) {
        return switch (kind) {
            case WARREN -> ModRegistry.RAT_WARREN.get();
            case PIT -> ModRegistry.PLAGUE_PIT.get();
            case WELL -> ModRegistry.MIASMA_WELL.get();
        };
    }

    public static void clear() {
        RISES.clear();
    }
}
