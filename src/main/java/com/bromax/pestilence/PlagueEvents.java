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

import com.bromax.pestilence.disease.Disease;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

public final class PlagueEvents {
    private static final TagKey<Item> RAW_MEAT = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "foods/raw_meat"));
    private static final TagKey<Biome> SWAMPS = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", "is_swamp"));

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

    /** Coughing: only the contagious diseases, and never through a mask or censer smoke. */
    private static void spread(LivingEntity source) {
        Disease disease = Plague.disease(source);
        if (!disease.contagious) return;
        double radius = PestConfig.CONTAGION_RADIUS.get();
        double chance = PestConfig.CONTAGION_CHANCE.get() * disease.contagion;
        for (LivingEntity other : source.level().getEntitiesOfClass(LivingEntity.class,
                source.getBoundingBox().inflate(radius), o -> o != source && Plague.canCatch(o))) {
            if (!Plague.airProtected(other) && source.getRandom().nextDouble() < chance) Plague.infect(other, disease);
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
        // Bad food and bad water: dysentery.
        double chance = 0;
        if (event.getItem().is(Items.ROTTEN_FLESH)) chance = PestConfig.ROTTEN_FLESH_CHANCE.get();
        else if (event.getItem().is(RAW_MEAT)) chance = PestConfig.RAW_MEAT_CHANCE.get();
        else if (event.getItem().is(Items.POTION) && event.getItem().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER)
                && Tracker.nestWithin(entity.level(), entity.blockPosition(), PestConfig.BLIGHT_HEAL_RANGE.get())) {
            chance = PestConfig.FOUL_WATER_CHANCE.get();
        }
        if (chance > 0 && entity.getRandom().nextDouble() < chance) Plague.infect(entity, Disease.DYSENTERY);
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
        if (!animals.isEmpty()) Plague.infect(animals.get(player.getRandom().nextInt(animals.size())), Disease.BUBONIC);
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

    /** Filling a bottle in swamp water sometimes brings up a jar of leeches instead. */
    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!event.getItemStack().is(Items.GLASS_BOTTLE) || player.level().isClientSide) return;
        HitResult hit = player.pick(player.blockInteractionRange(), 0f, true);
        if (!(hit instanceof BlockHitResult bhr) || !player.level().getFluidState(bhr.getBlockPos()).is(FluidTags.WATER)) return;
        if (!player.level().getBiome(bhr.getBlockPos()).is(SWAMPS) || player.getRandom().nextDouble() >= PestConfig.LEECH_CHANCE.get()) return;
        event.getItemStack().consume(1, player);
        ItemStack jar = new ItemStack(ModRegistry.LEECH_JAR.get());
        if (!player.getInventory().add(jar)) player.drop(jar, false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1f, 0.7f);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
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
