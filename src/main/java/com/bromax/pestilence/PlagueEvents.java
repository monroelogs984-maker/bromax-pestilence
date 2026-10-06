package com.bromax.pestilence;

import com.bromax.pestilence.entity.Corpse;
import com.bromax.pestilence.entity.PlagueDoctor;
import com.bromax.pestilence.entity.PlagueRat;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.phase.Phase;
import com.bromax.pestilence.phase.PestilenceData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import com.bromax.pestilence.world.NestSeeding;
import com.bromax.pestilence.world.Tracker;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class PlagueEvents {
    private PlagueEvents() {}

    /**
     * Stage progression, contagion, and animals wasting away. Runs before the entity ticks its
     * effects, so a stage about to run out is replaced here instead of expiring.
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide) return;
        MobEffectInstance plague = entity.getEffect(ModRegistry.PLAGUE);
        if (plague == null) return;

        int stage = plague.getAmplifier();
        if (plague.getDuration() <= 1) {
            Plague.advance(entity, stage);
            return;
        }
        if (stage >= 1 && entity.tickCount % PestConfig.CONTAGION_INTERVAL.get() == 0) spread(entity);
        if (stage >= Plague.MAX_STAGE && Plague.isHostAnimal(entity)
                && plague.getDuration() % PestConfig.MOB_PLAGUE_DAMAGE_INTERVAL.get() == 0
                && PestilenceData.phase(entity.level()) >= Phase.MURRAIN) {
            entity.hurt(ModDamage.plague(entity.level()), 1f);
        }
    }

    private static void spread(LivingEntity source) {
        double radius = PestConfig.CONTAGION_RADIUS.get();
        double chance = PestConfig.CONTAGION_CHANCE.get();
        for (LivingEntity other : source.level().getEntitiesOfClass(LivingEntity.class,
                source.getBoundingBox().inflate(radius), o -> o != source && Plague.canCatch(o))) {
            if (!Plague.wearsMask(other) && source.getRandom().nextDouble() < chance) Plague.infect(other);
        }
    }

    /** Plague deaths feed the Pestilence, and animals leave corpses that will rise. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (entity instanceof AfflictedMob && event.getSource().getEntity() instanceof Player) {
            Plague.addPoints(entity, PestConfig.POINTS_AFFLICTED_KILL.get());
        }
        if (!entity.hasEffect(ModRegistry.PLAGUE) || Plague.isPerson(entity)) return;
        Plague.addPoints(entity, PestConfig.POINTS_PLAGUE_DEATH.get());
        if (Plague.isHostAnimal(entity) && PestilenceData.phase(level) >= Phase.MURRAIN) {
            Corpse.spawnFrom(level, entity);
        }
    }

    @SubscribeEvent
    public static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Player)) return;
        if (event.getItem().is(Items.ROTTEN_FLESH) && entity.getRandom().nextDouble() < PestConfig.ROTTEN_FLESH_CHANCE.get()) {
            Plague.infect(entity);
        }
    }

    /** Phase gates for natural spawns: few rats while dormant, no Afflicted before Rat Season. */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION) return;
        int phase = PestilenceData.phase(event.getEntity().level());
        if (event.getEntity() instanceof AfflictedMob && phase < Phase.RAT_SEASON) {
            event.setSpawnCancelled(true);
        } else if (event.getEntity() instanceof PlagueRat && phase < Phase.FULL_RATS && event.getEntity().getRandom().nextBoolean()) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        PestilenceData.get(event.getServer()).tickDay(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        Player player = event.getEntity();
        if (player.isSpectator() || player.tickCount == 0) return;
        if (player.tickCount % PestConfig.DOCTOR_CHECK_INTERVAL.get() == 0) rollDoctorVisit(level, player);
        if (player.tickCount % PestConfig.OUTBREAK_INTERVAL.get() == 0) rollOutbreak(level, player);
        if (player.tickCount % PestConfig.NEST_SEED_INTERVAL.get() == 0 && player instanceof ServerPlayer sp) NestSeeding.roll(level, sp);
    }

    /** From the Murrain on, animals near players fall sick on their own; the herd does the rest. */
    private static void rollOutbreak(ServerLevel level, Player player) {
        int phase = PestilenceData.phase(level);
        if (phase < Phase.MURRAIN) return;
        if (player.getRandom().nextDouble() >= PestConfig.OUTBREAK_CHANCE_PER_PHASE.get() * (phase - 1)) return;
        List<LivingEntity> animals = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(48),
                e -> Plague.isHostAnimal(e) && Plague.canCatch(e) && e.distanceToSqr(player) > 16 * 16);
        if (!animals.isEmpty()) Plague.infect(animals.get(player.getRandom().nextInt(animals.size())));
    }

    /** Plague Doctor visits: a periodic roll per Overworld player, much likelier while infected. */
    private static void rollDoctorVisit(ServerLevel level, Player player) {
        boolean infected = player.hasEffect(ModRegistry.PLAGUE);
        double chance = infected ? PestConfig.DOCTOR_CHANCE_INFECTED.get() : PestConfig.DOCTOR_CHANCE.get();
        if (player.getRandom().nextDouble() < chance && trySpawnDoctor(level, player) && infected) {
            player.displayClientMessage(Component.translatable("message." + Pestilence.MODID + ".doctor_near")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    private static boolean trySpawnDoctor(ServerLevel level, Player player) {
        if (!level.getEntitiesOfClass(PlagueDoctor.class, player.getBoundingBox().inflate(96)).isEmpty()) return false;
        EntityType<PlagueDoctor> type = ModRegistry.PLAGUE_DOCTOR.get();
        for (int attempt = 0; attempt < 10; attempt++) {
            float angle = player.getRandom().nextFloat() * Mth.TWO_PI;
            double dist = 16 + player.getRandom().nextDouble() * 16;
            int x = Mth.floor(player.getX() + Mth.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Mth.sin(angle) * dist);
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!SpawnPlacements.isSpawnPositionOk(type, level, pos)) continue;
            PlagueDoctor doctor = type.create(level);
            if (doctor == null) return false;
            doctor.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getRandom().nextFloat() * 360f, 0f);
            doctor.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            doctor.setDespawnDelay(PestConfig.DOCTOR_STAY_TICKS.get());
            level.addFreshEntity(doctor);
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Tracker.clear();
        NestSeeding.clear();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        PestilenceCommand.register(event.getDispatcher());
    }
}
