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

    static net.minecraft.world.item.ItemStack find(LivingEntity entity, java.util.function.Predicate<net.minecraft.world.item.ItemStack> test) {
        return CuriosApi.getCuriosInventory(entity).flatMap(inv -> inv.findFirstCurio(test))
                .map(top.theillusivec4.curios.api.SlotResult::stack).orElse(net.minecraft.world.item.ItemStack.EMPTY);
    }
}
