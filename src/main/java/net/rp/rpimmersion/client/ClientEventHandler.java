package net.rp.rpimmersion.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.client.camera.CameraShakeHandler;
import net.rp.rpimmersion.client.fog.ClientFogCache;
import net.rp.rpimmersion.client.hud.CalendarHudCache;

@EventBusSubscriber(modid = RpImmersion.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientFogCache.reset();
        CameraShakeHandler.reset();
        CalendarHudCache.reset();
        ClientPermissions.reset();
        RpImmersion.LOGGER.info("[RPImmersion] Client caches reset on disconnect.");
    }
}