package com.bromax.pestilence;

import com.bromax.pestilence.phase.Phase;
import com.bromax.pestilence.phase.PestilenceData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * /pestilence                     current phase and points (anyone)
 * /pestilence points set|add <n>  (op)
 * /pestilence phase <0-8>         jump to the start of a phase (op)
 * /pestilence infect <targets> [stage]   (op, bypasses phase/immunity rules)
 */
public final class PestilenceCommand {
    private PestilenceCommand() {}

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pestilence")
                .executes(PestilenceCommand::status)
                .then(Commands.literal("points").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("set").then(Commands.argument("value", DoubleArgumentType.doubleArg(0))
                                .executes(c -> setPoints(c, DoubleArgumentType.getDouble(c, "value"), false))))
                        .then(Commands.literal("add").then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                .executes(c -> setPoints(c, DoubleArgumentType.getDouble(c, "value"), true)))))
                .then(Commands.literal("phase").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("phase", IntegerArgumentType.integer(0, Phase.MAX))
                                .executes(c -> setPoints(c, PestilenceData.threshold(IntegerArgumentType.getInteger(c, "phase")), false))))
                .then(Commands.literal("infect").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(c -> infect(c, 0))
                                .then(Commands.argument("stage", IntegerArgumentType.integer(0, Plague.MAX_STAGE))
                                        .executes(c -> infect(c, IntegerArgumentType.getInteger(c, "stage")))))));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        PestilenceData data = PestilenceData.get(server);
        int phase = data.phase();
        Component next = phase >= Phase.MAX ? Component.literal("-")
                : Component.literal(String.valueOf((int) PestilenceData.threshold(phase + 1)));
        ctx.getSource().sendSuccess(() -> Component.translatable("command." + Pestilence.MODID + ".status",
                phase, Phase.name(phase), (int) data.points(), next), false);
        return phase;
    }

    private static int setPoints(CommandContext<CommandSourceStack> ctx, double value, boolean add) {
        MinecraftServer server = ctx.getSource().getServer();
        PestilenceData data = PestilenceData.get(server);
        if (add) data.add(server, value); else data.set(server, value);
        return status(ctx);
    }

    private static int infect(CommandContext<CommandSourceStack> ctx, int stage) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        int count = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            if (e instanceof LivingEntity living) {
                living.removeEffect(ModRegistry.IMMUNITY);
                living.addEffect(new MobEffectInstance(ModRegistry.PLAGUE, Plague.stageTicks(living, stage), stage));
                count++;
            }
        }
        int n = count;
        ctx.getSource().sendSuccess(() -> Component.translatable("command." + Pestilence.MODID + ".infected", n), true);
        return count;
    }
}
