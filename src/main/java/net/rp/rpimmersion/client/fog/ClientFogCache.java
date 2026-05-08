package net.rp.rpimmersion.client.fog;

import net.rp.rpimmersion.config.FogConfig;

/**
 * Cache côté client de l'état fog reçu du serveur.
 *
 * Transitions :
 *  - vanilla → preset  : lerp depuis les valeurs vanilla sampées en temps réel
 *  - preset  → preset  : lerp depuis les valeurs interpolées courantes
 *  - preset  → vanilla : lerp vers les valeurs vanilla (clearPending), puis active=false
 *
 * Chunk culling (Sodium/Embeddium) :
 *  FogEventHandler ne réduit jamais farPlaneDistance en dessous du vanilla.
 *  Les valeurs vanilla sont stockées ici pour que le calcul se fasse correctement.
 *
 * Shaders (Iris/Oculus) :
 *  ViewportEvent ne touche pas le pipeline GLSL des shaders. Le fog serveur
 *  n'est donc pas appliqué quand un shader pack est actif — limitation connue.
 */
public class ClientFogCache {

    // ── État machine ──────────────────────────────────────────────────────────
    private static boolean active       = false;
    private static boolean clearPending = false; // transition vers vanilla en cours
    private static int overrideFlags = FogConfig.FogPreset.FLAG_TERRAIN;

    // ── Transition ────────────────────────────────────────────────────────────
    private static float fromRed, fromGreen, fromBlue;
    private static float fromStart, fromEnd;

    private static float targetRed, targetGreen, targetBlue;
    private static float targetStart, targetEnd;

    private static float transitionProgress      = 1.0f;
    private static float transitionDurationMs    = 0f;
    private static float lastTransitionDurationMs = 2000f; // réutilisé pour la sortie
    private static long  lastTickMs              = -1;

    // ── Valeurs interpolées (lues par FogEventHandler) ────────────────────────
    // Quand active=false : mis à jour par sampleVanilla*() → toujours frais
    // Quand active=true  : mis à jour par tick() → interpolation courante
    private static float currentRed, currentGreen, currentBlue;
    private static float currentStart, currentEnd;

    // ── Valeurs vanilla brutes (sampées chaque frame par FogEventHandler) ─────
    private static float vanillaRed, vanillaGreen, vanillaBlue;
    private static float vanillaStart, vanillaEnd;

    // =========================================================================
    // SAMPLING VANILLA — appelé CHAQUE FRAME par FogEventHandler (avant override)
    // =========================================================================

    /** Capture la couleur fog vanilla du frame courant. */
    public static void sampleVanillaColor(float r, float g, float b) {
        vanillaRed   = r;
        vanillaGreen = g;
        vanillaBlue  = b;
        // Quand aucun preset n'est actif, current* suit le vanilla en temps réel
        if (!active && !clearPending) {
            currentRed   = r;
            currentGreen = g;
            currentBlue  = b;
        }
    }

    /** Capture les distances fog vanilla du frame courant. */
    public static void sampleVanillaDistances(float start, float end) {
        vanillaStart = start;
        vanillaEnd   = end;
        if (!active && !clearPending) {
            currentStart = start;
            currentEnd   = end;
        }
    }

    // =========================================================================
    // API PUBLIQUE
    // =========================================================================

    /**
     * Déclenche une transition vers un nouveau preset.
     * current* contient toujours la valeur visuelle courante (vanilla ou preset),
     * donc le lerp part du bon endroit quelle que soit la situation.
     */

    public static void setTargetFog(float r, float g, float b,
                                    float start, float end,
                                    float transitionSeconds,
                                    int flags) {
        overrideFlags = flags;
        fromRed   = currentRed;   fromGreen = currentGreen; fromBlue  = currentBlue;
        fromStart = currentStart; fromEnd   = currentEnd;

        targetRed   = r;     targetGreen = g;     targetBlue  = b;
        targetStart = start; targetEnd   = end;

        transitionDurationMs     = Math.max(0f, transitionSeconds * 1000f);
        lastTransitionDurationMs = transitionDurationMs;
        transitionProgress       = 0f;
        lastTickMs               = System.currentTimeMillis();
        active                   = true;
        clearPending             = false;
    }

    /**
     * Déclenche une transition de retour vers vanilla.
     * Reste actif (shouldApply=true) pendant la transition, puis vanilla reprend.
     */
    public static void clearFog() {
        if (!active || clearPending) return;

        fromRed   = currentRed;   fromGreen = currentGreen; fromBlue  = currentBlue;
        fromStart = currentStart; fromEnd   = currentEnd;

        // Cible = valeurs vanilla du dernier frame sampé
        targetRed   = vanillaRed;   targetGreen = vanillaGreen; targetBlue  = vanillaBlue;
        targetStart = vanillaStart; targetEnd   = vanillaEnd;

        transitionDurationMs = lastTransitionDurationMs;
        transitionProgress   = 0f;
        lastTickMs           = System.currentTimeMillis();
        clearPending         = true;
        // active reste true → FogEventHandler continue d'appliquer le fog pendant la transition
    }

    public static void reset() {
        active       = false;
        clearPending = false;
        fromRed   = fromGreen   = fromBlue   = 0f;
        fromStart = fromEnd                  = 0f;
        targetRed = targetGreen = targetBlue = 0f;
        targetStart = targetEnd              = 0f;
        currentRed = currentGreen = currentBlue = 0f;
        currentStart = currentEnd               = 0f;
        vanillaRed = vanillaGreen = vanillaBlue = 0f;
        vanillaStart = vanillaEnd               = 0f;
        transitionProgress       = 1.0f;
        transitionDurationMs     = 0f;
        lastTransitionDurationMs = 2000f;
        lastTickMs               = -1;
        overrideFlags = FogConfig.FogPreset.FLAG_TERRAIN;
    }

    // =========================================================================
    // TICK
    // =========================================================================

    public static void tick() {
        if (!active && !clearPending) return;
        if (transitionProgress >= 1.0f) {
            if (clearPending) {
                // Transition de sortie terminée → vanilla reprend
                active       = false;
                clearPending = false;
            }
            return;
        }

        long now = System.currentTimeMillis();
        if (lastTickMs < 0) { lastTickMs = now; return; }
        float elapsed = now - lastTickMs;
        lastTickMs = now;

        transitionProgress = transitionDurationMs <= 0f
                ? 1.0f
                : Math.min(1.0f, transitionProgress + elapsed / transitionDurationMs);

        float t      = easeInOut(transitionProgress);
        currentRed   = lerp(fromRed,   targetRed,   t);
        currentGreen = lerp(fromGreen, targetGreen, t);
        currentBlue  = lerp(fromBlue,  targetBlue,  t);
        currentStart = lerp(fromStart, targetStart, t);
        currentEnd   = lerp(fromEnd,   targetEnd,   t);
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public static boolean isActive()        { return active || clearPending; }
    public static float getCurrentRed()     { return currentRed; }
    public static float getCurrentGreen()   { return currentGreen; }
    public static float getCurrentBlue()    { return currentBlue; }
    public static float getCurrentStart()   { return currentStart; }
    public static float getCurrentEnd()     { return currentEnd; }
    public static float getVanillaEnd()     { return vanillaEnd; }
    public static int getOverrideFlags() { return overrideFlags; }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float easeInOut(float t) {
        return t < 0.5f ? 2f * t * t : 1f - (-2f * t + 2f) * (-2f * t + 2f) / 2f;
    }
}