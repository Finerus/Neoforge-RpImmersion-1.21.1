package net.rp.rpimmersion.client.fog;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.client.ClientPermissions;
import net.rp.rpimmersion.config.ClientImmersionConfig;

/**
 * Applique le fog serveur côté client via ViewportEvent.
 *
 * COMPATIBILITÉ :
 *
 *  Vanilla / Embeddium / Sodium sans shaders :
 *    Fonctionne correctement. ViewportEvent est fired avant le pipeline de rendu.
 *    Fix chunk culling : farPlaneDistance n'est jamais réduit sous la valeur vanilla
 *    pour éviter que Sodium/Embeddium ne culling les chunks trop tôt.
 *
 *  Shaders (Iris / Oculus) :
 *    ViewportEvent ne touche PAS le pipeline GLSL. Le fog est calculé en shader,
 *    ce qui court-circuite nos overrides de couleur et de distance.
 *    Une intégration propre nécessiterait l'Iris API (uniforms custom), hors scope.
 *    En attendant, quand un shader est actif, les valeurs vanilla sont quand même
 *    sampées correctement pour que les transitions fonctionnent dès que le shader
 *    est désactivé.
 */
@EventBusSubscriber(modid = RpImmersion.MODID, value = Dist.CLIENT)
public class FogEventHandler {

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        // Toujours sampler les valeurs vanilla EN PREMIER, avant tout override.
        // Utilisé comme point de départ pour les transitions vanilla→preset et preset→vanilla.
        ClientFogCache.sampleVanillaColor(
                (float) event.getRed(),
                (float) event.getGreen(),
                (float) event.getBlue()
        );

        if (!shouldApply()) return;

        event.setRed(ClientFogCache.getCurrentRed());
        event.setGreen(ClientFogCache.getCurrentGreen());
        event.setBlue(ClientFogCache.getCurrentBlue());
    }

    @SubscribeEvent
    public static void onFogRender(ViewportEvent.RenderFog event) {
        if (!shouldApply()) {
            // Capture les distances vanilla
            ClientFogCache.sampleVanillaDistances(
                    event.getNearPlaneDistance(),
                    event.getFarPlaneDistance()
            );
            return;
        }

        event.setNearPlaneDistance(ClientFogCache.getCurrentStart());
        event.setFarPlaneDistance(ClientFogCache.getCurrentEnd());
        event.setCanceled(true);
    }

    private static boolean shouldApply() {
        if (!ClientFogCache.isActive()) return false;
        if (ClientPermissions.canOverrideFog()) {
            try {
                if (ClientImmersionConfig.DISABLE_SERVER_FOG.get()) return false;
            } catch (IllegalStateException ignored) {}
        }
        return true;
    }
}