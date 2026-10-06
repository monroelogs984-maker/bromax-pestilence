package com.bromax.pestilence.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * The only door to optional mods. Each check goes through here, so a missing mod's classes are
 * never touched.
 */
public final class Compat {
    private static Boolean curios;
    private static Boolean coldSweat;

    private Compat() {}

    public static boolean curios() {
        if (curios == null) curios = ModList.get().isLoaded("curios");
        return curios;
    }

    public static boolean coldSweat() {
        if (coldSweat == null) coldSweat = ModList.get().isLoaded("cold_sweat");
        return coldSweat;
    }

    /** Worn as a curio (with Curios), or otherwise held in the offhand or carried (per `anywhere`). */
    public static boolean wearing(LivingEntity entity, Item item, boolean anywhere) {
        if (curios() && CuriosCompat.equipped(entity, item)) return true;
        if (entity.getOffhandItem().is(item)) return true;
        if (anywhere && !curios() && entity instanceof net.minecraft.world.entity.player.Player p) {
            for (ItemStack s : p.getInventory().items) if (s.is(item)) return true;
        }
        return false;
    }

    /** Cold Sweat body temperature (above 50 is overheating), or NaN without Cold Sweat. */
    public static double bodyTemperature(LivingEntity entity) {
        return coldSweat() ? ColdSweatCompat.body(entity) : Double.NaN;
    }

    /** A fever: nudge Cold Sweat core temperature up, if it is installed. */
    public static void fever(LivingEntity entity, double amount) {
        if (coldSweat()) ColdSweatCompat.warm(entity, amount);
    }
}
