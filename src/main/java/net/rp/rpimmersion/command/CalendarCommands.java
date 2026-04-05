package net.rp.rpimmersion.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.rp.rpimmersion.calendar.CalendarManager;

public class CalendarCommands {

    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher,
                                com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> root) {
        var node = Commands.literal("calendar")
                .requires(src -> net.rp.rpessentials.RpEssentialsPermissions.isStaff(src.getPlayer()));

        node.then(Commands.literal("status").executes(ctx -> {
            CalendarManager cm = CalendarManager.get();
            CalendarManager.DateSnapshot s = cm.getSnapshot();
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§6[RPImmersion Calendar]\n§7Display: §f" + cm.formatDisplay(true) +
                    "\n§7Year " + s.year() + " M" + s.month() + " D" + s.day() +
                    " " + s.hour() + "h" + String.format("%02d", s.minute()) +
                    "\n§7Paused: §f" + cm.isPaused()), false);
            return 1;
        }));

        node.then(Commands.literal("pause")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean p = BoolArgumentType.getBool(ctx, "value");
                            CalendarManager.get().setPaused(p, ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§a[RPImmersion] Calendar " + (p ? "§epaused" : "§aresumed") + "§a."), true);
                            return 1;
                        })));

        node.then(Commands.literal("set")
                .then(Commands.argument("year", IntegerArgumentType.integer(0))
                        .then(Commands.argument("month", IntegerArgumentType.integer(1))
                                .then(Commands.argument("day", IntegerArgumentType.integer(1))
                                        .executes(ctx -> doSet(ctx, 0, 0))
                                        .then(Commands.argument("hour", IntegerArgumentType.integer(0))
                                                .executes(ctx -> doSet(ctx, IntegerArgumentType.getInteger(ctx, "hour"), 0))
                                                .then(Commands.argument("minute", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> doSet(ctx,
                                                                IntegerArgumentType.getInteger(ctx, "hour"),
                                                                IntegerArgumentType.getInteger(ctx, "minute")))))))));

        node.then(Commands.literal("advance")
                .then(Commands.argument("minutes", IntegerArgumentType.integer())
                        .executes(ctx -> {
                            int m = IntegerArgumentType.getInteger(ctx, "minutes");
                            CalendarManager.get().advance(m, ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§a[RPImmersion] Advanced §e" + m + "§a RP minutes."), false);
                            return 1;
                        })));

        node.then(Commands.literal("reload").executes(ctx -> {
            CalendarManager.get().syncToAll(ctx.getSource().getServer());
            ctx.getSource().sendSuccess(() -> Component.literal("§a[RPImmersion] Calendar synced."), true);
            return 1;
        }));

        root.addChild(node.build());
    }

    private static int doSet(CommandContext<CommandSourceStack> ctx, int hour, int minute) {
        int y = IntegerArgumentType.getInteger(ctx, "year");
        int m = IntegerArgumentType.getInteger(ctx, "month");
        int d = IntegerArgumentType.getInteger(ctx, "day");
        CalendarManager.get().setDate(y, m, d, hour, minute, ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a[RPImmersion] Calendar set: §e" + CalendarManager.get().formatDisplay(false)), true);
        return 1;
    }
}
