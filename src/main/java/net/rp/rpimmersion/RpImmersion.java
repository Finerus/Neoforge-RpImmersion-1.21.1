package net.rp.rpimmersion;

import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
        modContainer.registerConfig(ModConfig.Type.SERVER, HudConfig.SPEC,      "rpimmersion/rpimmersion-hud.toml");

        // Client config
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientImmersionConfig.SPEC, "rpimmersion-client.toml");

        // Packet registration via MOD bus
        modEventBus.register(net.rp.rpimmersion.network.NetworkHandler.class);
        modEventBus.addListener(RpImmersion::onConfigReloading);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modEventBus, modContainer);
        }

        // Server-side events (GAME bus)
        NeoForge.EVENT_BUS.register(new FogServerEventHandler());
        NeoForge.EVENT_BUS.register(new WeatherServerEventHandler());
        NeoForge.EVENT_BUS.register(new CalendarServerEventHandler());

        LOGGER.info("[RPImmersion] Mod initialized.");
    }

    private static final AtomicBoolean RELOAD_PENDING = new AtomicBoolean(false);

    private static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getType() != ModConfig.Type.SERVER) return;
        if (!MODID.equals(event.getConfig().getModId())) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !RELOAD_PENDING.compareAndSet(false, true)) return;
        CompletableFuture.runAsync(() -> server.execute(() -> {
            RELOAD_PENDING.set(false);
            ImmersionReload.reloadAll(server);
        }), CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS));
    }
}