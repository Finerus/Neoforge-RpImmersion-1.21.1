package net.rp.rpimmersion.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.rp.rpimmersion.config.WeatherConfig;
import net.rp.rpimmersion.weather.WeatherManager;

public class WeatherCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> root) {
        var node = Commands.literal("weather")
                .requires(src -> net.rp.rpessentials.RpEssentialsPermissions.isStaff(src.getPlayer()));

        node.then(Commands.literal("set")
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("CLEAR"); builder.suggest("RAIN"); builder.suggest("THUNDER");
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String typStr = StringArgumentType.getString(ctx, "type").toUpperCase();
                            WeatherConfig.WeatherType type;
                            try { type = WeatherConfig.WeatherType.valueOf(typStr); }
                            catch (Exception e) {
                                ctx.getSource().sendFailure(Component.literal("§c[RPImmersion] Unknown type: " + typStr));
                                return 0;
                            }
                            ServerLevel level = ctx.getSource().getServer().overworld();
                            switch (type) {
                                case CLEAR   -> level.setWeatherParameters(6000, 0, false, false);
                                case RAIN    -> level.setWeatherParameters(0, 6000, true, false);
                                case THUNDER -> level.setWeatherParameters(0, 6000, true, true);
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[RPImmersion] Weather: §e" + typStr), true);
                            return 1;
                        })));

        node.then(Commands.literal("status")
                .executes(ctx -> {
                    ServerLevel level = ctx.getSource().getServer().overworld();
                    String cur = level.isThundering() ? "THUNDER" : level.isRaining() ? "RAIN" : "CLEAR";
                    String fog = WeatherManager.get().getCurrentWeatherFogPreset();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "§6[RPImmersion] Weather: §e" + cur + "\n§6Fog preset: §e" + (fog != null ? fog : "none")), false);
                    return 1;
                }));

        node.then(Commands.literal("reload")
                .executes(ctx -> {
                    WeatherManager.get().reload();
                    ctx.getSource().sendSuccess(() -> Component.literal("§a[RPImmersion] Weather reloaded."), true);
                    return 1;
                }));

        root.addChild(node.build());
    }
}
