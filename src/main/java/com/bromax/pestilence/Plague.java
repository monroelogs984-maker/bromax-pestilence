package com.bromax.pestilence;

import com.bromax.pestilence.entity.PlagueDoctor;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.phase.Phase;
import com.bromax.pestilence.phase.PestilenceData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/** Rules for who can catch the plague, and the infect/advance/cure transitions. */
public final class Plague {
    public static final int MAX_STAGE = 2;

    private Plague() {}

    /** People: players, villagers and tamed pets. They recover; they never die of it or rise. */
    public static boolean isPerson(LivingEntity entity) {
        return entity instanceof Player || entity instanceof AbstractVillager
                || entity instanceof TamableAnimal pet && pet.isTame()
                || entity instanceof AbstractHorse horse && horse.isTamed();
    }

    /** Animals that have an Afflicted form. They catch it from the Murrain phase and die of it. */
    public static boolean isHostAnimal(LivingEntity entity) {
        return !isPerson(entity) && (entity instanceof Animal || entity instanceof AbstractHorse) && AfflictedMob.formOf(entity.getType()) != null;
    }

    public static boolean canCatch(LivingEntity entity) {
        if (!entity.isAlive() || entity.level().isClientSide) return false;
        if (entity.hasEffect(ModRegistry.PLAGUE) || entity.hasEffect(ModRegistry.IMMUNITY)) return false;
        if (entity instanceof Player p) return !p.isCreative() && !p.isSpectator();
        if (entity instanceof PlagueDoctor) return false;
        if (entity instanceof AbstractVillager) return true;
        return (isHostAnimal(entity) || isPerson(entity) && AfflictedMob.formOf(entity.getType()) != null)
                && PestilenceData.phase(entity.level()) >= Phase.MURRAIN;
    }

    public static boolean wearsMask(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModRegistry.PLAGUE_MASK.get());
    }

    public static int stageTicks(LivingEntity entity, int stage) {
        int ticks = PestConfig.stageTicks(stage);
        return isPerson(entity) ? ticks : Math.max(20, (int) (ticks * PestConfig.MOB_STAGE_SCALE.get()));
    }

    public static boolean infect(LivingEntity entity) {
        if (!canCatch(entity)) return false;
        entity.addEffect(new MobEffectInstance(ModRegistry.PLAGUE, stageTicks(entity, 0), 0));
        addPoints(entity, isPerson(entity) ? PestConfig.POINTS_INFECT_PERSON.get() : PestConfig.POINTS_INFECT_ANIMAL.get());
        notify(entity, "infected");
        return true;
    }

    /** Infection by a bite or blow: the mask halves the odds instead of blocking them. */
    public static void tryBiteInfect(LivingEntity target, double chance) {
        if (wearsMask(target)) chance *= 1.0 - PestConfig.MASK_BITE_REDUCTION.get();
        if (target.getRandom().nextDouble() < chance) infect(target);
    }

    /** Called when the current stage runs out: worsen, or at the last stage recover (people only). */
    static void advance(LivingEntity entity, int stage) {
        if (stage < MAX_STAGE) {
            entity.addEffect(new MobEffectInstance(ModRegistry.PLAGUE, stageTicks(entity, stage + 1), stage + 1));
            notify(entity, "stage" + (stage + 1));
        } else if (isPerson(entity) && PestConfig.NATURAL_RECOVERY.get()) {
            clear(entity);
            notify(entity, "recovered");
        } else {
            entity.addEffect(new MobEffectInstance(ModRegistry.PLAGUE, stageTicks(entity, MAX_STAGE), MAX_STAGE));
        }
    }

    /** A cure (vinegar, the doctor): clears the plague, grants immunity and lowers the Pestilence. */
    public static void cure(LivingEntity entity) {
        if (entity.hasEffect(ModRegistry.PLAGUE)) addPoints(entity, PestConfig.POINTS_CURE.get());
        clear(entity);
    }

    private static void clear(LivingEntity entity) {
        entity.removeEffect(ModRegistry.PLAGUE);
        int immunity = PestConfig.IMMUNITY_TICKS.get();
        if (immunity > 0) entity.addEffect(new MobEffectInstance(ModRegistry.IMMUNITY, immunity, 0));
        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
    }

    public static void addPoints(Entity source, double amount) {
        MinecraftServer server = source.level().getServer();
        if (server != null && amount != 0) PestilenceData.get(server).add(server, amount);
    }

    private static void notify(LivingEntity entity, String key) {
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.translatable("message." + Pestilence.MODID + "." + key)
                    .withStyle(ChatFormatting.DARK_GREEN), true);
        }
    }
}
