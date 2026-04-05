package net.rp.rpimmersion.client.camera;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.client.ClientPermissions;
import net.rp.rpimmersion.config.ClientImmersionConfig;

import java.util.Random;

/**
 * Gère le shake de caméra côté client.
 *
 * COMPATIBILITÉ MODS POPULAIRES :
 * - Sodium/Embeddium : ViewportEvent.ComputeCameraAngles est un event NeoForge
 *   fired AVANT le pipeline Sodium → compatible.
 * - Optifine/Oculus : idem, le hook NeoForge précède leur rendu.
 * - Dynamic FOV, Quake Pro : ils modifient FOV, pas les angles → 0 conflit.
 * - Autres mods de shake (ex: earthquakes) : on ADD notre delta aux valeurs
 *   déjà présentes dans l'event → additif, pas remplacement → compatible.
 *
 * Principe : on ne SET jamais les angles, on ADD notre delta.
 * Ainsi si un autre mod a déjà modifié les angles, on s'empile dessus.
 */
@EventBusSubscriber(modid = RpImmersion.MODID, value = Dist.CLIENT)
public class CameraShakeHandler {

    private static final Random RNG = new Random();

    private static float intensity       = 0f;
    private static float durationMs      = 0f;
    private static float elapsedMs       = 0f;
    private static long  lastFrameMs     = -1;
    private static boolean shakeActive   = false;

    // =========================================================================
    // API
    // =========================================================================

    /**
     * Déclenche un shake. Appelé depuis CameraShakePacket.handleOnClient().
     * Si le client a le droit d'override ET a désactivé le shake → ignoré.
     */
    public static void trigger(float serverIntensity, float serverDurationSeconds) {
        // Vérifier override client
        if (ClientPermissions.canOverrideShake()) {
            try {
                if (!ClientImmersionConfig.CAMERA_SHAKE_ENABLED.get()) return;
                double multiplier = ClientImmersionConfig.CAMERA_SHAKE_INTENSITY_MULTIPLIER.get();
                serverIntensity *= (float) multiplier;
            } catch (IllegalStateException ignored) {}
        }

        if (serverIntensity <= 0f) return;

        intensity    = serverIntensity;
        durationMs   = serverDurationSeconds * 1000f;
        elapsedMs    = 0f;
        lastFrameMs  = System.currentTimeMillis();
        shakeActive  = true;
    }

    public static void reset() {
        shakeActive = false;
        intensity   = 0f;
        elapsedMs   = 0f;
    }

    // =========================================================================
    // EVENT
    // =========================================================================

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!shakeActive) return;

        long now = System.currentTimeMillis();
        if (lastFrameMs < 0) { lastFrameMs = now; return; }

        float delta = now - lastFrameMs;
        lastFrameMs = now;
        elapsedMs  += delta;

        if (elapsedMs >= durationMs) {
            shakeActive = false;
            return;
        }

        // Enveloppe d'intensité : montée rapide, descente progressive
        float progress = elapsedMs / durationMs;
        float envelope = (float)(1.0 - Math.pow(progress, 0.5)); // Décroissance douce

        float currentIntensity = intensity * envelope;

        // Shake aléatoire oscillant
        float yawDelta   = (RNG.nextFloat() * 2f - 1f) * currentIntensity * 3f;
        float pitchDelta = (RNG.nextFloat() * 2f - 1f) * currentIntensity * 1.5f;

        // ADDITIF — on ajoute à ce que les autres mods ont déjà mis
        event.setYaw(event.getYaw()     + yawDelta);
        event.setPitch(event.getPitch() + pitchDelta);
        // On ne touche pas au roll pour rester compatible avec les mods qui l'utilisent
    }
}
