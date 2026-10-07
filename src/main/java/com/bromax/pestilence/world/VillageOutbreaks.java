package com.bromax.pestilence.world;

import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.phase.Phase;
import com.bromax.pestilence.phase.PestilenceData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Every Overworld village (keyed by its meeting-point bell) and how it is faring. An outbreak
 * starts when a villager falls sick and ends half a day after the last sick villager: if anyone is
 * left alive the village has weathered it (helpers are rewarded), otherwise it has fallen.
 */
public class VillageOutbreaks extends SavedData {
    private static final String NAME = "bromaxpestilence_villages";
    private static final Factory<VillageOutbreaks> FACTORY = new Factory<>(VillageOutbreaks::new, VillageOutbreaks::load, null);
    public static final int RADIUS = 48;

    public enum Status { SEEN, OUTBREAK, SURVIVED, FALLEN }

    private static final class Village {
        Status status = Status.SEEN;
        long lastSick;
        int deaths;
        int cures;
        final Map<UUID, Integer> helpers = new HashMap<>();
    }

    private final Map<Long, Village> villages = new HashMap<>();

    public static VillageOutbreaks get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public static Optional<BlockPos> centre(ServerLevel level, BlockPos pos, int range) {
        return level.getPoiManager().findClosest(h -> h.is(PoiTypes.MEETING), pos, range, PoiManager.Occupancy.ANY);
    }

    private Village village(BlockPos centre) {
        return villages.computeIfAbsent(centre.asLong(), k -> new Village());
    }

    /** A villager here is sick: start (or keep going) an outbreak. */
    public void noteSick(ServerLevel level, Villager villager) {
        centre(level, villager.blockPosition(), RADIUS).ifPresent(c -> {
            Village v = village(c);
            if (v.status == Status.FALLEN) return;
            if (v.status != Status.OUTBREAK) {
                v.status = Status.OUTBREAK;
                v.deaths = 0;
                v.cures = 0;
                v.helpers.clear();
                announce(level, c, "village_outbreak", ChatFormatting.DARK_RED);
                level.playSound(null, c, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2f, 0.5f);
            }
            v.lastSick = level.getGameTime();
            setDirty();
        });
    }

    public void noteDeath(ServerLevel level, Villager villager) {
        centre(level, villager.blockPosition(), RADIUS).ifPresent(c -> {
            village(c).deaths++;
            setDirty();
        });
    }

    public void noteCure(ServerLevel level, Villager villager, Player helper) {
        centre(level, villager.blockPosition(), RADIUS).ifPresent(c -> {
            Village v = village(c);
            v.cures++;
            if (helper != null) v.helpers.merge(helper.getUUID(), 1, Integer::sum);
            setDirty();
        });
    }

    /** A player has come near a village for the first time. From the Heralds phase, it may already have fallen. */
    public void discover(ServerLevel level, BlockPos centre) {
        if (villages.containsKey(centre.asLong())) return;
        Village v = village(centre);
        setDirty();
        if (PestilenceData.phase(level) >= Phase.HERALDS && level.random.nextDouble() < PestConfig.fallenVillageChance()) {
            v.status = Status.FALLEN;
            VillageFall.fall(level, centre, true);
        }
    }

    public boolean known(BlockPos centre) {
        return villages.containsKey(centre.asLong());
    }

    /** Every minute: settle outbreaks that have gone half a day without a sick villager. */
    public void tick(ServerLevel level) {
        long now = level.getGameTime();
        for (Map.Entry<Long, Village> e : villages.entrySet()) {
            Village v = e.getValue();
            BlockPos c = BlockPos.of(e.getKey());
            if (v.status != Status.OUTBREAK || now - v.lastSick < 12000 || !level.isLoaded(c)) continue;
            List<Villager> alive = level.getEntitiesOfClass(Villager.class, new AABB(c).inflate(RADIUS));
            if (!alive.isEmpty()) {
                v.status = Status.SURVIVED;
                reward(level, v, alive);
                Plague.addPoints(level, PestConfig.POINTS_VILLAGE_SAVED.get());
                announce(level, c, "village_survived", ChatFormatting.DARK_GREEN);
            } else {
                v.status = Status.FALLEN;
                VillageFall.fall(level, c, false);
                Plague.addPoints(level, PestConfig.POINTS_VILLAGE_FALLEN.get());
            }
            setDirty();
        }
    }

    private static void reward(ServerLevel level, Village v, List<Villager> alive) {
        for (Map.Entry<UUID, Integer> h : v.helpers.entrySet()) {
            for (Villager villager : alive) villager.getGossips().add(h.getKey(), GossipType.MAJOR_POSITIVE, 5 * h.getValue());
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(h.getKey());
            if (player != null) {
                com.bromax.pestilence.disease.Consecration.deed(player, com.bromax.pestilence.disease.Consecration.VILLAGE);
                com.bromax.pestilence.ModAdvancements.grant(player, com.bromax.pestilence.ModAdvancements.SAVED_VILLAGE);
                player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 48000, Math.min(h.getValue(), 5) - 1));
                player.sendSystemMessage(Component.translatable("message." + Pestilence.MODID + ".village_thanks", h.getValue())
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }

    static void announce(ServerLevel level, BlockPos centre, String key, ChatFormatting colour) {
        Component msg = Component.translatable("message." + Pestilence.MODID + "." + key).withStyle(colour);
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().distSqr(centre) <= 128 * 128) p.sendSystemMessage(msg);
        }
    }

    private static VillageOutbreaks load(CompoundTag tag, HolderLookup.Provider registries) {
        VillageOutbreaks data = new VillageOutbreaks();
        for (Tag t : tag.getList("villages", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Village v = new Village();
            v.status = Status.values()[Math.min(c.getInt("status"), Status.values().length - 1)];
            v.lastSick = c.getLong("lastSick");
            v.deaths = c.getInt("deaths");
            v.cures = c.getInt("cures");
            for (Tag h : c.getList("helpers", Tag.TAG_COMPOUND)) {
                CompoundTag hc = (CompoundTag) h;
                v.helpers.put(hc.getUUID("id"), hc.getInt("cures"));
            }
            data.villages.put(c.getLong("pos"), v);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        villages.forEach((pos, v) -> {
            CompoundTag c = new CompoundTag();
            c.putLong("pos", pos);
            c.putInt("status", v.status.ordinal());
            c.putLong("lastSick", v.lastSick);
            c.putInt("deaths", v.deaths);
            c.putInt("cures", v.cures);
            ListTag helpers = new ListTag();
            v.helpers.forEach((id, n) -> {
                CompoundTag hc = new CompoundTag();
                hc.putUUID("id", id);
                hc.putInt("cures", n);
                helpers.add(hc);
            });
            c.put("helpers", helpers);
            list.add(c);
        });
        tag.put("villages", list);
        return tag;
    }
}
