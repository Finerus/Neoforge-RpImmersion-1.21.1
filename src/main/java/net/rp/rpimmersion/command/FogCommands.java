package net.rp.rpimmersion.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.fog.FogManager;

/**
 * Commandes staff pour le module fog.
 *
 * /rpimmersion fog set global <preset|clear>   — fog global pour tous
 * /rpimmersion fog set player <joueur> <preset|clear> — fog pour un joueur
 * /rpimmersion fog clear global                — retire l'override global
 * /rpimmersion fog clear player <joueur>       — retire l'override joueur
 * /rpimmersion fog list                        — liste les presets disponibles
 * /rpimmersion fog status                      — état actuel du système
 * /rpimmersion fog reload                      — recharge la config
 */
@EventBusSubscriber(modid = RpImmersion.MODID)
public class FogCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        var root = Commands.literal("rpimmersion")
                .requires(src -> src.hasPermission(2));

        var fogNode = Commands.literal("fog");

        // ── fog set ───────────────────────────────────────────────────────────
        var setNode = Commands.literal("set");

        // /rpimmersion fog set global <preset>
        setNode.then(Commands.literal("global")
                .then(Commands.argument("preset", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            FogManager.get().getPresets().keySet().forEach(builder::suggest);
                            builder.suggest("clear");
                            return builder.buildFuture();
                        })
                        .executes(ctx -> setGlobal(ctx, StringArgumentType.getString(ctx, "preset")))));

        // /rpimmersion fog set player <joueur> <preset>
        setNode.then(Commands.literal("player")
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("preset", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    FogManager.get().getPresets().keySet().forEach(builder::suggest);
                                    builder.suggest("clear");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> setPlayer(ctx,
                                        EntityArgument.getPlayer(ctx, "target"),
                                        StringArgumentType.getString(ctx, "preset"))))));

        fogNode.then(setNode);

        // ── fog clear ─────────────────────────────────────────────────────────
        var clearNode = Commands.literal("clear");

        clearNode.then(Commands.literal("global")
                .executes(ctx -> clearGlobal(ctx)));

        clearNode.then(Commands.literal("player")
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> clearPlayer(ctx,
                                EntityArgument.getPlayer(ctx, "target")))));

        fogNode.then(clearNode);

        // ── fog list ──────────────────────────────────────────────────────────
        fogNode.then(Commands.literal("list")
                .executes(FogCommands::listPresets));

        // ── fog status ────────────────────────────────────────────────────────
        fogNode.then(Commands.literal("status")
                .executes(FogCommands::showStatus));

        // ── fog reload ────────────────────────────────────────────────────────
        fogNode.then(Commands.literal("reload")
                .executes(FogCommands::reloadFog));

        root.then(fogNode);
        dispatcher.register(root);
    }

    // =========================================================================
    // HANDLERS
    // =========================================================================

    private static int setGlobal(CommandContext<CommandSourceStack> ctx, String preset) {
        FogManager mgr = FogManager.get();

        if ("clear".equalsIgnoreCase(preset)) {
            mgr.setGlobalOverride("");
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§a[RPImmersion] Global fog cleared — reverting to config."), true);
        } else if (!mgr.hasPreset(preset)) {
            ctx.getSource().sendFailure(Component.literal(
                    "§c[RPImmersion] Unknown preset: §f" + preset +
                    "§c. Use §f/rpimmersion fog list§c to see available presets."));
            return 0;
        } else {
            mgr.setGlobalOverride(preset);
            mgr.syncToAll(ctx.getSource().getServer());
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§a[RPImmersion] Global fog set to §e" + preset + "§a."), true);
        }
        return 1;
    }

    private static int setPlayer(CommandContext<CommandSourceStack> ctx,
                                  ServerPlayer target, String preset) {
        FogManager mgr = FogManager.get();

        if ("clear".equalsIgnoreCase(preset)) {
            mgr.setPlayerOverride(target.getUUID(), null);
            mgr.syncToPlayer(target);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§a[RPImmersion] Fog override removed for §e" +
                    target.getName().getString() + "§a."), true);
        } else if (!mgr.hasPreset(preset)) {
            ctx.getSource().sendFailure(Component.literal(
                    "§c[RPImmersion] Unknown preset: §f" + preset));
            return 0;
        } else {
            mgr.setPlayerOverride(target.getUUID(), preset);
            mgr.syncToPlayer(target);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§a[RPImmersion] Fog for §e" + target.getName().getString() +
                    "§a set to §e" + preset + "§a."), true);
        }
        return 1;
    }

    private static int clearGlobal(CommandContext<CommandSourceStack> ctx) {
        FogManager.get().setGlobalOverride(null);
        FogManager.get().syncToAll(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a[RPImmersion] Global fog override removed — following config."), true);
        return 1;
    }

    private static int clearPlayer(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        FogManager.get().setPlayerOverride(target.getUUID(), null);
        FogManager.get().syncToPlayer(target);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a[RPImmersion] Fog override cleared for §e" +
                target.getName().getString() + "§a."), true);
        return 1;
    }

    private static int listPresets(CommandContext<CommandSourceStack> ctx) {
        var presets = FogManager.get().getPresets();
        if (presets.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§7[RPImmersion] No presets defined."), false);
            return 1;
        }
        StringBuilder sb = new StringBuilder("§6╔═ RPImmersion Fog Presets ════════╗\n");
        for (var entry : presets.entrySet()) {
            var p = entry.getValue();
            sb.append(String.format(
                    "§6║ §e%-20s §7rgb(%.2f,%.2f,%.2f) dist:%s→%s trans:%ss\n",
                    entry.getKey(), p.red(), p.green(), p.blue(),
                    p.start(), p.end(), p.transitionSeconds()));
        }
        sb.append("§6╚═══════════════════════════════════╝");
        String msg = sb.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        FogManager mgr = FogManager.get();
        String global = mgr.getGlobalOverride() != null
                ? "§e" + mgr.getGlobalOverride() + " §8(command override)"
                : "§7following config";

        StringBuilder sb = new StringBuilder("§6╔═ RPImmersion Fog Status ══════════╗\n");
        sb.append("§6║ §7Presets   : §e").append(mgr.getPresets().size()).append("\n");
        sb.append("§6║ §7Zones     : §e").append(mgr.getZones().size()).append("\n");
        sb.append("§6║ §7Biomes    : §e").append(mgr.getBiomes().size()).append("\n");
        sb.append("§6║ §7Global    : ").append(global).append("\n");

        // Affiche le fog actif de chaque joueur connecté
        sb.append("§6╠═══════════════════════════════════╣\n");
        for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            var packet = mgr.resolve(p);
            String fog = packet.active() ? "§a" + packet.presetName() : "§8vanilla";
            sb.append("§6║ §f").append(p.getName().getString()).append("§7: ").append(fog).append("\n");
        }
        sb.append("§6╚═══════════════════════════════════╝");
        String msg = sb.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int reloadFog(CommandContext<CommandSourceStack> ctx) {
        FogManager.get().reload();
        FogManager.get().syncToAll(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a[RPImmersion] Fog config reloaded and synced to all players."), true);
        return 1;
    }
}
