package com.bromax.pestilence.world;

import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.entity.Corpse;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.entity.form.MiasmaClot;
import com.bromax.pestilence.entity.herald.HeraldMob;
import com.bromax.pestilence.phase.PestilenceData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The merge: Afflicted that have killed, gathered together (or the unburned dead lying together),
 * melt into a Miasma Clot. Always telegraphed: the Afflicted stop, shudder and smoke for three
 * seconds first, and fire breaks it up.
 */
public final class Coalescence {
    public static final int TELEGRAPH = 60;

    private Coalescence() {}

    public static boolean enabled(ServerLevel level) {
        return PestConfig.FORMS_ENABLED.get() && PestilenceData.phase(level) >= PestConfig.COALESCE_PHASE.get();
    }

    /** Kills needed; beside a nest the plague gathers faster. */
    private static int killsNeeded(AfflictedMob mob) {
        int kills = PestConfig.COALESCE_KILLS.get();
        return Tracker.nestWithin(mob.level(), mob.blockPosition(), 24) ? kills / 2 : kills;
    }

    /** Afflicted animals and villagers (not Heralds) that have killed enough. */
    public static boolean canJoin(net.minecraft.world.entity.LivingEntity entity) {
        return entity instanceof AfflictedMob mob && !(mob instanceof HeraldMob) && mob.isAlive() && !mob.isOnFire()
                && mob.level() instanceof ServerLevel level && enabled(level) && mob.kills() >= killsNeeded(mob);
    }

    /** Every two seconds from each ready Afflicted: gather a group and start the telegraph. */
    public static void tick(ServerLevel level, AfflictedMob mob) {
        if (mob.isCoalescing() || !canJoin(mob)) return;
        List<AfflictedMob> group = level.getEntitiesOfClass(AfflictedMob.class, mob.getBoundingBox().inflate(4),
                o -> o != mob && !o.isCoalescing() && canJoin(o));
        int size = PestConfig.COALESCE_GROUP.get();
        if (group.size() + 1 < size) return;
        mob.startCoalescing(mob.getUUID());
        for (AfflictedMob other : group.subList(0, size - 1)) other.startCoalescing(mob.getUUID());
        level.playSound(null, mob.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    /** The leader's telegraph ran out: everyone still with it melts into one clot. */
    public static void finish(ServerLevel level, AfflictedMob leader) {
        List<AfflictedMob> members = level.getEntitiesOfClass(AfflictedMob.class, leader.getBoundingBox().inflate(8),
                o -> o.isAlive() && leader.getUUID().equals(o.coalesceLeader()));
        if (members.size() < 2) {
            members.forEach(AfflictedMob::stopCoalescing);
            return;
        }
        Vec3 centre = Vec3.ZERO;
        for (AfflictedMob m : members) centre = centre.add(m.position());
        centre = centre.scale(1.0 / members.size());
        for (AfflictedMob m : members) {
            level.sendParticles(ParticleTypes.SQUID_INK, m.getX(), m.getY() + m.getBbHeight() * 0.5, m.getZ(), 20, 0.3, 0.3, 0.3, 0.02);
            m.discard();
        }
        MiasmaClot.create(level, centre.x, centre.y, centre.z, members.size());
    }

    /** A corpse about to rise beside two or more others may melt into a clot with them instead. */
    public static boolean corpsesCoalesce(ServerLevel level, Corpse corpse) {
        if (!enabled(level)) return false;
        List<Corpse> others = level.getEntitiesOfClass(Corpse.class, corpse.getBoundingBox().inflate(4),
                c -> c != corpse && c.isAlive() && !c.isBurning());
        if (others.size() < 2 || level.random.nextDouble() >= PestConfig.CORPSE_COALESCE_CHANCE.get()) return false;
        Vec3 centre = corpse.position();
        for (Corpse c : others) {
            centre = centre.add(c.position());
            level.sendParticles(ParticleTypes.SQUID_INK, c.getX(), c.getY() + 0.3, c.getZ(), 15, 0.4, 0.2, 0.4, 0.02);
            c.discard();
        }
        centre = centre.scale(1.0 / (others.size() + 1));
        corpse.discard();
        MiasmaClot.create(level, centre.x, centre.y, centre.z, others.size() + 1);
        return true;
    }
}
