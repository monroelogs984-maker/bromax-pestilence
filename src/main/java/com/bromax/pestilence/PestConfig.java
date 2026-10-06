package com.bromax.pestilence;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class PestConfig {
    public static final ModConfigSpec SPEC;

    /** How hard the Pestilence pushes. Scales the core numbers below without having to tune each one. */
    public enum Preset {
        /** A lighter plague: it stops at the Outbreak phase, grows at half pace, and no Blackblight. */
        VANILLA_PLUS(0.5, 0.6, 0.6, 4, 0.0),
        STANDARD(1.0, 1.0, 1.0, 8, 1.0),
        /** Faster, more contagious, and the sick don't get better on their own. */
        GRIM(1.6, 1.4, 1.0, 8, 1.6);

        final double growth, contagion, spawns, fallen;
        final int maxPhase;

        Preset(double growth, double contagion, double spawns, int maxPhase, double fallen) {
            this.growth = growth;
            this.contagion = contagion;
            this.spawns = spawns;
            this.maxPhase = maxPhase;
            this.fallen = fallen;
        }
    }

    public static final ModConfigSpec.EnumValue<Preset> PRESET;
    public static final ModConfigSpec.IntValue MAX_PHASE;
    public static final ModConfigSpec.BooleanValue ALLOW_BLACKBLIGHT;

    // stages
    public static final ModConfigSpec.IntValue INCUBATION_TICKS;
    public static final ModConfigSpec.IntValue FEVER_TICKS;
    public static final ModConfigSpec.IntValue BUBO_TICKS;
    public static final ModConfigSpec.DoubleValue MOB_STAGE_SCALE;
    public static final ModConfigSpec.BooleanValue NATURAL_RECOVERY;
    public static final ModConfigSpec.IntValue IMMUNITY_TICKS;
    public static final ModConfigSpec.BooleanValue NAUSEA;
    public static final ModConfigSpec.IntValue MOB_PLAGUE_DAMAGE_INTERVAL;
    public static final ModConfigSpec.IntValue VILLAGER_PLAGUE_DAMAGE_INTERVAL;
    public static final ModConfigSpec.DoubleValue FALLEN_VILLAGE_CHANCE;
    public static final ModConfigSpec.DoubleValue POINTS_VILLAGER_DEATH;
    public static final ModConfigSpec.DoubleValue POINTS_VILLAGE_SAVED;
    public static final ModConfigSpec.DoubleValue POINTS_VILLAGE_FALLEN;

    // spread
    public static final ModConfigSpec.DoubleValue CONTAGION_RADIUS;
    public static final ModConfigSpec.DoubleValue CONTAGION_CHANCE;
    public static final ModConfigSpec.IntValue CONTAGION_INTERVAL;
    public static final ModConfigSpec.DoubleValue RAT_BITE_CHANCE;
    public static final ModConfigSpec.DoubleValue AFFLICTED_BITE_CHANCE;
    public static final ModConfigSpec.DoubleValue ROTTEN_FLESH_CHANCE;
    public static final ModConfigSpec.DoubleValue RAW_MEAT_CHANCE;
    public static final ModConfigSpec.DoubleValue FOUL_WATER_CHANCE;
    public static final ModConfigSpec.DoubleValue LEECH_CHANCE;
    public static final ModConfigSpec.DoubleValue MASK_BITE_REDUCTION;
    public static final ModConfigSpec.DoubleValue MIASMA_CHANCE;
    public static final ModConfigSpec.IntValue OUTBREAK_INTERVAL;
    public static final ModConfigSpec.DoubleValue NATURAL_SPAWN_RATE;
    public static final ModConfigSpec.DoubleValue SWEATING_CHANCE;
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

    // nests and blight
    public static final ModConfigSpec.IntValue NEST_STAGE_TICKS;
    public static final ModConfigSpec.IntValue NEST_SEED_INTERVAL;
    public static final ModConfigSpec.DoubleValue NEST_SEED_CHANCE_PER_PHASE;
    public static final ModConfigSpec.IntValue NEST_SPACING;
    public static final ModConfigSpec.BooleanValue NEST_SPREADS;
    public static final ModConfigSpec.DoubleValue POINTS_NEST_DAILY;
    public static final ModConfigSpec.DoubleValue POINTS_NEST_DESTROYED;
    public static final ModConfigSpec.BooleanValue BLIGHT_ENABLED;
    public static final ModConfigSpec.IntValue BLIGHT_INTERVAL;
    public static final ModConfigSpec.IntValue BLIGHT_RADIUS_PER_STAGE;
    public static final ModConfigSpec.IntValue BLIGHT_HEAL_RANGE;
    public static final ModConfigSpec.DoubleValue BLIGHT_STEP_INFECT_CHANCE;
    public static final ModConfigSpec.IntValue BRAZIER_RADIUS;
    public static final ModConfigSpec.IntValue BRAZIER_FUEL_TICKS;

    // doctor
    public static final ModConfigSpec.IntValue DOCTOR_CHECK_INTERVAL;
    public static final ModConfigSpec.DoubleValue DOCTOR_CHANCE;
    public static final ModConfigSpec.DoubleValue DOCTOR_CHANCE_INFECTED;
    public static final ModConfigSpec.IntValue DOCTOR_STAY_TICKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("difficulty");
        PRESET = b.comment("VANILLA_PLUS: a light plague that stops at phase 4. STANDARD: as designed. GRIM: faster, more contagious, no natural recovery.")
                .defineEnum("preset", Preset.STANDARD);
        MAX_PHASE = b.comment("The Pestilence never climbs past this phase (the preset may cap it lower).")
                .defineInRange("maxPhase", 8, 0, 8);
        ALLOW_BLACKBLIGHT = b.comment("Whether the secret Blackblight can be unleashed at all (never on VANILLA_PLUS).")
                .define("allowBlackblight", true);
        b.pop();

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
        VILLAGER_PLAGUE_DAMAGE_INTERVAL = b.comment("From the Outbreak phase, villagers in the worst stage lose 1 health every this many ticks (they die unless treated).")
                .defineInRange("villagerPlagueDamageInterval", 400, 20, 72000);
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
        ROTTEN_FLESH_CHANCE = b.comment("Chance that eating rotten flesh gives a player dysentery.")
                .defineInRange("rottenFleshChance", 0.15, 0.0, 1.0);
        RAW_MEAT_CHANCE = b.comment("Chance that eating raw meat (#c:foods/raw_meat) gives a player dysentery.")
                .defineInRange("rawMeatChance", 0.05, 0.0, 1.0);
        FOUL_WATER_CHANCE = b.comment("Chance that drinking a water bottle near a plague nest gives a player dysentery.")
                .defineInRange("foulWaterChance", 0.5, 0.0, 1.0);
        LEECH_CHANCE = b.comment("Chance that filling a bottle in swamp water brings up a Leech Jar instead.")
                .defineInRange("leechChance", 0.25, 0.0, 1.0);
        MASK_BITE_REDUCTION = b.comment("How much the Plague Doctor's Mask reduces contact infection (it fully blocks coughs and miasma). The other three Doctor's Garb pieces split the rest.")
                .defineInRange("maskBiteReduction", 0.5, 0.0, 1.0);
        MIASMA_CHANCE = b.comment("Chance per second that an unmasked host standing in a miasma cloud catches the plague.")
                .defineInRange("miasmaChance", 0.25, 0.0, 1.0);
        OUTBREAK_INTERVAL = b.comment("From phase 2, ticks between rolls (per Overworld player) for a nearby animal to fall sick on its own.")
                .defineInRange("outbreakInterval", 2400, 200, 1_000_000);
        OUTBREAK_CHANCE_PER_PHASE = b.comment("Chance per roll, multiplied by (phase - 1).")
                .defineInRange("outbreakChancePerPhase", 0.12, 0.0, 1.0);
        NATURAL_SPAWN_RATE = b.comment("Fraction of this mod's natural mob spawns that are allowed (lower it to share the night with other mob mods, e.g. Lycanites).")
                .defineInRange("naturalSpawnRate", 1.0, 0.0, 1.0);
        SWEATING_CHANCE = b.comment("Chance per half minute that an overheated player (Cold Sweat body temperature over 50, or sprinting through hot country by day) comes down with Sweating Sickness.")
                .defineInRange("sweatingChance", 0.1, 0.0, 1.0);
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
        POINTS_VILLAGER_DEATH = b.defineInRange("pointsVillagerDeath", 6.0, -1000.0, 1000.0);
        POINTS_VILLAGE_SAVED = b.defineInRange("pointsVillageSaved", -15.0, -1000.0, 1000.0);
        POINTS_VILLAGE_FALLEN = b.defineInRange("pointsVillageFallen", 25.0, -1000.0, 1000.0);
        FALLEN_VILLAGE_CHANCE = b.comment("From the Heralds phase, chance that a village found for the first time has already fallen to the plague.")
                .defineInRange("fallenVillageChance", 0.25, 0.0, 1.0);
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

        b.push("nests");
        NEST_STAGE_TICKS = b.comment("Ticks for a nest to grow one stage (I to IV).")
                .defineInRange("stageTicks", 24000, 200, 10_000_000);
        NEST_SEED_INTERVAL = b.comment("Ticks between rolls (per Overworld player) for a new nest to form nearby, from phase 3.")
                .defineInRange("seedInterval", 6000, 200, 10_000_000);
        NEST_SEED_CHANCE_PER_PHASE = b.comment("Chance per roll, multiplied by (phase - 2).")
                .defineInRange("seedChancePerPhase", 0.08, 0.0, 1.0);
        NEST_SPACING = b.comment("Minimum blocks between nests.")
                .defineInRange("spacing", 40, 8, 512);
        NEST_SPREADS = b.comment("Stage IV nests plant a new stage I nest nearby every stage interval.")
                .define("stageFourSpreads", true);
        POINTS_NEST_DAILY = b.comment("Plague Points each nest adds per day, multiplied by its stage.")
                .defineInRange("pointsPerDay", 5.0, -1000.0, 1000.0);
        POINTS_NEST_DESTROYED = b.comment("Plague Points for destroying a nest, multiplied by its stage.")
                .defineInRange("pointsDestroyed", -8.0, -1000.0, 1000.0);
        b.pop();

        b.push("blight");
        BLIGHT_ENABLED = b.comment("Whether nests spread the Blight (from phase 4). It only changes grass, natural leaves, crops and small plants, and heals on its own.")
                .define("enabled", true);
        BLIGHT_INTERVAL = b.comment("Ticks between blight spread attempts per nest.")
                .defineInRange("interval", 100, 10, 72000);
        BLIGHT_RADIUS_PER_STAGE = b.comment("Blight radius around a nest per stage.")
                .defineInRange("radiusPerStage", 3, 1, 16);
        BLIGHT_HEAL_RANGE = b.comment("Blighted blocks with no nest within this range slowly heal.")
                .defineInRange("healRange", 32, 8, 128);
        BLIGHT_STEP_INFECT_CHANCE = b.comment("Chance per tick that an unmasked host standing on blighted soil catches the plague.")
                .defineInRange("stepInfectChance", 0.0015, 0.0, 1.0);
        BRAZIER_RADIUS = b.comment("Radius a lit Herb Brazier protects from blight, miasma and nests.")
                .defineInRange("brazierRadius", 10, 2, 48);
        BRAZIER_FUEL_TICKS = b.comment("Ticks of burn time per herb added to a brazier (max 4 herbs' worth).")
                .defineInRange("brazierFuelTicks", 6000, 20, 1_000_000);
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

    public static Preset preset() {
        return PRESET.get();
    }

    public static double dailyGrowth() {
        return DAILY_GROWTH.get() * preset().growth;
    }

    public static double contagionChance() {
        return CONTAGION_CHANCE.get() * preset().contagion;
    }

    public static double naturalSpawnRate() {
        return NATURAL_SPAWN_RATE.get() * preset().spawns;
    }

    public static boolean naturalRecovery() {
        return preset() != Preset.GRIM && NATURAL_RECOVERY.get();
    }

    public static int maxPhase() {
        return Math.min(MAX_PHASE.get(), preset().maxPhase);
    }

    public static double fallenVillageChance() {
        return Math.min(1, FALLEN_VILLAGE_CHANCE.get() * preset().fallen);
    }

    public static boolean blackblightAllowed() {
        return preset() != Preset.VANILLA_PLUS && ALLOW_BLACKBLIGHT.get();
    }

    public static int stageTicks(int stage) {
        return switch (stage) {
            case 0 -> INCUBATION_TICKS.get();
            case 1 -> FEVER_TICKS.get();
            default -> BUBO_TICKS.get();
        };
    }
}
