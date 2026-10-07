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

import com.bromax.pestilence.block.PlagueCrossBlock;
import com.bromax.pestilence.world.VillageOutbreaks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

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
        MobEffectInstance blight = entity.getEffect(ModRegistry.BLACKBLIGHT);
        if (blight != null) {
            if (blight.getDuration() <= 1) {
                com.bromax.pestilence.disease.Blackblight.advance(entity);
                return;
            }
            if (entity.tickCount % 20 == 0 && entity.level() instanceof ServerLevel level) com.bromax.pestilence.disease.Blackblight.spread(level, entity);
        }
        MobEffectInstance plague = entity.getEffect(ModRegistry.PLAGUE);
        if (plague == null) return;

        int stage = plague.getAmplifier();
        if (plague.getDuration() <= 1) {
            Plague.advance(entity, stage);
            return;
        }
        if (entity instanceof Villager villager && entity.level() instanceof ServerLevel level) {
            tickSickVillager(level, villager, plague, stage);
            return;
        }
        if (stage >= 1 && entity.tickCount % PestConfig.CONTAGION_INTERVAL.get() == 0) spread(entity);
        if (stage >= Plague.MAX_STAGE && Plague.isHostAnimal(entity)
                && plague.getDuration() % PestConfig.MOB_PLAGUE_DAMAGE_INTERVAL.get() == 0
                && PestilenceData.phase(entity.level()) >= Phase.MURRAIN) {
            entity.hurt(ModDamage.plague(entity.level()), 1f);
        }
    }

    /**
     * Sick villagers: they report the outbreak to their village, keep indoors and stop spreading
     * it when quarantined by a plague cross, and from the Outbreak phase they waste away in the
     * worst stage unless someone treats them.
     */
    private static void tickSickVillager(ServerLevel level, Villager villager, MobEffectInstance plague, int stage) {
        if (villager.tickCount % 200 == 0) VillageOutbreaks.get(level.getServer()).noteSick(level, villager);
        if (villager.tickCount % PestConfig.CONTAGION_INTERVAL.get() == 0) {
            if (PlagueCrossBlock.quarantined(level, villager.blockPosition())) {
                villager.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PestConfig.CONTAGION_INTERVAL.get() + 20, 3, true, false, false));
            } else if (stage >= 1) {
                spread(villager);
            }
        }
        if (stage >= Plague.MAX_STAGE && PestilenceData.phase(level) >= Phase.OUTBREAK
                && plague.getDuration() % PestConfig.VILLAGER_PLAGUE_DAMAGE_INTERVAL.get() == 0) {
            villager.hurt(ModDamage.plague(level), 1f);
        }
    }

    /** Village physicians (healthy ones) treat a sick neighbour every half minute. */
    @SubscribeEvent
    public static void onPhysicianTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager physician) || physician.tickCount % 600 != 0
                || !(physician.level() instanceof ServerLevel level) || Plague.isSick(physician)
                || physician.getVillagerData().getProfession() != ModRegistry.PHYSICIAN.get()) return;
        level.getEntitiesOfClass(Villager.class, physician.getBoundingBox().inflate(10), v -> v != physician && Plague.isSick(v))
                .stream().findFirst().ifPresent(patient -> {
                    Plague.cure(patient);
                    VillageOutbreaks.get(level.getServer()).noteCure(level, patient, null);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, patient.getX(), patient.getEyeY(), patient.getZ(), 8, 0.3, 0.3, 0.3, 0);
                    physician.playSound(SoundEvents.VILLAGER_WORK_CLERIC, 1f, 1f);
                });
    }

    /** Coughing: only the contagious diseases, and never through a mask or censer smoke. */
    private static void spread(LivingEntity source) {
        Disease disease = Plague.disease(source);
        if (!disease.contagious) return;
        double radius = PestConfig.CONTAGION_RADIUS.get();
        double chance = PestConfig.contagionChance() * disease.contagion;
        for (LivingEntity other : source.level().getEntitiesOfClass(LivingEntity.class,
                source.getBoundingBox().inflate(radius), o -> o != source && Plague.canCatch(o))) {
            if (source.getRandom().nextDouble() < chance * Plague.airExposure(other)) Plague.infect(other, disease);
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
        // A fully consecrated cleaver mends its bearer with each of the plague's creatures it fells.
        if (event.getSource().getEntity() instanceof Player killer && isPlagueCreature(entity)
                && killer.getMainHandItem().is(ModRegistry.CONSECRATED_CLEAVER.get())
                && com.bromax.pestilence.disease.Consecration.level(killer.getMainHandItem()) >= 5) {
            killer.heal(2f);
        }
        // From the Heralds on, a dead rat sometimes leaves its fleas behind.
        if (entity instanceof PlagueRat && PestilenceData.phase(level) >= Phase.HERALDS && entity.getRandom().nextFloat() < 0.2f) {
            var fleas = ModRegistry.FLEA_CLOUD.get().create(level);
            if (fleas != null) {
                fleas.moveTo(entity.getX(), entity.getY() + 0.3, entity.getZ());
                level.addFreshEntity(fleas);
            }
        }
        if (!entity.hasEffect(ModRegistry.PLAGUE)) return;
        if (entity instanceof Villager villager) {
            Plague.addPoints(entity, PestConfig.POINTS_VILLAGER_DEATH.get());
            VillageOutbreaks.get(level.getServer()).noteDeath(level, villager);
            if (PestilenceData.phase(level) >= Phase.OUTBREAK) Corpse.spawnFrom(level, entity);
            return;
        }
        if (Plague.isPerson(entity)) return;
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

    /** Phase gates for natural spawns: each plague mob has a phase it appears from; few rats while dormant. */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION) return;
        int phase = PestilenceData.phase(event.getEntity().level());
        if (event.getEntity() instanceof com.bromax.pestilence.entity.PhaseGated
                && event.getEntity().getRandom().nextDouble() >= PestConfig.naturalSpawnRate()) {
            event.setSpawnCancelled(true);
            return;
        }
        if (event.getEntity() instanceof com.bromax.pestilence.entity.PhaseGated gated && phase < gated.minPhase()) {
            event.setSpawnCancelled(true);
        } else if (event.getEntity().getType() == ModRegistry.PLAGUE_RAT.get() && phase < Phase.FULL_RATS && event.getEntity().getRandom().nextBoolean()) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        PestilenceData.get(event.getServer()).tickDay(event.getServer());
        if (event.getServer().getTickCount() % 1200 == 0) {
            VillageOutbreaks.get(event.getServer()).tick(event.getServer().overworld());
            summonThePaleRider(event.getServer().overworld());
        }
    }

    /** In the last phase, at nightfall, the Pale Rider comes for a player in the Overworld (one at a time). */
    private static void summonThePaleRider(ServerLevel level) {
        PestilenceData data = PestilenceData.get(level.getServer());
        if (data.phase() < Phase.MAX || !level.isNight() || level.players().isEmpty()) return;
        if (data.rider(level.getGameTime()) != null) return;
        ServerPlayer prey = level.players().get(level.random.nextInt(level.players().size()));
        for (int attempt = 0; attempt < 10; attempt++) {
            float angle = level.random.nextFloat() * Mth.TWO_PI;
            int x = Mth.floor(prey.getX() + Mth.cos(angle) * 40), z = Mth.floor(prey.getZ() + Mth.sin(angle) * 40);
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!level.isLoaded(pos) || !level.getBlockState(pos.below()).isSolid()) continue;
            var rider = ModRegistry.PALE_RIDER.get().create(level);
            if (rider == null) return;
            rider.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
            rider.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            rider.setTarget(prey);
            level.addFreshEntity(rider);
            data.setRider(rider.getUUID(), level.getGameTime());
            Component msg = Component.translatable("message." + Pestilence.MODID + ".pale_rider").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD);
            for (ServerPlayer p : level.players()) {
                p.sendSystemMessage(msg);
                p.playNotifySound(SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1f, 0.3f);
            }
            return;
        }
    }

    /** The Pale Rider's arrows carry the Rot (the Doctor's Garb turns them as it does any contact). */
    @SubscribeEvent
    public static void onDamaged(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow
                && arrow.getPersistentData().getBoolean(com.bromax.pestilence.entity.boss.PaleRider.ROT_ARROW)) {
            Plague.tryContactInfect(event.getEntity(), 0.6, Disease.ROT);
        }
    }

    /** Villagers too sick to stand behind a counter won't trade. */
    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager) || event.getItemStack().getItem() instanceof com.bromax.pestilence.item.RemedyItem
                || event.getItemStack().is(ModRegistry.PHYSICIANS_KIT.get()) || event.getItemStack().getItem() instanceof com.bromax.pestilence.item.TreatmentItem) return;
        MobEffectInstance plague = villager.getEffect(ModRegistry.PLAGUE);
        if (plague == null || plague.getAmplifier() < 1) return;
        if (!event.getLevel().isClientSide) {
            villager.playSound(SoundEvents.VILLAGER_NO, 1f, 0.8f);
            event.getEntity().displayClientMessage(Component.translatable("message." + Pestilence.MODID + ".too_sick_to_trade")
                    .withStyle(ChatFormatting.GRAY), true);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }

    /** The village physician's trades: herbs bought, remedies and garb sold. */
    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != ModRegistry.PHYSICIAN.get()) return;
        var trades = event.getTrades();
        trades.computeIfAbsent(1, k -> new java.util.ArrayList<>()).add(buy(ModRegistry.HERB_ITEMS.get("garlic").get(), 12, 16, 2));
        trades.computeIfAbsent(1, k -> new java.util.ArrayList<>()).add(sell(ModRegistry.FOUR_THIEVES_VINEGAR.get(), 1, 3, 8, 2));
        trades.computeIfAbsent(1, k -> new java.util.ArrayList<>()).add(sell(ModRegistry.PLAGUE_POULTICE.get(), 2, 2, 8, 2));
        trades.computeIfAbsent(2, k -> new java.util.ArrayList<>()).add(buy(ModRegistry.HERB_ITEMS.get("sage").get(), 12, 16, 10));
        trades.computeIfAbsent(2, k -> new java.util.ArrayList<>()).add(buy(ModRegistry.RAT_TAIL.get(), 4, 16, 10));
        trades.computeIfAbsent(2, k -> new java.util.ArrayList<>()).add(sell(ModRegistry.PHYSICIANS_KIT.get(), 1, 4, 4, 10));
        trades.computeIfAbsent(2, k -> new java.util.ArrayList<>()).add(sell(ModRegistry.QUICKLIME.get(), 4, 1, 12, 5));
        trades.computeIfAbsent(3, k -> new java.util.ArrayList<>()).add(buy(ModRegistry.HERB_ITEMS.get("wormwood").get(), 10, 16, 20));
        trades.computeIfAbsent(3, k -> new java.util.ArrayList<>()).add(sell(ModRegistry.WORMWOOD_TINCTURE.get(), 1, 4, 8, 15));
        trades.computeIfAbsent(3, k -> new java.util.ArrayList<>()).add(sell(ModRegistry.HERBAL_BROTH.get(), 1, 3, 8, 15));
        trades.computeIfAbsent(4, k -> new java.util.ArrayList<>()).add(sell(ModRegistry.DOCTORS_COAT.get(), 1, 10, 3, 20));
        trades.computeIfAbsent(4, k -> new java.util.ArrayList<>()).add(sell(ModRegistry.DOCTORS_TROUSERS.get(), 1, 8, 3, 20));
        trades.computeIfAbsent(4, k -> new java.util.ArrayList<>()).add(sell(ModRegistry.DOCTORS_BOOTS.get(), 1, 6, 3, 20));
        trades.computeIfAbsent(4, k -> new java.util.ArrayList<>()).add(sell(ModRegistry.CENSER.get(), 1, 12, 3, 20));
        trades.computeIfAbsent(5, k -> new java.util.ArrayList<>()).add(sell(ModRegistry.THERIAC.get(), 1, 10, 4, 30));
        trades.computeIfAbsent(5, k -> new java.util.ArrayList<>()).add(sell(ModRegistry.PLAGUE_MASK.get(), 1, 12, 3, 30));
    }

    private static VillagerTrades.ItemListing buy(Item item, int count, int uses, int xp) {
        return (trader, random) -> new MerchantOffer(new ItemCost(item, count), new ItemStack(Items.EMERALD), uses, xp, 0.05f);
    }

    private static VillagerTrades.ItemListing sell(Item item, int count, int emeralds, int uses, int xp) {
        return (trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), new ItemStack(item, count), uses, xp, 0.05f);
    }

    @SubscribeEvent
    public static void onBlockBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getPlayer() instanceof ServerPlayer player) {
            com.bromax.pestilence.world.Farming.harvested(level, player, event.getState());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        Player player = event.getEntity();
        if (player.isSpectator() || player.tickCount == 0) return;
        if (player.tickCount % PestConfig.DOCTOR_CHECK_INTERVAL.get() == 0) rollDoctorVisit(level, player);
        if (player.tickCount % PestConfig.OUTBREAK_INTERVAL.get() == 0) rollOutbreak(level, player);
        if (player.tickCount % PestConfig.NEST_SEED_INTERVAL.get() == 0 && player instanceof ServerPlayer sp) NestSeeding.roll(level, sp);
        if (player.tickCount % 600 == 0 && player instanceof ServerPlayer sp) rollSweating(level, sp);
        com.bromax.pestilence.item.CenserItem.tickBelt(level, player);
        if (player.tickCount % 10 == 0 && player instanceof ServerPlayer sp && com.bromax.pestilence.compat.Compat.wearing(sp, ModRegistry.PHYSICIANS_MONOCLE.get(), false)) {
            diagnose(level, sp);
        }
        if (player.tickCount % PestConfig.FARM_INTERVAL.get() == 0 && player instanceof ServerPlayer sp) com.bromax.pestilence.world.Farming.roll(level, sp);
        if (player.tickCount % 100 == 0) {
            VillageOutbreaks villages = VillageOutbreaks.get(level.getServer());
            VillageOutbreaks.centre(level, player.blockPosition(), 64).filter(c -> !villages.known(c)).ifPresent(c -> villages.discover(level, c));
        }
    }

    /** The Physician's Monocle: motes over every sick creature in sight, seen by the wearer alone, redder as the sickness worsens. */
    private static void diagnose(ServerLevel level, ServerPlayer player) {
        int[] tints = {0xd8c840, 0xd87830, 0xb02020};
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16), Plague::isSick)) {
            if (e == player || !player.hasLineOfSight(e)) continue;
            var effect = e.getEffect(ModRegistry.PLAGUE);
            int stage = effect == null ? 0 : Math.min(2, effect.getAmplifier());
            var mote = net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xff000000 | tints[stage]);
            level.sendParticles(player, mote, true, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), stage + 1, 0.2, 0.1, 0.2, 0);
        }
    }

    /**
     * Sweating Sickness: with Cold Sweat, an overheated body (over 50); without it, sprinting
     * under the open sky by day in hot country. Comes in any phase: it is the season, not the plague.
     */
    private static void rollSweating(ServerLevel level, ServerPlayer player) {
        double body = com.bromax.pestilence.compat.Compat.bodyTemperature(player);
        boolean hot = Double.isNaN(body)
                ? level.isDay() && player.isSprinting() && level.canSeeSky(player.blockPosition()) && level.getBiome(player.blockPosition()).value().getBaseTemperature() > 1.0f
                : body > 50;
        if (hot && player.getRandom().nextDouble() < PestConfig.SWEATING_CHANCE.get()) Plague.infect(player, Disease.SWEATING);
    }

    /** A full night's sleep breaks Sweating Sickness. */
    @SubscribeEvent
    public static void onWake(net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide && Plague.isSick(player) && Plague.disease(player) == Disease.SWEATING && player.level().isDay()) {
            Plague.cure(player);
        }
    }

    /** The Consecrated Cleaver: a fifth more damage per level against the plague's creatures; fire from level 3. */
    @SubscribeEvent
    public static void onIncomingDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player) return;
        ItemStack weapon = player.getMainHandItem();
        if (weapon.is(ModRegistry.CENSER_FLAIL.get()) && event.getEntity() instanceof com.bromax.pestilence.entity.herald.MiasmaWraith) {
            event.setAmount(event.getAmount() * 2);
            return;
        }
        if (!weapon.is(ModRegistry.CONSECRATED_CLEAVER.get()) || !isPlagueCreature(event.getEntity())) return;
        int level = com.bromax.pestilence.disease.Consecration.level(weapon);
        event.setAmount(event.getAmount() * (1 + 0.2f * level));
        if (level >= 3) event.getEntity().igniteForSeconds(3);
    }

    private static boolean isPlagueCreature(LivingEntity e) {
        return e instanceof AfflictedMob || e instanceof PlagueRat || e instanceof com.bromax.pestilence.entity.vermin.RatSwarm
                || e instanceof com.bromax.pestilence.entity.vermin.CarrionCrow;
    }

    /** The Plaguebane Amulet keeps off the Heralds' lesser afflictions too. */
    @SubscribeEvent
    public static void onEffectApplicable(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable event) {
        var effect = event.getEffectInstance().getEffect();
        if ((effect.is(ModRegistry.DREAD) || effect.is(ModRegistry.DELIRIUM) || effect.is(ModRegistry.ROTTING)) && Plague.plaguebane(event.getEntity())) {
            event.setResult(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** From the Murrain on, animals near players fall sick on their own; the herd does the rest. */
    private static void rollOutbreak(ServerLevel level, Player player) {
        int phase = PestilenceData.phase(level);
        if (phase < Phase.MURRAIN) return;
        if (player.getRandom().nextDouble() >= PestConfig.OUTBREAK_CHANCE_PER_PHASE.get() * (phase - 1)) return;
        boolean villages = phase >= Phase.OUTBREAK;
        List<LivingEntity> hosts = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(48),
                e -> (Plague.isHostAnimal(e) || villages && e instanceof Villager) && Plague.canCatch(e) && e.distanceToSqr(player) > 16 * 16);
        if (hosts.isEmpty()) return;
        LivingEntity host = hosts.get(player.getRandom().nextInt(hosts.size()));
        Plague.infect(host, host instanceof Villager && player.getRandom().nextInt(3) == 0 ? Disease.PNEUMONIC : Disease.BUBONIC);
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
