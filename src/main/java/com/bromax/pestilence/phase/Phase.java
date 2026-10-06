package com.bromax.pestilence.phase;

import com.bromax.pestilence.Pestilence;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Phase numbers that gate content, and the server-wide announcement when the phase changes. */
public final class Phase {
    public static final int MAX = 8;

    public static final int FULL_RATS = 1;
    public static final int MURRAIN = 2;        // animals sicken and die, corpses rise
    public static final int RAT_SEASON = 3;     // Afflicted spawn naturally at night
    public static final int OUTBREAK = 4;       // the Blight
    public static final int HERALDS = 5;        // plague pits
    public static final int BLACK_DEATH = 6;    // miasma wells

    private Phase() {}

    public static Component name(int phase) {
        return Component.translatable("phase." + Pestilence.MODID + "." + phase);
    }

    static void announce(MinecraftServer server, int from, int to) {
        boolean rising = to > from;
        Component title = Component.translatable("message." + Pestilence.MODID + (rising ? ".phase_up" : ".phase_down"), to, name(to))
                .withStyle(rising ? ChatFormatting.DARK_RED : ChatFormatting.DARK_GREEN, ChatFormatting.BOLD);
        Component detail = Component.translatable("phase." + Pestilence.MODID + "." + to + ".desc").withStyle(ChatFormatting.GRAY);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(title);
            player.sendSystemMessage(detail);
            player.playNotifySound(rising ? SoundEvents.BELL_BLOCK : SoundEvents.BELL_RESONATE, SoundSource.AMBIENT, 1f, rising ? 0.6f : 1.2f);
        }
    }
}
