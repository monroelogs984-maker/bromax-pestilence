package com.bromax.pestilence;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public final class ModDamage {
    public static final ResourceKey<DamageType> PLAGUE = ResourceKey.create(Registries.DAMAGE_TYPE, Pestilence.id("plague"));
    public static final ResourceKey<DamageType> BLACKBLIGHT = ResourceKey.create(Registries.DAMAGE_TYPE, Pestilence.id("blackblight"));

    private ModDamage() {}

    public static DamageSource plague(Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(PLAGUE));
    }

    public static DamageSource blackblight(Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BLACKBLIGHT));
    }
}
