package net.rp.rpimmersion.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rp.rpimmersion.RpImmersion;

@EventBusSubscriber(modid = RpImmersion.MODID)
public class ImmersionCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        var root = Commands.literal("rpimmersion")
                .requires(src -> net.rp.rpessentials.RpEssentialsPermissions.isStaff(src.getPlayer()));

        var built = root.build();

        FogCommands.register(dispatcher, built);
        WeatherCommands.register(dispatcher, built);
        CalendarCommands.register(dispatcher, built);

        dispatcher.getRoot().addChild(built);
        RpImmersion.LOGGER.debug("[RPImmersion] Commands registered.");
    }
}
