package net.rp.rpimmersion;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.rp.rpimmersion.client.ClientSetup;
import net.rp.rpimmersion.command.ImmersionCommands;
import net.rp.rpimmersion.config.*;
import net.rp.rpimmersion.event.*;
import org.slf4j.Logger;

@Mod("rpimmersion")
public class RpImmersion {

    public static final String MODID = "rpimmersion";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RpImmersion(IEventBus modEventBus, ModContainer modContainer) {

        // Server configs
        modContainer.registerConfig(ModConfig.Type.SERVER, FogConfig.SPEC,      "rpimmersion/rpimmersion-fog.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, WeatherConfig.SPEC,  "rpimmersion/rpimmersion-weather.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, CalendarConfig.SPEC, "rpimmersion/rpimmersion-calendar.toml");

        // Client config
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientImmersionConfig.SPEC, "rpimmersion-client.toml");

        // Packet registration via MOD bus
        modEventBus.register(net.rp.rpimmersion.network.NetworkHandler.class);

        // Client-only setup — bonne façon en NeoForge 1.21.1
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modEventBus);
        }

        // Server-side events (GAME bus)
        NeoForge.EVENT_BUS.register(new FogServerEventHandler());
        NeoForge.EVENT_BUS.register(new WeatherServerEventHandler());
        NeoForge.EVENT_BUS.register(new CalendarServerEventHandler());

        // Commands
        NeoForge.EVENT_BUS.register(ImmersionCommands.class);

        LOGGER.info("[RPImmersion] Mod initialized.");
    }
}