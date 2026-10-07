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
    public static final int DANSE_MACABRE = 7;  // the Rot
    /** The secret ninth: never reached by points, only by burning the Heart of the Pestilence (see HeartOfPestilenceItem). */
    public static final int BLACKBLIGHT = 9;

    private Phase() {}

    public static Component name(int phase) {
        return Component.translatable("phase." + Pestilence.MODID + "." + phase);
    }

    static void announce(MinecraftServer server, int from, int to) {
        boolean rising = to > from;
        Component title = Component.translatable("message." + Pestilence.MODID + (rising ? ".phase_up" : ".phase_down"), to, name(to))
                .withStyle(rising ? ChatFormatting.DARK_RED : ChatFormatting.DARK_GREEN, ChatFormatting.BOLD);
        Component detail = Component.translatable("phase." + Pestilence.MODID + "." + to + ".desc").withStyle(ChatFormatting.GRAY);
        PestilenceData data = PestilenceData.get(server);
        Component next = to < data.cap()
                ? Component.translatable("message." + Pestilence.MODID + ".phase_next", to + 1, name(to + 1)).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                : null;
        // Whoever decides for the world (an op, or the singleplayer host) can stop it right here.
        Component halt = Component.translatable("message." + Pestilence.MODID + ".halt_prompt").withStyle(style -> style
                .withColor(ChatFormatting.GOLD).withUnderlined(true)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/pestilence halt"))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("message." + Pestilence.MODID + ".halt_hover"))));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(title);
            player.sendSystemMessage(detail);
            if (rising && next != null) player.sendSystemMessage(next);
            if (rising && to <= MAX && canDecide(server, player)) player.sendSystemMessage(halt);
            player.playNotifySound(rising ? SoundEvents.BELL_BLOCK : SoundEvents.BELL_RESONATE, SoundSource.AMBIENT, 1f, rising ? 0.6f : 1.2f);
        }
    }

    /** Ops, or the owner of a singleplayer world (who may not have cheats on). */
    public static boolean canDecide(MinecraftServer server, ServerPlayer player) {
        return server.getPlayerList().isOp(player.getGameProfile()) || server.isSingleplayerOwner(player.getGameProfile());
    }
}
