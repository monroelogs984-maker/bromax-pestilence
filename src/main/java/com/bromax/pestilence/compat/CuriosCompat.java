package com.bromax.pestilence.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

/** Only loaded when Curios is installed (see Compat). */
final class CuriosCompat {
    private CuriosCompat() {}

    static boolean equipped(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity).map(inv -> inv.isEquipped(item)).orElse(false);
    }
}
