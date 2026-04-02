package net.rp.rpimmersion.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.client.fog.ClientFogCache;

@EventBusSubscriber(modid = RpImmersion.MODID, value = Dist.CLIENT)
public class ClientSetup {

    public static void register(IEventBus modEventBus) {
        // Rien à enregistrer sur le mod bus pour l'instant
        // Les @EventBusSubscriber static suffisent
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientFogCache.reset();
        RpImmersion.LOGGER.debug("[RPImmersion] Client fog cache reset on disconnect.");
    }
}
