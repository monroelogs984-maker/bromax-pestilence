package com.bromax.pestilence.disease;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.Pestilence;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Good deeds against the Pestilence. A Consecrated Cleaver grows with the deeds of whoever carries
 * it, not with killing: curing the sick, burning the dead, destroying nests, saving villages and
 * felling the plague's champions.
 */
public final class Consecration {
    public static final int CURE = 2, BURN = 1, NEST = 3, VILLAGE = 5, BOSS = 10;
    private static final int[] LEVELS = {0, 10, 30, 60, 100};

    private Consecration() {}

    public static int deeds(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("deeds");
    }

    public static int level(ItemStack stack) {
        int d = deeds(stack), l = 0;
        for (int i = 0; i < LEVELS.length; i++) if (d >= LEVELS[i]) l = i + 1;
        return l;
    }

    /** Deeds needed for the next level, or -1 at the top. */
    public static int next(ItemStack stack) {
        int l = level(stack);
        return l >= LEVELS.length ? -1 : LEVELS[l];
    }

    public static void deed(Player player, int amount) {
        if (player == null || player.level().isClientSide) return;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(ModRegistry.CONSECRATED_CLEAVER.get())) continue;
            int before = level(stack);
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putInt("deeds", tag.getInt("deeds") + amount);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            if (level(stack) > before) {
                player.displayClientMessage(Component.translatable("message." + Pestilence.MODID + ".consecrated", level(stack))
                        .withStyle(ChatFormatting.GOLD), true);
                player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.4f);
            }
        }
    }
}
