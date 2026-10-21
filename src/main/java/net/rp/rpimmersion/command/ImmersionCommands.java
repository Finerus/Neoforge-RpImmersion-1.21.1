package net.rp.rpimmersion.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rp.rpimmersion.ImmersionReload;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.compat.StaffChecker;
import net.rp.rpimmersion.network.CameraShakePacket;

import java.util.Collection;

@EventBusSubscriber(modid = RpImmersion.MODID)
public class ImmersionCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        var root = Commands.literal("rpimmersion")
                .requires(StaffChecker::isStaffSource);

        var built = root.build();

        FogCommands.register(dispatcher, built);
        WeatherCommands.register(dispatcher, built);
        CalendarCommands.register(dispatcher, built);
        HudCommands.register(dispatcher, built);
        EffectsCommands.register(dispatcher, built);
        EventCommands.register(dispatcher, built);

        // /rpimmersion reload
        built.addChild(Commands.literal("reload").executes(ctx -> {
            ImmersionReload.reloadAll(ctx.getSource().getServer());
            ctx.getSource().sendSuccess(() -> Component.literal("§a[RPImmersion] All configs reloaded and synced."), true);
            return 1;
        }).build());

        // /rpimmersion shake <targets> <intensity> <seconds>
        built.addChild(Commands.literal("shake")
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("intensity", DoubleArgumentType.doubleArg(0.0, 1.0))
                                .then(Commands.argument("seconds", DoubleArgumentType.doubleArg(0.1, 30.0))
                                        .executes(ctx -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                            float intensity = (float) DoubleArgumentType.getDouble(ctx, "intensity");
                                            float seconds = (float) DoubleArgumentType.getDouble(ctx, "seconds");
                                            for (ServerPlayer p : targets) {
                                                PacketDistributor.sendToPlayer(p, new CameraShakePacket(intensity, seconds));
                                            }
                                            int count = targets.size();
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "§a[RPImmersion] Camera shake sent to §e" + count + "§a player(s)."), true);
                                            return count;
                                        }))))
                .build());

        dispatcher.getRoot().addChild(built);
        RpImmersion.LOGGER.debug("[RPImmersion] Commands registered.");
    }
}
