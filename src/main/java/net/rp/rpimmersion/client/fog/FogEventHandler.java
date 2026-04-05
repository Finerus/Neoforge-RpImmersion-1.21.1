package net.rp.rpimmersion.client.fog;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.client.ClientPermissions;
import net.rp.rpimmersion.config.ClientImmersionConfig;

/**
 * Applique le fog serveur côté client.
 *
 * COMPATIBILITÉ :
 * - Sodium/Embeddium : NeoForge ViewportEvent fired avant leur pipeline.
 * - Optifine/Oculus  : idem.
 * - On SET uniquement si ClientFogCache.isActive() pour ne pas interférer
 *   avec le fog vanilla ou d'autres mods quand on n'a pas de preset.
 */
@EventBusSubscriber(modid = RpImmersion.MODID, value = Dist.CLIENT)
public class FogEventHandler {

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!shouldApply()) return;

        // OVERRIDE de la couleur du fog
        event.setRed(ClientFogCache.getCurrentRed());
        event.setGreen(ClientFogCache.getCurrentGreen());
        event.setBlue(ClientFogCache.getCurrentBlue());
    }

    @SubscribeEvent
    public static void onFogRender(ViewportEvent.RenderFog event) {
        if (!shouldApply()) return;

        // OVERRIDE des distances du fog
        event.setNearPlaneDistance(ClientFogCache.getCurrentStart());
        event.setFarPlaneDistance(ClientFogCache.getCurrentEnd());
        event.setCanceled(true); // Empêche le fog vanilla de s'appliquer par-dessus
    }

    private static boolean shouldApply() {
        if (!ClientFogCache.isActive()) return false;
        // Vérifier si le client a désactivé le fog serveur (override autorisé)
        if (ClientPermissions.canOverrideFog()) {
            try {
                if (ClientImmersionConfig.DISABLE_SERVER_FOG.get()) return false;
            } catch (IllegalStateException ignored) {}
        }
        return true;
    }
}
