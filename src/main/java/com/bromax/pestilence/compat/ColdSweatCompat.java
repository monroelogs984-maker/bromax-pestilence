package com.bromax.pestilence.compat;

import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.world.entity.LivingEntity;

/** Only loaded when Cold Sweat is installed (see Compat). */
final class ColdSweatCompat {
    private ColdSweatCompat() {}

    static double body(LivingEntity entity) {
        return Temperature.get(entity, Temperature.Trait.BODY);
    }

    static void warm(LivingEntity entity, double amount) {
        Temperature.add(entity, Temperature.Trait.CORE, amount);
    }
}
