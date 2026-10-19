package net.rp.rpimmersion.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.rp.rpimmersion.compat.StaffChecker;
import net.rp.rpimmersion.config.HudConfig;
import net.rp.rpimmersion.network.ClientSyncManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HudCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                LiteralCommandNode<CommandSourceStack> root) {
        var node = Commands.literal("hud")
                .requires(StaffChecker::isStaffSource);

        // /rpimmersion hud hide <element>
        node.then(Commands.literal("hide")
                .then(Commands.argument("element", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(HudConfig.ELEMENTS, builder))
                        .executes(ctx -> change(ctx, true))));

        // /rpimmersion hud show <element>
        node.then(Commands.literal("show")
                .then(Commands.argument("element", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(hidden(), builder))
                        .executes(ctx -> change(ctx, false))));

        // /rpimmersion hud status
        node.then(Commands.literal("status").executes(ctx -> {
            boolean enabled = false, exempt = false, firstPerson = false;
            try {
                enabled = HudConfig.ENABLE_HUD_CONTROL.get();
                exempt = HudConfig.EXEMPT_STAFF.get();
                firstPerson = HudConfig.FORCE_FIRST_PERSON.get();
            } catch (IllegalStateException ignored) {}
            List<String> list = hidden();
            final boolean e = enabled, x = exempt, f = firstPerson;
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§6[RPImmersion] HUD control: §f" + (e ? "ON" : "OFF")
                            + "\n§7 Hidden: §f" + (list.isEmpty() ? "none" : String.join(", ", list))
                            + "\n§7 Staff exempt: §f" + x + " §7| Force first person: §f" + f), false);
            return 1;
        }));

        // /rpimmersion hud reload
        node.then(Commands.literal("reload").executes(ctx -> {
            ClientSyncManager.syncToAll(ctx.getSource().getServer());
            ctx.getSource().sendSuccess(() -> Component.literal("§a[RPImmersion] HUD settings synced to all players."), true);
            return 1;
        }));

        root.addChild(node.build());
    }

    private static List<String> hidden() {
        try { return new ArrayList<>(HudConfig.HIDDEN_ELEMENTS.get()); }
        catch (IllegalStateException e) { return new ArrayList<>(); }
    }

    private static int change(CommandContext<CommandSourceStack> ctx, boolean hide) {
        String element = StringArgumentType.getString(ctx, "element").toLowerCase(Locale.ROOT);
        if (!HudConfig.ELEMENTS.contains(element)) {
            ctx.getSource().sendFailure(Component.literal("§c[RPImmersion] Unknown HUD element: " + element));
            return 0;
        }
        try {
            List<String> list = hidden();
            boolean changed = hide ? (!list.contains(element) && list.add(element)) : list.remove(element);
            if (!changed) {
                ctx.getSource().sendFailure(Component.literal("§c[RPImmersion] Nothing to change for: " + element));
                return 0;
            }
            HudConfig.HIDDEN_ELEMENTS.set(list);
            if (hide) HudConfig.ENABLE_HUD_CONTROL.set(true);
            HudConfig.SPEC.save();
            ClientSyncManager.syncToAll(ctx.getSource().getServer());
        } catch (IllegalStateException e) {
            ctx.getSource().sendFailure(Component.literal("§c[RPImmersion] HUD config not loaded."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a[RPImmersion] HUD element §e" + element + "§a " + (hide ? "hidden." : "shown.")), true);
        return 1;
    }
}
