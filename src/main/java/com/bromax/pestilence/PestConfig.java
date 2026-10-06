package com.bromax.pestilence;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class PestConfig {
    public static final ModConfigSpec SPEC;

    // stages
    public static final ModConfigSpec.IntValue INCUBATION_TICKS;
    public static final ModConfigSpec.IntValue FEVER_TICKS;
    public static final ModConfigSpec.IntValue BUBO_TICKS;
    public static final ModConfigSpec.DoubleValue MOB_STAGE_SCALE;
    public static final ModConfigSpec.BooleanValue NATURAL_RECOVERY;
    public static final ModConfigSpec.IntValue IMMUNITY_TICKS;
    public static final ModConfigSpec.BooleanValue NAUSEA;
    public static final ModConfigSpec.IntValue MOB_PLAGUE_DAMAGE_INTERVAL;

    // spread
    public static final ModConfigSpec.DoubleValue CONTAGION_RADIUS;
    public static final ModConfigSpec.DoubleValue CONTAGION_CHANCE;
    public static final ModConfigSpec.IntValue CONTAGION_INTERVAL;
    public static final ModConfigSpec.DoubleValue RAT_BITE_CHANCE;
    public static final ModConfigSpec.DoubleValue AFFLICTED_BITE_CHANCE;
    public static final ModConfigSpec.DoubleValue ROTTEN_FLESH_CHANCE;
    public static final ModConfigSpec.DoubleValue MASK_BITE_REDUCTION;
    public static final ModConfigSpec.DoubleValue MIASMA_CHANCE;
    public static final ModConfigSpec.IntValue OUTBREAK_INTERVAL;
    public static final ModConfigSpec.DoubleValue OUTBREAK_CHANCE_PER_PHASE;

    // phases
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> PHASE_THRESHOLDS;
    public static final ModConfigSpec.DoubleValue DAILY_GROWTH;
    public static final ModConfigSpec.DoubleValue POINTS_INFECT_PERSON;
    public static final ModConfigSpec.DoubleValue POINTS_INFECT_ANIMAL;
    public static final ModConfigSpec.DoubleValue POINTS_PLAGUE_DEATH;
    public static final ModConfigSpec.DoubleValue POINTS_CORPSE_RISE;
    public static final ModConfigSpec.DoubleValue POINTS_CURE;
    public static final ModConfigSpec.DoubleValue POINTS_CORPSE_BURN;
    public static final ModConfigSpec.DoubleValue POINTS_AFFLICTED_KILL;

    // corpses
    public static final ModConfigSpec.IntValue CORPSE_RISE_TICKS;
    public static final ModConfigSpec.IntValue CORPSE_BURN_TICKS;
    public static final ModConfigSpec.IntValue PYRE_RADIUS;
    public static final ModConfigSpec.DoubleValue CARRY_INFECT_CHANCE;

    // doctor
    public static final ModConfigSpec.IntValue DOCTOR_CHECK_INTERVAL;
    public static final ModConfigSpec.DoubleValue DOCTOR_CHANCE;
    public static final ModConfigSpec.DoubleValue DOCTOR_CHANCE_INFECTED;
    public static final ModConfigSpec.IntValue DOCTOR_STAY_TICKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("stages");
        INCUBATION_TICKS = b.comment("Stage 1 (Incubation) length in ticks for players and villagers. No symptoms, not contagious.")
                .defineInRange("incubationTicks", 6000, 20, 1_000_000);
        FEVER_TICKS = b.comment("Stage 2 (Fever) length in ticks. Hunger and coughing; contagious.")
                .defineInRange("feverTicks", 9000, 20, 1_000_000);
        BUBO_TICKS = b.comment("Stage 3 (Buboes) length in ticks. Weakness, mining fatigue, dizzy spells.")
                .defineInRange("buboTicks", 12000, 20, 1_000_000);
        MOB_STAGE_SCALE = b.comment("Stage lengths for animals are multiplied by this, so herds sicken and die visibly fast.")
                .defineInRange("mobStageScale", 0.2, 0.01, 10.0);
        NATURAL_RECOVERY = b.comment("Players and villagers recover on their own after the Buboes stage. If false only a cure ends it.")
                .define("naturalRecovery", true);
        IMMUNITY_TICKS = b.comment("Plague Immunity granted by recovering or being cured, in ticks.")
                .defineInRange("immunityTicks", 12000, 0, 1_000_000);
        NAUSEA = b.comment("Whether the Buboes stage causes occasional short nausea.")
                .define("nausea", true);
        MOB_PLAGUE_DAMAGE_INTERVAL = b.comment("Animals in the Buboes stage (phase 2+) lose 1 health every this many ticks until they die.")
                .defineInRange("mobPlagueDamageInterval", 100, 10, 72000);
        b.pop();

        b.push("spread");
        CONTAGION_RADIUS = b.comment("Blocks around a coughing (Fever or Buboes) entity that can catch the plague.")
                .defineInRange("contagionRadius", 4.0, 0.0, 32.0);
        CONTAGION_CHANCE = b.comment("Chance per contagion check for each nearby unmasked host to catch it.")
                .defineInRange("contagionChance", 0.06, 0.0, 1.0);
        CONTAGION_INTERVAL = b.comment("Ticks between contagion checks for each sick entity.")
                .defineInRange("contagionInterval", 100, 20, 72000);
        RAT_BITE_CHANCE = b.comment("Chance that a plague rat bite infects its target.")
                .defineInRange("ratBiteChance", 0.3, 0.0, 1.0);
        AFFLICTED_BITE_CHANCE = b.comment("Chance that a hit from an Afflicted mob infects its target.")
                .defineInRange("afflictedBiteChance", 0.25, 0.0, 1.0);
        ROTTEN_FLESH_CHANCE = b.comment("Chance that eating rotten flesh infects a player.")
                .defineInRange("rottenFleshChance", 0.15, 0.0, 1.0);
        MASK_BITE_REDUCTION = b.comment("How much the Plague Doctor's Mask reduces bite infection (it fully blocks airborne contagion and miasma).")
                .defineInRange("maskBiteReduction", 0.5, 0.0, 1.0);
        MIASMA_CHANCE = b.comment("Chance per second that an unmasked host standing in a miasma cloud catches the plague.")
                .defineInRange("miasmaChance", 0.25, 0.0, 1.0);
        OUTBREAK_INTERVAL = b.comment("From phase 2, ticks between rolls (per Overworld player) for a nearby animal to fall sick on its own.")
                .defineInRange("outbreakInterval", 2400, 200, 1_000_000);
        OUTBREAK_CHANCE_PER_PHASE = b.comment("Chance per roll, multiplied by (phase - 1).")
                .defineInRange("outbreakChancePerPhase", 0.12, 0.0, 1.0);
        b.pop();

        b.push("phases");
        PHASE_THRESHOLDS = b.comment("Plague Points needed for phases 1 to 8.")
                .defineList("thresholds", List.of(100, 250, 500, 900, 1500, 2400, 3600, 5000), o -> o instanceof Integer i && i >= 0);
        DAILY_GROWTH = b.comment("Plague Points added every Minecraft day on its own. This is what makes ignoring the plague a losing move.")
                .defineInRange("dailyGrowth", 30.0, 0.0, 100_000.0);
        POINTS_INFECT_PERSON = b.defineInRange("pointsInfectPerson", 4.0, -1000.0, 1000.0);
        POINTS_INFECT_ANIMAL = b.defineInRange("pointsInfectAnimal", 2.0, -1000.0, 1000.0);
        POINTS_PLAGUE_DEATH = b.defineInRange("pointsPlagueDeath", 3.0, -1000.0, 1000.0);
        POINTS_CORPSE_RISE = b.defineInRange("pointsCorpseRise", 10.0, -1000.0, 1000.0);
        POINTS_CURE = b.comment("Negative values lower the Pestilence.")
                .defineInRange("pointsCure", -4.0, -1000.0, 1000.0);
        POINTS_CORPSE_BURN = b.defineInRange("pointsCorpseBurn", -6.0, -1000.0, 1000.0);
        POINTS_AFFLICTED_KILL = b.defineInRange("pointsAfflictedKill", -1.0, -1000.0, 1000.0);
        b.pop();

        b.push("corpses");
        CORPSE_RISE_TICKS = b.comment("Ticks until a plague corpse rises as an Afflicted mob.")
                .defineInRange("riseTicks", 3600, 100, 1_000_000);
        CORPSE_BURN_TICKS = b.comment("Ticks a burning corpse takes to be consumed.")
                .defineInRange("burnTicks", 60, 1, 6000);
        PYRE_RADIUS = b.comment("Corpses within this many blocks of a lit pyre catch fire.")
                .defineInRange("pyreRadius", 3, 1, 8);
        CARRY_INFECT_CHANCE = b.comment("Chance every 10 seconds that carrying a shrouded corpse without a mask infects you.")
                .defineInRange("carryInfectChance", 0.05, 0.0, 1.0);
        b.pop();

        b.push("doctor");
        DOCTOR_CHECK_INTERVAL = b.comment("Ticks between Plague Doctor visit rolls, per player in the Overworld.")
                .defineInRange("checkInterval", 12000, 200, 1_000_000);
        DOCTOR_CHANCE = b.comment("Chance per roll that a Plague Doctor visits a healthy player.")
                .defineInRange("visitChance", 0.1, 0.0, 1.0);
        DOCTOR_CHANCE_INFECTED = b.comment("Chance per roll that a Plague Doctor visits an infected player.")
                .defineInRange("visitChanceInfected", 0.5, 0.0, 1.0);
        DOCTOR_STAY_TICKS = b.comment("How long a visiting Plague Doctor stays before leaving, in ticks.")
                .defineInRange("stayTicks", 36000, 200, 1_000_000);
        b.pop();

        SPEC = b.build();
    }

    private PestConfig() {}

    public static int stageTicks(int stage) {
        return switch (stage) {
            case 0 -> INCUBATION_TICKS.get();
            case 1 -> FEVER_TICKS.get();
            default -> BUBO_TICKS.get();
        };
    }
}
