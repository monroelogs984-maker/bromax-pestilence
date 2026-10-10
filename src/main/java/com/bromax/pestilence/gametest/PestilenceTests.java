package com.bromax.pestilence.gametest;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.block.NestBlock;
import com.bromax.pestilence.disease.Blackblight;
import com.bromax.pestilence.disease.Consecration;
import com.bromax.pestilence.disease.Disease;
import com.bromax.pestilence.entity.Corpse;
import com.bromax.pestilence.entity.MiasmaCloud;
import com.bromax.pestilence.entity.PlagueRat;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.entity.form.FormMob;
import com.bromax.pestilence.entity.form.MiasmaClot;
import com.bromax.pestilence.phase.PestilenceData;
import com.bromax.pestilence.world.Blight;
import com.bromax.pestilence.world.Tracker;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Headless checks of every Pestilence system: {@code ./gradlew runGameTestServer}. Each test sits
 * in its own batch, because the Pestilence phase is world-wide and tests must not share it.
 * Not shipped (the jar task excludes this package and the arena structure).
 */
@GameTestHolder(Pestilence.MODID)
@PrefixGameTestTemplate(false)
public final class PestilenceTests {
    private static final String ARENA = "gametest/arena";
    private static final Map<ModConfigSpec.ConfigValue<?>, Object> DEFAULTS = new HashMap<>();

    private PestilenceTests() {}

    // ---- helpers -------------------------------------------------------------------------------

    /** Every test starts from the same world state: config defaults, no halt, no Blackblight, the given phase. */
    private static void reset(GameTestHelper h, int phase) {
        DEFAULTS.forEach((value, original) -> set(value, original));
        h.killAllEntities();
        MinecraftServer server = h.getLevel().getServer();
        PestilenceData data = PestilenceData.get(server);
        if (data.blackblight()) data.cleanse(server);
        data.resume();
        data.set(server, PestilenceData.threshold(phase));
    }

    @SuppressWarnings("unchecked")
    private static <T> void set(ModConfigSpec.ConfigValue<?> value, Object v) {
        ((ModConfigSpec.ConfigValue<T>) value).set((T) v);
    }

    /** Changes a config value for this test; reset() puts it back for the next one. */
    private static <T> void tweak(ModConfigSpec.ConfigValue<T> value, T v) {
        DEFAULTS.putIfAbsent(value, value.get());
        value.set(v);
    }

    private static PestilenceData data(GameTestHelper h) {
        return PestilenceData.get(h.getLevel().getServer());
    }

    private static <T extends Entity> T spawn(GameTestHelper h, EntityType<T> type, int x, int z) {
        return h.spawn(type, new BlockPos(x, 1, z));
    }

