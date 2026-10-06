package com.bromax.pestilence;

import com.bromax.pestilence.disease.Ailment;
import com.bromax.pestilence.disease.Disease;
import com.bromax.pestilence.entity.PlagueDoctor;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.item.CenserItem;
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
import net.minecraft.world.level.Level;

import java.util.Set;

/** Rules for who can catch which disease, protection, and the infect/advance/cure transitions. */
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
        return !isPerson(entity) && (entity instanceof Animal || entity instanceof AbstractHorse)
                && AfflictedMob.formOf(entity.getType()) != null;
    }

    public static boolean isSick(LivingEntity entity) {
        return entity.hasEffect(ModRegistry.PLAGUE);
    }

    public static boolean canCatch(LivingEntity entity) {
        if (!entity.isAlive() || entity.level().isClientSide) return false;
        if (isSick(entity) || entity.hasEffect(ModRegistry.IMMUNITY)) return false;
        if (entity instanceof Player p) return !p.isCreative() && !p.isSpectator();
        if (entity instanceof PlagueDoctor) return false;
        if (entity instanceof AbstractVillager) return true;
        return (isHostAnimal(entity) || isPerson(entity) && AfflictedMob.formOf(entity.getType()) != null)
                && PestilenceData.phase(entity.level()) >= Phase.MURRAIN;
    }

    public static Disease disease(LivingEntity entity) {
        return entity.getData(ModRegistry.AILMENT).disease();
    }

    public static Ailment ailment(LivingEntity entity) {
        return entity.getData(ModRegistry.AILMENT);
    }

    // ---------------------------------------------------------------------------------------------
    // Protection
    // ---------------------------------------------------------------------------------------------

    public static boolean wearsMask(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModRegistry.PLAGUE_MASK.get());
    }

    /** Safe from coughs and miasma: wearing the mask, or within a lit censer's smoke. */
    public static boolean airProtected(LivingEntity entity) {
        if (wearsMask(entity)) return true;
        for (Player p : entity.level().players()) {
            if (p.distanceToSqr(entity) <= CenserItem.RADIUS * CenserItem.RADIUS && CenserItem.isSwinging(p)) return true;
        }
        return false;
    }

    /**
     * Multiplier on infection by contact (bites, blows, blighted soil, handling the dead). The mask
     * takes off its share and each other piece of the Doctor's Garb a third of the rest: the full
     * set makes the wearer immune.
     */
    public static double contactExposure(LivingEntity entity) {
        double mask = PestConfig.MASK_BITE_REDUCTION.get();
        double exposure = 1.0;
        if (wearsMask(entity)) exposure -= mask;
        double perPiece = (1.0 - mask) / 3.0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (entity.getItemBySlot(slot).is(ModRegistry.DOCTORS_GARB)) exposure -= perPiece;
        }
        return Math.max(0, exposure);
    }

    // ---------------------------------------------------------------------------------------------
    // Transitions
    // ---------------------------------------------------------------------------------------------

    public static int stageTicks(LivingEntity entity, Disease disease, int stage) {
        double ticks = PestConfig.stageTicks(stage) * disease.pace;
        if (!isPerson(entity)) ticks *= PestConfig.MOB_STAGE_SCALE.get();
        return Math.max(20, (int) ticks);
    }

    public static boolean infect(LivingEntity entity, Disease disease) {
        if (!canCatch(entity)) return false;
        force(entity, disease, 0);
        addPoints(entity, isPerson(entity) ? PestConfig.POINTS_INFECT_PERSON.get() : PestConfig.POINTS_INFECT_ANIMAL.get());
        notify(entity, "infected");
        return true;
    }

    /** Infect regardless of rules (commands, spreading). */
    public static void force(LivingEntity entity, Disease disease, int stage) {
        entity.removeEffect(ModRegistry.IMMUNITY);
        entity.setData(ModRegistry.AILMENT, new Ailment(disease, false));
        entity.addEffect(new MobEffectInstance(ModRegistry.PLAGUE, stageTicks(entity, disease, stage), stage));
    }

    /** Infection by contact: scaled by the Doctor's Garb. */
    public static void tryContactInfect(LivingEntity target, double chance, Disease disease) {
        if (target.getRandom().nextDouble() < chance * contactExposure(target)) infect(target, disease);
    }

    /** Called when the current stage runs out: worsen, or at the last stage recover (people only). */
    static void advance(LivingEntity entity, int stage) {
        Disease disease = disease(entity);
        if (stage < MAX_STAGE) {
            entity.addEffect(new MobEffectInstance(ModRegistry.PLAGUE, stageTicks(entity, disease, stage + 1), stage + 1));
            notify(entity, "stage" + (stage + 1));
        } else if (isPerson(entity) && PestConfig.NATURAL_RECOVERY.get()) {
            clear(entity);
            notify(entity, "recovered");
        } else {
            entity.addEffect(new MobEffectInstance(ModRegistry.PLAGUE, stageTicks(entity, disease, MAX_STAGE), MAX_STAGE));
        }
    }

    /** Set the stage directly (lancet, leeches). Stage below 0 cures. */
    public static void setStage(LivingEntity entity, int stage) {
        if (stage < 0) {
            cure(entity);
            return;
        }
        Disease disease = disease(entity);
        entity.removeEffect(ModRegistry.PLAGUE);
        entity.addEffect(new MobEffectInstance(ModRegistry.PLAGUE, stageTicks(entity, disease, stage), stage));
    }

    /** A remedy for some diseases. Returns false (and does nothing) if it is the wrong one. */
    public static boolean treat(LivingEntity entity, Set<Disease> remedyFor) {
        if (!isSick(entity) || !remedyFor.contains(disease(entity))) return false;
        cure(entity);
        return true;
    }

    /** Any cure (the right remedy, theriac, the doctor): clears it, grants immunity and lowers the Pestilence. */
    public static void cure(LivingEntity entity) {
        if (isSick(entity)) addPoints(entity, PestConfig.POINTS_CURE.get());
        clear(entity);
    }

    private static void clear(LivingEntity entity) {
        entity.removeEffect(ModRegistry.PLAGUE);
        entity.removeData(ModRegistry.AILMENT);
        int immunity = PestConfig.IMMUNITY_TICKS.get();
        if (immunity > 0) entity.addEffect(new MobEffectInstance(ModRegistry.IMMUNITY, immunity, 0));
        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
    }

    public static void addPoints(Entity source, double amount) {
        addPoints(source.level(), amount);
    }

    public static void addPoints(Level level, double amount) {
        MinecraftServer server = level.getServer();
        if (server != null && amount != 0) PestilenceData.get(server).add(server, amount);
    }

    private static void notify(LivingEntity entity, String key) {
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.translatable("message." + Pestilence.MODID + "." + key)
                    .withStyle(ChatFormatting.DARK_GREEN), true);
        }
    }
}
