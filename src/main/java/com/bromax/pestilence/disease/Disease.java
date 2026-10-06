package com.bromax.pestilence.disease;

import com.bromax.pestilence.ModDamage;
import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.effect.PlagueEffect;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;

/**
 * The sicknesses. They all show as the same "Sickness" effect (amplifier = stage), and are told
 * apart by their symptoms or a Physician's Kit. Each has its own sources, pace and cure.
 *
 *   BUBONIC    rats, fleas in blighted soil, handling corpses   fever, then buboes (weakness, heavy limbs)
 *   PNEUMONIC  miasma, coughs (very contagious)                 constant coughing, breathlessness; fastest
 *   SEPTICEMIC Afflicted blows                                  weakness, bleeding (never below 2 hearts)
 *   DYSENTERY  rotten flesh, raw meat, water near nests         hunger and nausea, no cough
 *   ROT        Afflicted/Herald blows from phase 7              weakness, green rot; turns mobs Afflicted; Theriac only
 *   SWEATING   overheating (Cold Sweat) or hot country           sweat, hunger, weakness; quick; sleep cures it
 */
public enum Disease implements StringRepresentable {
    BUBONIC(1.0, true, 1.0),
    PNEUMONIC(0.6, true, 2.0),
    SEPTICEMIC(0.8, false, 0.0),
    DYSENTERY(0.7, false, 0.0),
    /** Phase 7+: from Afflicted and Herald blows. Mobs and villagers that reach the worst stage turn Afflicted on the spot. */
    ROT(0.7, false, 0.0),
    /** From overheating (Cold Sweat) or hot country. Fierce and quick; a full night's sleep cures it. */
    SWEATING(0.35, false, 0.0);

    public static final Codec<Disease> CODEC = StringRepresentable.fromEnum(Disease::values);

    /** Stage lengths are multiplied by this. */
    public final double pace;
    public final boolean contagious;
    public final double contagion;

    Disease(double pace, boolean contagious, double contagion) {
        this.pace = pace;
        this.contagious = contagious;
        this.contagion = contagion;
    }

    public Component displayName() {
        return Component.translatable("disease." + Pestilence.MODID + "." + getSerializedName());
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }

    /** Applied every 40 ticks while sick. People get the full set; animals only the visible parts. */
    public void symptoms(ServerLevel level, LivingEntity e, int stage, boolean person) {
        switch (this) {
            case BUBONIC -> {
                if (stage >= 1) {
                    if (person) hidden(e, MobEffects.HUNGER, 0);
                    if (e.getRandom().nextInt(3) == 0) PlagueEffect.cough(level, e);
                }
                if (stage >= 2) {
                    hidden(e, MobEffects.WEAKNESS, 0);
                    hidden(e, MobEffects.MOVEMENT_SLOWDOWN, 0);
                    if (person) hidden(e, MobEffects.DIG_SLOWDOWN, 0);
                    if (person && PestConfig.NAUSEA.get() && e.getRandom().nextInt(12) == 0) brief(e, MobEffects.CONFUSION, 120);
                }
            }
            case PNEUMONIC -> {
                if (stage >= 1) {
                    PlagueEffect.cough(level, e);
                    hidden(e, MobEffects.MOVEMENT_SLOWDOWN, stage - 1);
                    if (person) hidden(e, MobEffects.HUNGER, 0);
                }
                if (stage >= 2) hidden(e, MobEffects.WEAKNESS, 0);
            }
            case SEPTICEMIC -> {
                if (stage >= 1) {
                    hidden(e, MobEffects.WEAKNESS, stage - 1);
                    if (person) hidden(e, MobEffects.HUNGER, 0);
                }
                // Bleeding: every other symptom tick, never below 2 hearts.
                if (stage >= 2 && e.tickCount % 80 < 40 && e.getHealth() > 4f) {
                    e.hurt(ModDamage.plague(level), 1f);
                }
            }
            case ROT -> {
                if (stage >= 1) {
                    hidden(e, MobEffects.WEAKNESS, stage - 1);
                    if (person) hidden(e, MobEffects.HUNGER, 1);
                    level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.35f, 0.42f, 0.12f), 1.2f),
                            e.getX(), e.getY(0.6), e.getZ(), 4, 0.25, 0.3, 0.25, 0);
                }
                if (stage >= 2) {
                    hidden(e, MobEffects.MOVEMENT_SLOWDOWN, 0);
                    if (person) hidden(e, MobEffects.DIG_SLOWDOWN, 1);
                }
            }
            case SWEATING -> {
                if (stage >= 1) {
                    if (person) hidden(e, MobEffects.HUNGER, stage);
                    hidden(e, MobEffects.WEAKNESS, stage - 1);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.FALLING_WATER, e.getX(), e.getY(0.8), e.getZ(), 4, 0.3, 0.3, 0.3, 0);
                }
                if (stage >= 2) hidden(e, MobEffects.MOVEMENT_SLOWDOWN, 0);
            }
            case DYSENTERY -> {
                if (stage >= 1 && person) hidden(e, MobEffects.HUNGER, stage);
                if (stage >= 1 && person && e.getRandom().nextInt(stage >= 2 ? 4 : 10) == 0) brief(e, MobEffects.CONFUSION, 100);
                if (stage >= 2) hidden(e, MobEffects.WEAKNESS, 0);
            }
        }
    }

    private static void hidden(LivingEntity e, Holder<MobEffect> effect, int amplifier) {
        e.addEffect(new MobEffectInstance(effect, 60, amplifier, true, false, false));
    }

    private static void brief(LivingEntity e, Holder<MobEffect> effect, int ticks) {
        e.addEffect(new MobEffectInstance(effect, ticks, 0, true, false, false));
    }
}
