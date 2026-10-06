package com.bromax.pestilence.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker effect: while active the entity cannot catch the plague. */
public class ImmunityEffect extends MobEffect {
    public ImmunityEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7fb86a);
    }
}
