package net.rp.rpimmersion.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.rp.rpimmersion.compat.StaffChecker;
import net.rp.rpimmersion.effects.EventManager;

public class EventCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                LiteralCommandNode<CommandSourceStack> root) {
        var node = Commands.literal("event").requires(StaffChecker::isStaffSource);

        node.then(Commands.literal("start")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            EventManager.get().getIds().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> start(ctx, 0))
                        .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 1440))
                                .executes(ctx -> start(ctx, IntegerArgumentType.getInteger(ctx, "minutes"))))));

        node.then(Commands.literal("stop").executes(ctx -> {
            if (!EventManager.get().stop(ctx.getSource().getServer())) {
                ctx.getSource().sendFailure(Component.literal("§c[RPImmersion] No event is running."));
                return 0;
            }
            ctx.getSource().sendSuccess(() -> Component.literal("§a[RPImmersion] Event stopped."), true);
            return 1;
        }));

        node.then(Commands.literal("status").executes(ctx -> {
            String id = EventManager.get().getActiveId();
            long left = EventManager.get().getRemainingSeconds();
            ctx.getSource().sendSuccess(() -> Component.literal(id == null
                    ? "§6[RPImmersion] No event running."
                    : "§6[RPImmersion] Event: §e" + id + " §6(" + left / 60 + "m " + left % 60 + "s left)"), false);
            return 1;
        }));

        node.then(Commands.literal("list").executes(ctx -> {
            var ids = EventManager.get().getIds();
            ctx.getSource().sendSuccess(() -> Component.literal(ids.isEmpty()
                    ? "§7[RPImmersion] No events defined."
                    : "§6[RPImmersion] Events: §e" + String.join("§7, §e", ids)), false);
            return 1;
        }));

        node.then(Commands.literal("reload").executes(ctx -> {
            EventManager.get().reload();
            ctx.getSource().sendSuccess(() -> Component.literal("§a[RPImmersion] Events reloaded."), true);
            return 1;
        }));

        root.addChild(node.build());
    }

    private static int start(CommandContext<CommandSourceStack> ctx, int minutes) {
        String id = StringArgumentType.getString(ctx, "id");
        if (!EventManager.get().start(id, ctx.getSource().getServer(), minutes)) {
            ctx.getSource().sendFailure(Component.literal("§c[RPImmersion] Unknown event or events disabled: " + id));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("§a[RPImmersion] Event started: §e" + id), true);
        return 1;
    }
}