    /**
     * A survival mock player in the level. GameTestHelper's own mock player is hard-wired to
     * creative, which every plague rule (rightly) ignores.
     */
    private static ServerPlayer player(GameTestHelper h, int x, int z) {
        MinecraftServer server = h.getLevel().getServer();
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "pest-mock"), false);
        ServerPlayer p = new ServerPlayer(server, h.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
        };
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        Vec3 at = h.absoluteVec(new Vec3(x + 0.5, 1, z + 0.5));
        p.moveTo(at.x, at.y, at.z);
        return p;
    }

    private static <T extends Entity> List<T> all(GameTestHelper h, Class<T> type) {
        return h.getLevel().getEntitiesOfClass(type, h.getBounds().inflate(2));
    }

    /** Removes any nest the test placed and its mobs, so nothing keeps ticking into later tests. */
    private static void clean(GameTestHelper h) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            if (h.getBlockState(new BlockPos(x, 1, z)).getBlock() instanceof NestBlock) h.setBlock(new BlockPos(x, 1, z), Blocks.AIR);
        }
        h.killAllEntities();
    }

    /** succeedWhen, but cleans up the moment the condition holds. */
    private static void succeedThenClean(GameTestHelper h, Runnable check) {
        h.onEachTick(() -> {
            try {
                check.run();
            } catch (net.minecraft.gametest.framework.GameTestAssertException notYet) {
                return;
            }
            clean(h);
            h.succeed();
        });
    }

    private static void consume(GameTestHelper h, LivingEntity who, net.minecraft.world.item.Item item) {
        ItemStack stack = new ItemStack(item);
        item.finishUsingItem(stack, h.getLevel(), who);
    }

    // ---- phases --------------------------------------------------------------------------------

    @GameTest(template = ARENA, batch = "phase_thresholds")
    public static void phaseThresholds(GameTestHelper h) {
        reset(h, 0);
        for (int phase = 0; phase <= 8; phase++) {
            data(h).set(h.getLevel().getServer(), PestilenceData.threshold(phase));
            h.assertTrue(data(h).phase() == phase, "points at threshold(" + phase + ") gave phase " + data(h).phase());
        }
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "halt")
    public static void haltCapsThePhase(GameTestHelper h) {
        reset(h, 0);
        MinecraftServer server = h.getLevel().getServer();
        data(h).halt(server, 3);
        data(h).set(server, PestilenceData.threshold(6));
        h.assertTrue(data(h).phase() == 3, "halted at 3 but phase is " + data(h).phase());
        data(h).resume();
        data(h).set(server, PestilenceData.threshold(6));
        h.assertTrue(data(h).phase() == 6, "resumed but phase is " + data(h).phase());
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "command")
    public static void phaseCommand(GameTestHelper h) {
        reset(h, 0);
        MinecraftServer server = h.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "pestilence phase 5");
        h.assertTrue(data(h).phase() == 5, "/pestilence phase 5 left phase " + data(h).phase());
        h.succeed();
    }

    // ---- disease -------------------------------------------------------------------------------

    @GameTest(template = ARENA, batch = "infect_cure")
    public static void infectAndCure(GameTestHelper h) {
        reset(h, 0);
        ServerPlayer p = player(h, 4, 4);
        h.assertTrue(Plague.infect(p, Disease.BUBONIC), "player did not catch bubonic plague");
        h.assertTrue(Plague.isSick(p) && Plague.disease(p) == Disease.BUBONIC, "player not sick with bubonic");
        consume(h, p, ModRegistry.PLAGUE_POULTICE.get());
        h.assertFalse(Plague.isSick(p), "poultice did not cure bubonic");
        h.assertTrue(p.hasEffect(ModRegistry.IMMUNITY), "no immunity after the cure");
        h.assertFalse(Plague.infect(p, Disease.BUBONIC), "immune player caught it again");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "wrong_remedy")
    public static void wrongRemedyThenTheriac(GameTestHelper h) {
        reset(h, 0);
        ServerPlayer p = player(h, 4, 4);
        Plague.infect(p, Disease.PNEUMONIC);
        consume(h, p, ModRegistry.PLAGUE_POULTICE.get());
        h.assertTrue(Plague.isSick(p), "the poultice cured pneumonic plague");
        consume(h, p, ModRegistry.FOUR_THIEVES_VINEGAR.get());
        h.assertFalse(Plague.isSick(p), "four thieves vinegar did not cure pneumonic");
        p.removeEffect(ModRegistry.IMMUNITY);
        Plague.infect(p, Disease.DYSENTERY);
        consume(h, p, ModRegistry.THERIAC.get());
        h.assertFalse(Plague.isSick(p), "theriac did not cure dysentery");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "stage_advance", timeoutTicks = 100)
    public static void stageAdvances(GameTestHelper h) {
        reset(h, 0);
        Villager v = spawn(h, EntityType.VILLAGER, 4, 4);
        Plague.force(v, Disease.BUBONIC, 0);
        v.removeEffect(ModRegistry.PLAGUE);
        v.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModRegistry.PLAGUE, 10, 0));
        h.succeedWhen(() -> {
            var e = v.getEffect(ModRegistry.PLAGUE);
            h.assertTrue(e != null && e.getAmplifier() == 1, "villager did not advance to stage 2");
        });
    }

    @GameTest(template = ARENA, batch = "animal_gate")
    public static void animalsCatchFromMurrain(GameTestHelper h) {
        reset(h, 0);
        var cow = spawn(h, EntityType.COW, 4, 4);
        h.assertFalse(Plague.canCatch(cow), "a cow could catch the plague in phase 0");
        data(h).set(h.getLevel().getServer(), PestilenceData.threshold(2));
        h.assertTrue(Plague.canCatch(cow), "a cow could not catch the plague in the Murrain");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "protection")
    public static void maskGarbAndAmulet(GameTestHelper h) {
        reset(h, 0);
        ServerPlayer p = player(h, 4, 4);
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModRegistry.PLAGUE_MASK.get()));
        h.assertTrue(Plague.airExposure(p) == 0, "the mask did not stop airborne exposure");
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModRegistry.DOCTORS_COAT.get()));
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModRegistry.DOCTORS_TROUSERS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModRegistry.DOCTORS_BOOTS.get()));
        h.assertTrue(Plague.contactExposure(p) < 1e-6, "the full garb left contact exposure " + Plague.contactExposure(p));
        ServerPlayer q = player(h, 8, 8);
        q.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModRegistry.PLAGUEBANE_AMULET.get()));
        h.assertFalse(Plague.canCatch(q), "the Plaguebane Amulet (offhand) did not protect");
        h.succeed();
    }

    // ---- corpses -------------------------------------------------------------------------------

    @GameTest(template = ARENA, batch = "corpse_from_death", timeoutTicks = 40)
    public static void sickCowLeavesCorpse(GameTestHelper h) {
        reset(h, 2);
        var cow = spawn(h, EntityType.COW, 6, 6);
        Plague.force(cow, Disease.BUBONIC, 2);
        cow.hurt(h.getLevel().damageSources().magic(), 1000f);
        h.succeedWhen(() -> h.assertTrue(!all(h, Corpse.class).isEmpty(), "a plague-dead cow left no corpse"));
    }

    @GameTest(template = ARENA, batch = "corpse_rises", timeoutTicks = 200)
    public static void corpseRises(GameTestHelper h) {
        reset(h, 2);
        BlockPos at = h.absolutePos(new BlockPos(8, 1, 8));
        Corpse.place(h.getLevel(), EntityType.COW, PestConfig.CORPSE_RISE_TICKS.get() - 60, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f);
        h.succeedWhen(() -> h.assertTrue(!all(h, AfflictedMob.class).isEmpty() && all(h, Corpse.class).isEmpty(), "the corpse did not rise"));
    }

    @GameTest(template = ARENA, batch = "corpse_burns", timeoutTicks = 200)
    public static void corpseBurns(GameTestHelper h) {
        reset(h, 3);
        double before = data(h).points();
        BlockPos at = h.absolutePos(new BlockPos(8, 1, 8));
        Corpse.place(h.getLevel(), EntityType.COW, 0, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f);
        all(h, Corpse.class).forEach(Corpse::ignite);
        h.succeedWhen(() -> {
            h.assertTrue(all(h, Corpse.class).isEmpty(), "the corpse is still there");
            h.assertTrue(all(h, AfflictedMob.class).isEmpty(), "a burned corpse rose");
            h.assertTrue(all(h, ItemEntity.class).stream().anyMatch(i -> i.getItem().is(Items.BONE_MEAL)), "no bone meal left by the pyre");
            h.assertTrue(data(h).points() < before, "burning did not lower the Pestilence");
        });
    }

    @GameTest(template = ARENA, batch = "corpse_lime")
    public static void quicklimeDissolvesCorpse(GameTestHelper h) {
        reset(h, 2);
        BlockPos at = h.absolutePos(new BlockPos(8, 1, 8));
        Corpse.place(h.getLevel(), EntityType.PIG, 0, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f);
        ServerPlayer p = player(h, 7, 8);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistry.QUICKLIME.get(), 4));
        all(h, Corpse.class).forEach(c -> c.interact(p, InteractionHand.MAIN_HAND));
        h.succeedWhen(() -> h.assertTrue(all(h, Corpse.class).isEmpty(), "quicklime did not dissolve the corpse"));
    }

    // ---- nests and the Blight ------------------------------------------------------------------

    @GameTest(template = ARENA, batch = "nest_grows", timeoutTicks = 300)
    public static void nestGrows(GameTestHelper h) {
        reset(h, 0);
        tweak(PestConfig.NEST_STAGE_TICKS, 200);
        h.setBlock(new BlockPos(8, 1, 8), ModRegistry.PLAGUE_PIT.get());
        succeedThenClean(h, () -> h.assertTrue(h.getBlockState(new BlockPos(8, 1, 8)).getValue(NestBlock.STAGE) >= 2, "the nest did not grow"));
    }

    @GameTest(template = ARENA, batch = "brazier_stops_nest", timeoutTicks = 300)
    public static void brazierStopsNest(GameTestHelper h) {
        reset(h, 0);
        tweak(PestConfig.NEST_STAGE_TICKS, 200);
        BlockPos brazier = h.absolutePos(new BlockPos(5, 1, 5));
        Tracker.setBrazier(h.getLevel(), brazier, true);
        h.setBlock(new BlockPos(8, 1, 8), ModRegistry.PLAGUE_PIT.get());
        h.runAfterDelay(260, () -> {
            Tracker.setBrazier(h.getLevel(), brazier, false);
            h.assertTrue(h.getBlockState(new BlockPos(8, 1, 8)).getValue(NestBlock.STAGE) == 1, "the nest grew inside a lit brazier's circle");
            clean(h);
            h.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "rat_king", timeoutTicks = 300)
    public static void warrenCrownsRatKing(GameTestHelper h) {
        reset(h, 3);
        tweak(PestConfig.NEST_STAGE_TICKS, 200);
        h.setBlock(new BlockPos(8, 1, 8), ModRegistry.RAT_WARREN.get().defaultBlockState().setValue(NestBlock.STAGE, 3));
        succeedThenClean(h, () -> h.assertTrue(!all(h, com.bromax.pestilence.entity.boss.RatKing.class).isEmpty(), "a stage IV warren crowned no Rat King"));
    }

    @GameTest(template = ARENA, batch = "nest_break")
    public static void breakingNestLowersPestilence(GameTestHelper h) {
        reset(h, 3);
        double before = data(h).points();
        h.setBlock(new BlockPos(8, 1, 8), ModRegistry.MIASMA_WELL.get().defaultBlockState().setValue(NestBlock.STAGE, 2));
        ServerPlayer p = player(h, 7, 8);
        p.gameMode.destroyBlock(h.absolutePos(new BlockPos(8, 1, 8)));
        h.assertTrue(h.getBlockState(new BlockPos(8, 1, 8)).isAir(), "the nest was not broken");
        h.assertTrue(data(h).points() < before, "breaking the nest did not lower the Pestilence");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "blight")
    public static void blightSpreadsAndHeals(GameTestHelper h) {
        reset(h, 4);
        for (int x = 3; x <= 12; x++) for (int z = 3; z <= 12; z++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            // GameTest seals the arena under a barrier ceiling; the Blight works on the open surface
            for (int y = 9; y <= 13; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
        ServerLevel level = h.getLevel();
        Blight.spread(level, h.absolutePos(new BlockPos(8, 1, 8)), 3, 200);
        int blighted = 0;
        for (int x = 3; x <= 12; x++) for (int z = 3; z <= 12; z++) {
            if (Blight.isBlighted(h.getBlockState(new BlockPos(x, 1, z)))) {
                blighted++;
                Blight.heal(level, h.absolutePos(new BlockPos(x, 1, z)));
            }
        }
        h.assertTrue(blighted > 0, "the Blight spread to nothing (surface at centre: y="
                + level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, h.absolutePos(new BlockPos(8, 1, 8)).getX(), h.absolutePos(new BlockPos(8, 1, 8)).getZ())
                + ", grass at y=" + h.absolutePos(new BlockPos(8, 1, 8)).getY() + ")");
        for (int x = 3; x <= 12; x++) for (int z = 3; z <= 12; z++) {
            h.assertFalse(Blight.isBlighted(h.getBlockState(new BlockPos(x, 1, z))), "blighted soil did not heal at " + x + "," + z);
        }
        h.succeed();
    }

    // ---- the Afflicted, villages ---------------------------------------------------------------

    @GameTest(template = ARENA, batch = "bite")
    public static void afflictedBiteInfects(GameTestHelper h) {
        reset(h, 4);
        tweak(PestConfig.AFFLICTED_BITE_CHANCE, 1.0);
        Villager v = spawn(h, EntityType.VILLAGER, 6, 6);
        var cow = spawn(h, ModRegistry.AFFLICTED_COW.get(), 7, 6);
        cow.doHurtTarget(v);
        h.assertTrue(Plague.isSick(v), "an Afflicted bite did not infect the villager");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "cow_burst", timeoutTicks = 40)
    public static void afflictedCowBursts(GameTestHelper h) {
        reset(h, 3);
        var cow = spawn(h, ModRegistry.AFFLICTED_COW.get(), 8, 8);
        cow.hurt(h.getLevel().damageSources().magic(), 1000f);
        h.succeedWhen(() -> h.assertTrue(!all(h, MiasmaCloud.class).isEmpty(), "the Afflicted Cow did not burst into miasma"));
    }

    @GameTest(template = ARENA, batch = "villager_corpse", timeoutTicks = 40)
    public static void plagueDeadVillagerLeavesCorpse(GameTestHelper h) {
        reset(h, 4);
        Villager v = spawn(h, EntityType.VILLAGER, 6, 6);
        Plague.force(v, Disease.BUBONIC, 2);
        v.hurt(h.getLevel().damageSources().magic(), 1000f);
        h.succeedWhen(() -> h.assertTrue(!all(h, Corpse.class).isEmpty(), "a plague-dead villager left no corpse in the Outbreak"));
    }

    @GameTest(template = ARENA, batch = "harvest")
    public static void harvestFeedsPestilence(GameTestHelper h) {
        reset(h, 1);
        double before = data(h).points();
        h.setBlock(new BlockPos(8, 1, 8), Blocks.FARMLAND);
        h.setBlock(new BlockPos(8, 2, 8), ((CropBlock) Blocks.WHEAT).getStateForAge(7));
        ServerPlayer p = player(h, 7, 8);
        p.gameMode.destroyBlock(h.absolutePos(new BlockPos(8, 2, 8)));
        h.assertTrue(data(h).points() > before, "harvesting wheat did not feed the Pestilence");
        h.succeed();
    }

    // ---- every creature ticks ------------------------------------------------------------------

    @GameTest(template = ARENA, batch = "all_mobs", timeoutTicks = 200)
    public static void everyCreatureTicks(GameTestHelper h) {
        reset(h, 8);
        player(h, 8, 8);
        int x = 1;
        for (var holder : ModRegistry.ENTITIES.getEntries()) {
            Entity e = holder.get().create(h.getLevel());
            if (!(e instanceof Mob)) continue;
            e.discard();
            spawn(h, holder.get(), 1 + (x % 14), 1 + (x / 14) * 7);
            x += 2;
        }
        // Any exception while these tick fails the run; afterwards the world must still be ticking.
        h.runAfterDelay(160, h::succeed);
    }

    // ---- bosses --------------------------------------------------------------------------------

    @GameTest(template = ARENA, batch = "rider_death", timeoutTicks = 40)
    public static void paleRiderDeathEndsThePestilence(GameTestHelper h) {
        reset(h, 8);
        var rider = spawn(h, ModRegistry.PALE_RIDER.get(), 8, 8);
        rider.hurt(h.getLevel().damageSources().magic(), Float.MAX_VALUE);
        h.succeedWhen(() -> h.assertTrue(data(h).phase() == 0, "killing the Pale Rider left phase " + data(h).phase()));
    }

    @GameTest(template = ARENA, batch = "king_death", timeoutTicks = 40)
    public static void ratKingDeathDropsTwoPhases(GameTestHelper h) {
        reset(h, 6);
        var king = spawn(h, ModRegistry.RAT_KING.get(), 8, 8);
        king.hurt(h.getLevel().damageSources().magic(), Float.MAX_VALUE);
        h.succeedWhen(() -> h.assertTrue(data(h).phase() == 4, "killing the Rat King at phase 6 left phase " + data(h).phase()));
    }

    // ---- the Blackblight -----------------------------------------------------------------------

    @GameTest(template = ARENA, batch = "blackblight")
    public static void blackblightStagesAndTheriac(GameTestHelper h) {
        reset(h, 8);
        ServerPlayer p = player(h, 4, 4);
        h.assertTrue(Blackblight.infect(p), "the Blackblight did not take");
        Blackblight.setStage(p, 7);
        consume(h, p, ModRegistry.THERIAC.get());
        h.assertTrue(Blackblight.stage(p) == 5, "theriac eased stage 7 to " + Blackblight.stage(p));
        // Straight after joining (spawn protection still up): the tenth stage must kill regardless
        ServerPlayer q = player(h, 10, 10);
        Blackblight.setStage(q, 9);
        Blackblight.advance(q);
        h.assertTrue(q.isDeadOrDying(), "the tenth stage did not kill");
        h.succeed();
    }

    // ---- gear ----------------------------------------------------------------------------------

    @GameTest(template = ARENA, batch = "consecration")
    public static void cleaverConsecrates(GameTestHelper h) {
        reset(h, 0);
        ServerPlayer p = player(h, 4, 4);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistry.CONSECRATED_CLEAVER.get()));
        Consecration.deed(p, Consecration.BOSS);
        ItemStack cleaver = p.getMainHandItem();
        h.assertTrue(Consecration.deeds(cleaver) == Consecration.BOSS, "deeds not recorded: " + Consecration.deeds(cleaver));
        h.assertTrue(Consecration.level(cleaver) >= 1, "ten deeds did not consecrate the cleaver");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "fumigation", timeoutTicks = 100)
    public static void fumigationHurtsWraith(GameTestHelper h) {
        reset(h, 6);
        var wraith = spawn(h, ModRegistry.MIASMA_WRAITH.get(), 8, 8);
        wraith.setNoAi(true);
        float full = wraith.getHealth();
        var cloud = ModRegistry.FUMIGATION_CLOUD.get().create(h.getLevel());
        Vec3 at = h.absoluteVec(new Vec3(8.5, 1.5, 8.5));
        cloud.moveTo(at.x, at.y, at.z);
        h.getLevel().addFreshEntity(cloud);
        h.succeedWhen(() -> h.assertTrue(wraith.getHealth() < full, "the fumigation cloud did not hurt the wraith"));
    }

    // ---- Forms ---------------------------------------------------------------------------------

    @GameTest(template = ARENA, batch = "coalesce", timeoutTicks = 200)
    public static void afflictedCoalesce(GameTestHelper h) {
        reset(h, 4);
        for (int i = 0; i < 3; i++) {
            AfflictedMob cow = spawn(h, ModRegistry.AFFLICTED_COW.get(), 7 + i, 8);
            for (int k = 0; k < 5; k++) cow.noteKill();
        }
        h.succeedWhen(() -> {
            h.assertTrue(all(h, AfflictedMob.class).isEmpty(), "the Afflicted did not melt away");
            h.assertTrue(all(h, MiasmaClot.class).stream().anyMatch(c -> c.mass() == 3), "no clot of mass 3");
        });
    }

    @GameTest(template = ARENA, batch = "no_coalesce_early", timeoutTicks = 140)
    public static void noCoalescenceBeforeOutbreak(GameTestHelper h) {
        reset(h, 3);
        for (int i = 0; i < 3; i++) {
            AfflictedMob cow = spawn(h, ModRegistry.AFFLICTED_COW.get(), 7 + i, 8);
            for (int k = 0; k < 5; k++) cow.noteKill();
        }
        h.runAfterDelay(120, () -> {
            h.assertTrue(all(h, MiasmaClot.class).isEmpty(), "Afflicted coalesced before the Outbreak");
            h.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "corpse_coalesce", timeoutTicks = 200)
    public static void corpsesCoalesce(GameTestHelper h) {
        reset(h, 4);
        tweak(PestConfig.CORPSE_COALESCE_CHANCE, 1.0);
        for (int i = 0; i < 3; i++) {
            BlockPos at = h.absolutePos(new BlockPos(7 + i, 1, 8));
            Corpse.place(h.getLevel(), EntityType.SHEEP, PestConfig.CORPSE_RISE_TICKS.get() - 40, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f);
        }
        h.succeedWhen(() -> {
            h.assertTrue(all(h, Corpse.class).isEmpty(), "the corpses are still lying there");
            h.assertTrue(all(h, AfflictedMob.class).isEmpty(), "a corpse rose instead of coalescing");
            h.assertTrue(!all(h, MiasmaClot.class).isEmpty(), "the corpses made no clot");
        });
    }

    @GameTest(template = ARENA, batch = "clot_absorbs", timeoutTicks = 100)
    public static void clotsMerge(GameTestHelper h) {
        reset(h, 4);
        Vec3 a = h.absoluteVec(new Vec3(8, 1, 8));
        MiasmaClot.create(h.getLevel(), a.x, a.y, a.z, 3);
        MiasmaClot.create(h.getLevel(), a.x + 0.8, a.y, a.z, 2);
        h.succeedWhen(() -> {
            var clots = all(h, MiasmaClot.class);
            h.assertTrue(clots.size() == 1 && clots.get(0).mass() == 5, "clots did not merge into one of mass 5: "
                    + clots.stream().map(c -> c.mass() + "@" + String.format("%.1f,%.1f", c.getX(), c.getZ()) + (c.isAlive() ? "" : "dead")).toList());
        });
    }

    @GameTest(template = ARENA, batch = "clot_condenses", timeoutTicks = 300)
    public static void clotCondensesIntoForm(GameTestHelper h) {
        reset(h, 4);
        Vec3 a = h.absoluteVec(new Vec3(8, 1, 8));
        MiasmaClot.create(h.getLevel(), a.x, a.y, a.z, PestConfig.CLOT_MASS_FOR_FORM.get());
        h.succeedWhen(() -> {
            h.assertTrue(all(h, MiasmaClot.class).isEmpty(), "the clot is still there");
            var forms = all(h, FormMob.class);
            h.assertTrue(forms.size() == 1 && forms.get(0).tier() == FormMob.LESSER, "the clot did not condense into one Lesser Form");
        });
    }

    @GameTest(template = ARENA, batch = "form_grows", timeoutTicks = 120)
    public static void formGrowsFromKills(GameTestHelper h) {
        reset(h, 6);
        FormMob form = spawn(h, ModRegistry.BLACKENED.get(), 8, 8);
        form.setNoAi(false);
        float lesserMax = form.getMaxHealth();
        for (int i = 0; i < PestConfig.GREATER_KILLS.get(); i++) form.noteKill();
        h.succeedWhen(() -> {
            h.assertTrue(form.tier() == FormMob.GREATER, "the Blackened did not grow Greater");
            h.assertTrue(form.getMaxHealth() > lesserMax, "growing did not raise its health");
        });
    }

    @GameTest(template = ARENA, batch = "form_gated", timeoutTicks = 120)
    public static void formGrowthWaitsForPhase(GameTestHelper h) {
        reset(h, 4);
        FormMob form = spawn(h, ModRegistry.BLACKENED.get(), 8, 8);
        for (int i = 0; i < 20; i++) form.noteKill();
        h.runAfterDelay(100, () -> {
            h.assertTrue(form.tier() == FormMob.LESSER, "the Form grew before the Black Death");
            h.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "hardening")
    public static void formsHardenAndFireStrips(GameTestHelper h) {
        reset(h, 4);
        FormMob form = spawn(h, ModRegistry.MOULDHUSK.get(), 8, 8);
        form.setNoAi(true);
        var pig = spawn(h, EntityType.PIG, 10, 8);
        for (int i = 0; i < 10; i++) {
            form.invulnerableTime = 0;
            form.hurt(h.getLevel().damageSources().mobAttack(pig), 0.5f);
        }
        h.assertTrue(form.hardening(FormMob.Harm.MELEE) == PestConfig.ADAPT_MAX_STACKS.get(), "ten blows gave " + form.hardening(FormMob.Harm.MELEE) + " stacks");
        h.assertTrue(form.hardening(FormMob.Harm.PROJECTILE) == 0, "melee hardened it against arrows too");
        float before = form.getHealth();
        form.invulnerableTime = 0;
        form.hurt(h.getLevel().damageSources().mobAttack(pig), 10f);
        float hardenedLoss = before - form.getHealth();
        form.invulnerableTime = 0;
        form.hurt(h.getLevel().damageSources().inFire(), 0.5f);
        h.assertTrue(form.hardening(FormMob.Harm.MELEE) == 0, "fire did not strip the hardening");
        before = form.getHealth();
        form.invulnerableTime = 0;
        form.hurt(h.getLevel().damageSources().mobAttack(pig), 10f);
        float freshLoss = before - form.getHealth();
        h.assertTrue(freshLoss > hardenedLoss * 1.5f, "stripped damage " + freshLoss + " vs hardened " + hardenedLoss);
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "gnawing_sheds", timeoutTicks = 40)
    public static void gnawingMassShedsRats(GameTestHelper h) {
        reset(h, 4);
        FormMob mass = spawn(h, ModRegistry.GNAWING_MASS.get(), 8, 8);
        mass.setNoAi(true);
        mass.hurt(h.getLevel().damageSources().magic(), mass.getMaxHealth() * 0.3f);
        h.succeedWhen(() -> h.assertTrue(all(h, PlagueRat.class).size() >= 2, "the Gnawing Mass shed " + all(h, PlagueRat.class).size() + " rats"));
    }

    @GameTest(template = ARENA, batch = "nest_reinforces", timeoutTicks = 60)
    public static void hurtFormCallsTheNest(GameTestHelper h) {
        reset(h, 4);
        h.setBlock(new BlockPos(2, 1, 2), ModRegistry.RAT_WARREN.get());
        FormMob form = spawn(h, ModRegistry.BLACKENED.get(), 10, 10);
        form.setNoAi(true);
        h.runAfterDelay(5, () -> form.hurt(h.getLevel().damageSources().magic(), form.getMaxHealth() * 0.6f));
        succeedThenClean(h, () -> h.assertTrue(!all(h, PlagueRat.class).isEmpty(), "the warren sent no rats to the hurt Form"));
    }

    @GameTest(template = ARENA, batch = "form_killed", timeoutTicks = 40)
    public static void killingFormLowersPestilence(GameTestHelper h) {
        reset(h, 5);
        double before = data(h).points();
        FormMob form = spawn(h, ModRegistry.CHOKING_SHADE.get(), 8, 8);
        ServerPlayer p = player(h, 6, 8);
        form.hurt(h.getLevel().damageSources().playerAttack(p), Float.MAX_VALUE);
        h.succeedWhen(() -> h.assertTrue(data(h).points() < before, "killing a Form did not lower the Pestilence"));
    }
}
