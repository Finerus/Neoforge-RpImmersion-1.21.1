package net.rp.rpimmersion.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.fog.FogManager;

public class FogServerEventHandler {

    private int tickCounter = 0;
    private static final int ZONE_CHECK_INTERVAL = 40; // 2 secondes

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
        FogManager.get().syncToPlayer(player);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        FogManager.get().clearPlayerCache(player.getUUID());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < ZONE_CHECK_INTERVAL) return;
        tickCounter = 0;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            FogManager.get().syncToPlayer(player);
        }
    }
}