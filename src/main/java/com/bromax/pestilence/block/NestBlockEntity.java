package com.bromax.pestilence.block;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.entity.Corpse;
import com.bromax.pestilence.entity.MiasmaCloud;
import com.bromax.pestilence.entity.PlagueRat;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.phase.Phase;
import com.bromax.pestilence.phase.PestilenceData;
import com.bromax.pestilence.world.Blight;
import com.bromax.pestilence.world.NestSeeding;
import com.bromax.pestilence.world.Tracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Nest behaviour. Nests only act while a player is within 48 blocks, and do nothing inside a lit
 * Herb Brazier's circle. Every nest grows, feeds the Pestilence daily and spreads the Blight;
 * warrens breed rats, pits turn up corpses, wells breathe miasma.
 */
public class NestBlockEntity extends BlockEntity {
    private static final List<EntityType<?>> PIT_BODIES = List.of(EntityType.COW, EntityType.COW, EntityType.PIG, EntityType.PIG,
            EntityType.SHEEP, EntityType.SHEEP, EntityType.CHICKEN, EntityType.HORSE, EntityType.WOLF);

    private int growth;
    private int dayTimer;
    private int timer;

    public NestBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.NEST_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NestBlockEntity nest) {
        if (!(level instanceof ServerLevel server) || !(state.getBlock() instanceof NestBlock block)) return;
        if (Tracker.inBrazier(server, pos)) return;
        int stage = state.getValue(NestBlock.STAGE);
        nest.timer++;

        if (++nest.growth >= PestConfig.NEST_STAGE_TICKS.get()) {
            nest.growth = 0;
            if (stage < 4) {
                server.setBlock(pos, state.setValue(NestBlock.STAGE, stage + 1), Block.UPDATE_ALL);
                server.playSound(null, pos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1f, 0.6f);
                if (stage + 1 == 4 && block.kind() == NestKind.WARREN) crownTheKing(server, pos);
            } else if (PestConfig.NEST_SPREADS.get()) {
                NestSeeding.seedNear(server, pos, block.kind());
            }
        }
        if (++nest.dayTimer >= 24000) {
            nest.dayTimer = 0;
            com.bromax.pestilence.Plague.addPoints(server, PestConfig.POINTS_NEST_DAILY.get() * stage);
        }
        nest.setChanged();

        if (server.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 48, false) == null) return;

        AABB area = new AABB(pos).inflate(16);
        switch (block.kind()) {
            case WARREN -> {
                if (nest.timer % 600 == 0 && server.getEntitiesOfClass(PlagueRat.class, area).size() < 2 * stage) {
                    PlagueRat rat = ratFor(server, stage).create(server);
                    if (rat != null) {
                        rat.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, server.random.nextFloat() * 360f, 0f);
                        rat.finalizeSpawn(server, server.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
                        server.addFreshEntity(rat);
                    }
                }
            }
            case PIT -> {
                if (nest.timer % 1200 == 0 && server.getEntitiesOfClass(Corpse.class, area).size()
                        + server.getEntitiesOfClass(AfflictedMob.class, area).size() < stage) {
                    BlockPos out = pos.relative(Direction.Plane.HORIZONTAL.getRandomDirection(server.random));
                    EntityType<?> body = PIT_BODIES.get(server.random.nextInt(PIT_BODIES.size()));
                    Corpse.place(server, body, PestConfig.CORPSE_RISE_TICKS.get() / 2, out.getX() + 0.5, out.getY() + 1, out.getZ() + 0.5,
                            server.random.nextFloat() * 360f);
                    server.playSound(null, pos, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 1f, 0.6f);
                }
            }
            case WELL -> {
                if (nest.timer % 400 == 0) MiasmaCloud.spawn(server, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 2 + stage, 200);
            }
        }
        heralds(server, pos, block.kind(), stage, nest.timer);

        if (PestConfig.BLIGHT_ENABLED.get() && nest.timer % PestConfig.BLIGHT_INTERVAL.get() == 0
                && PestilenceData.phase(server) >= Phase.OUTBREAK) {
            int attempts = block.kind() == NestKind.WELL ? 2 * stage : stage;
            Blight.spread(server, pos, PestConfig.BLIGHT_RADIUS_PER_STAGE.get() * stage + (block.kind() == NestKind.WELL ? 2 : 0), attempts);
        }
    }

    /** A warren that reaches stage IV (from Rat Season) gives up its Rat King, if none reigns nearby. */
    private static void crownTheKing(ServerLevel level, BlockPos pos) {
        if (PestilenceData.phase(level) < com.bromax.pestilence.entity.boss.RatKing.minimumPhase()
                || !level.getEntitiesOfClass(com.bromax.pestilence.entity.boss.RatKing.class, new AABB(pos).inflate(64)).isEmpty()) return;
        summon(level, pos, ModRegistry.RAT_KING.get());
        net.minecraft.network.chat.Component msg = net.minecraft.network.chat.Component.translatable("message.bromaxpestilence.rat_king")
                .withStyle(net.minecraft.ChatFormatting.DARK_RED);
        level.players().stream().filter(p -> p.blockPosition().distSqr(pos) < 96 * 96).forEach(p -> p.sendSystemMessage(msg));
        level.playSound(null, pos, SoundEvents.SILVERFISH_DEATH, SoundSource.HOSTILE, 3f, 0.4f);
    }

    /** Mature warrens breed worse rats: sewer, bloated and burrowing (and from stage IV, swarms). */
    private static EntityType<? extends PlagueRat> ratFor(ServerLevel level, int stage) {
        if (stage < 2 || level.random.nextFloat() > 0.35f) return ModRegistry.PLAGUE_RAT.get();
        return switch (level.random.nextInt(3)) {
            case 0 -> ModRegistry.SEWER_RAT.get();
            case 1 -> ModRegistry.BLOATED_RAT.get();
            default -> ModRegistry.BURROWER_RAT.get();
        };
    }

    /** From the Heralds phase, mature nests send out their servants and grow pustules. */
    private static void heralds(ServerLevel level, BlockPos pos, NestKind kind, int stage, int timer) {
        if (PestilenceData.phase(level) < Phase.HERALDS) return;
        AABB near = new AABB(pos).inflate(24);
        if (timer % 2400 == 0 && stage >= 3 && kind != NestKind.WARREN
                && level.getEntitiesOfClass(com.bromax.pestilence.entity.herald.Pustule.class, new AABB(pos).inflate(8)).size() < stage - 2) {
            summon(level, pos, ModRegistry.PUSTULE.get());
        }
        if (timer % 1800 != 0 || !level.getEntitiesOfClass(com.bromax.pestilence.entity.herald.HeraldMob.class, near,
                h -> !(h instanceof com.bromax.pestilence.entity.herald.Pustule)).isEmpty()) return;
        switch (kind) {
            case PIT -> { if (stage >= 3) summon(level, pos, level.random.nextBoolean() ? ModRegistry.GRAVEDIGGER.get() : ModRegistry.BONE_COLLECTOR.get()); }
            case WELL -> { if (stage >= 2) summon(level, pos, level.random.nextBoolean() ? ModRegistry.MIASMA_WRAITH.get() : ModRegistry.CENSER_MONK.get()); }
            case WARREN -> {
                if (stage >= 3) summon(level, pos, ModRegistry.RATCATCHER.get());
                if (stage >= 4 && PestilenceData.phase(level) >= Phase.BLACK_DEATH) summon(level, pos, ModRegistry.RAT_SWARM.get());
            }
        }
    }

    private static void summon(ServerLevel level, BlockPos pos, EntityType<? extends net.minecraft.world.entity.Mob> type) {
        net.minecraft.world.entity.Mob mob = type.create(level);
        if (mob == null) return;
        BlockPos at = pos.relative(net.minecraft.core.Direction.Plane.HORIZONTAL.getRandomDirection(level.random));
        mob.moveTo(at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.SPAWNER, null);
        level.addFreshEntity(mob);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) Tracker.addNest(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) Tracker.removeNest(level, worldPosition);
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide) Tracker.removeNest(level, worldPosition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        growth = tag.getInt("growth");
        dayTimer = tag.getInt("dayTimer");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("growth", growth);
        tag.putInt("dayTimer", dayTimer);
    }
}
