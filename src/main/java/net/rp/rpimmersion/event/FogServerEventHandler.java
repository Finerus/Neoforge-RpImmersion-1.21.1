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
import net.rp.rpimmersion.network.WeatherSyncPacket;

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

        // Fog initial
        FogManager.get().syncToPlayer(player);

        // Permissions client (shake/fog override)
        sendClientPermissions(player);
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

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void sendClientPermissions(ServerPlayer player) {
        boolean allowShake = true;
        boolean allowFog   = false; // Par défaut : le serveur contrôle le fog
        try {
            allowShake = WeatherConfig.ALLOW_CLIENT_SHAKE_OVERRIDE.get();
        } catch (IllegalStateException ignored) {}
        PacketDistributor.sendToPlayer(player, new WeatherSyncPacket(allowShake, allowFog));
    }
}
