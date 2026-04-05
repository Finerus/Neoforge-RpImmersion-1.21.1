package net.rp.rpimmersion.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LightningBolt;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.config.WeatherConfig;
import net.rp.rpimmersion.network.WeatherSyncPacket;
import net.rp.rpimmersion.weather.WeatherManager;

public class WeatherServerEventHandler {

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        WeatherManager.reset();
        WeatherManager.get().reload();
        RpImmersion.LOGGER.info("[RPImmersion] Weather system ready.");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        WeatherManager.reset();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        try {
            if (!WeatherConfig.ENABLE_WEATHER_SYSTEM.get()) return;
        } catch (IllegalStateException e) { return; }
        WeatherManager.get().tick(event.getServer());
    }

    @SubscribeEvent
    public void onLightningStruck(EntityStruckByLightningEvent event) {
        if (!(event.getLightning() instanceof LightningBolt bolt)) return;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        WeatherManager.get().onLightningStruck(bolt.blockPosition(), server);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            boolean allowShake = WeatherConfig.ALLOW_CLIENT_SHAKE_OVERRIDE.get();
            PacketDistributor.sendToPlayer(player, new WeatherSyncPacket(allowShake, false));
        } catch (IllegalStateException ignored) {}
    }
}
