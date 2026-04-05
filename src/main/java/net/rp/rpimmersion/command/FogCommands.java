package net.rp.rpimmersion.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.rp.rpimmersion.config.FogConfig;
import net.rp.rpimmersion.fog.FogManager;

public class FogCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> root) {
        var node = Commands.literal("fog")
                .requires(src -> net.rp.rpessentials.RpEssentialsPermissions.isStaff(src.getPlayer()));

        // /rpimmersion fog set global <preset|clear>
        node.then(Commands.literal("set")
                .then(Commands.literal("global")
                        .then(Commands.argument("preset", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    FogManager.get().getPresets().keySet().forEach(builder::suggest);
                                    builder.suggest("clear");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String preset = StringArgumentType.getString(ctx, "preset");
                                    if (preset.equalsIgnoreCase("clear")) {
                                        FogManager.get().setGlobalOverride(null);
                                        ctx.getSource().sendSuccess(() -> Component.literal("§a[RPImmersion] Global fog cleared."), true);
                                    } else if (!FogManager.get().hasPreset(preset)) {
                                        ctx.getSource().sendFailure(Component.literal("§c[RPImmersion] Unknown preset: " + preset));
                                        return 0;
                                    } else {
                                        FogManager.get().setGlobalOverride(preset);
                                        ctx.getSource().sendSuccess(() -> Component.literal("§a[RPImmersion] Global fog set to: §e" + preset), true);
                                    }
                                    FogManager.get().syncToAll(ctx.getSource().getServer());
                                    return 1;
                                })))
                .then(Commands.literal("player")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("preset", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            FogManager.get().getPresets().keySet().forEach(builder::suggest);
                                            builder.suggest("clear");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                            String preset = StringArgumentType.getString(ctx, "preset");
                                            if (preset.equalsIgnoreCase("clear")) {
                                                FogManager.get().setPlayerOverride(target.getUUID(), null);
                                                ctx.getSource().sendSuccess(() -> Component.literal("§a[RPImmersion] Fog cleared for §e" + target.getName().getString()), true);
                                            } else if (!FogManager.get().hasPreset(preset)) {
                                                ctx.getSource().sendFailure(Component.literal("§c[RPImmersion] Unknown preset: " + preset));
                                                return 0;
                                            } else {
                                                FogManager.get().setPlayerOverride(target.getUUID(), preset);
                                                ctx.getSource().sendSuccess(() -> Component.literal("§a[RPImmersion] Fog for §e" + target.getName().getString() + "§a set to: §e" + preset), true);
                                            }
                                            FogManager.get().syncToPlayer(target);
                                            return 1;
                                        })))));

        // /rpimmersion fog clear global
        node.then(Commands.literal("clear")
                .then(Commands.literal("global").executes(ctx -> {
                    FogManager.get().setGlobalOverride(null);
                    FogManager.get().syncToAll(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("§a[RPImmersion] Global fog cleared."), true);
                    return 1;
                }))
                .then(Commands.literal("player")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    FogManager.get().setPlayerOverride(target.getUUID(), null);
                                    FogManager.get().syncToPlayer(target);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[RPImmersion] Fog cleared for §e" + target.getName().getString()), true);
                                    return 1;
                                }))));

        // /rpimmersion fog list
        node.then(Commands.literal("list").executes(ctx -> {
            var presets = FogManager.get().getPresets();
            if (presets.isEmpty()) {
                ctx.getSource().sendSuccess(() -> Component.literal("§7[RPImmersion] No fog presets loaded."), false);
                return 1;
            }
            StringBuilder sb = new StringBuilder("§6[RPImmersion] Fog presets:\n");
            presets.forEach((name, p) -> sb.append("§6  ").append(name)
                    .append("§7 — RGB(").append(p.red()).append(",").append(p.green()).append(",").append(p.blue())
                    .append(") dist ").append(p.start()).append("-").append(p.end())
                    .append(" (").append(p.transitionSeconds()).append("s)\n"));
            ctx.getSource().sendSuccess(() -> Component.literal(sb.toString().stripTrailing()), false);
            return 1;
        }));

        // /rpimmersion fog status
        node.then(Commands.literal("status").executes(ctx -> {
            String global = FogManager.get().getGlobalOverride();
            boolean worldBorderTmp;
            try { worldBorderTmp = FogConfig.WORLD_BORDER_FOG_ENABLED.get(); }
            catch (Exception e) { worldBorderTmp = false; }
            final boolean worldBorder = worldBorderTmp; // ← effectively final
            int zones  = FogManager.get().getZones().size();
            int biomes = FogManager.get().getBiomes().size();
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§6[RPImmersion] Fog Status:\n" +
                            "§7 Global override: §f" + (global != null ? global : "none") + "\n" +
                            "§7 World border fog: §f" + worldBorder + "\n" +
                            "§7 Zones: §f" + zones + " | Biomes: §f" + biomes), false);
            return 1;
        }));

        // /rpimmersion fog reload
        node.then(Commands.literal("reload").executes(ctx -> {
            FogManager.get().reload();
            FogManager.get().syncToAll(ctx.getSource().getServer());
            ctx.getSource().sendSuccess(() -> Component.literal("§a[RPImmersion] Fog config reloaded and synced."), true);
            return 1;
        }));

        root.addChild(node.build());
    }
}
