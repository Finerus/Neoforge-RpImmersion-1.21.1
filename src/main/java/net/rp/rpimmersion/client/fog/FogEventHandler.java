package net.rp.rpimmersion.client.fog;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rp.rpimmersion.RpImmersion;

/**
 * Hooks sur les ViewportEvents NeoForge pour injecter le fog du serveur.
 *
 * ComputeFogColor → couleur RGB
 * RenderFog       → distances start/end + cancel du rendu vanilla
 */
@EventBusSubscriber(modid = RpImmersion.MODID, value = Dist.CLIENT)
public class FogEventHandler {

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        ClientFogCache.tick(); // Une seule fois par frame (ici avant RenderFog)

        if (!ClientFogCache.isActive()) return;

        event.setRed(ClientFogCache.getCurrentRed());
        event.setGreen(ClientFogCache.getCurrentGreen());
        event.setBlue(ClientFogCache.getCurrentBlue());
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!ClientFogCache.isActive()) return;

        event.setNearPlaneDistance(ClientFogCache.getCurrentStart());
        event.setFarPlaneDistance(ClientFogCache.getCurrentEnd());
        event.setCanceled(true); // Empêche vanilla de réécrire nos valeurs
    }
}
