package net.rp.rpimmersion.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.config.FogConfig;
import net.rp.rpimmersion.config.WeatherConfig;
import net.rp.rpimmersion.fog.FogManager;
import net.rp.rpimmersion.network.ClientSyncManager;

/**
 * Gère le cycle de vie serveur du fog.
 * Envoie aussi le WeatherSyncPacket (permissions client shake/fog) à la connexion.
 *
 * OPTIMISATION : FogManager.syncToPlayer() compare l'état précédent avant d'envoyer
 * → pas de packet réseau si rien n'a changé.
 */
public class FogServerEventHandler {

    private int tickCounter = 0;

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        FogManager.reset();
        FogManager.get().reload();
        RpImmersion.LOGGER.info("[RPImmersion] Fog system ready.");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        FogManager.reset();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ClientSyncManager.syncToPlayer(player);
        FogManager.get().syncToPlayer(player);
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) FogManager.get().syncToPlayer(player);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) FogManager.get().syncToPlayer(player);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        FogManager.get().clearPlayerCache(player.getUUID());
    }

    /**
     * Vérification périodique — intervalle configurable (défaut 60 ticks = 3s).
     * Nécessaire pour détecter les changements de zone, biome, world border, et cycle jour/nuit.
     */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        int interval;
        try { interval = FogConfig.ZONE_CHECK_INTERVAL_TICKS.get(); }
        catch (IllegalStateException e) { interval = 60; }

        if (++tickCounter < interval) return;
        tickCounter = 0;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            FogManager.get().syncToPlayer(player);
        }
    }
}
