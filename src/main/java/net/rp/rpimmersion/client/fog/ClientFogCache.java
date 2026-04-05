package net.rp.rpimmersion.client.fog;

/**
 * Cache côté client de l'état fog reçu du serveur.
 *
 * BUG FIX : L'ancienne implémentation utilisait currentRed comme source de lerp
 * ET la modifiait chaque frame → approche exponentielle, pas de vraie transition.
 * Fix : stocker fromRed/Green/Blue/Start/End comme point de départ FIXE.
 */
public class ClientFogCache {

    private static boolean active       = false;
    private static boolean clearPending = false;

    // Point de départ FIXE (ne change pas pendant la transition)
    private static float fromRed, fromGreen, fromBlue;
    private static float fromStart, fromEnd;

    // Valeurs cibles reçues du serveur
    private static float targetRed, targetGreen, targetBlue;
    private static float targetStart, targetEnd;

    // Valeurs interpolées lues par FogEventHandler
    private static float currentRed, currentGreen, currentBlue;
    private static float currentStart, currentEnd;

    private static float transitionProgress   = 1.0f;
    private static float transitionDurationMs = 1000f;
    private static long  lastTickMs           = -1;

    // =========================================================================
    // API PUBLIQUE
    // =========================================================================

    public static void setTargetFog(float r, float g, float b,
                                    float start, float end,
                                    float transitionSeconds) {
        fromRed   = currentRed;   fromGreen = currentGreen; fromBlue  = currentBlue;
        fromStart = currentStart; fromEnd   = currentEnd;

        targetRed   = r;   targetGreen = g;   targetBlue  = b;
        targetStart = start; targetEnd = end;

        transitionDurationMs = Math.max(0f, transitionSeconds * 1000f);
        transitionProgress   = 0f;
        lastTickMs           = System.currentTimeMillis();
        active               = true;
        clearPending         = false;
    }

    public static void clearFog() {
        if (!active && !clearPending) return;
        fromRed   = currentRed;   fromGreen = currentGreen; fromBlue  = currentBlue;
        fromStart = currentStart; fromEnd   = currentEnd;
        targetRed = 0; targetGreen = 0; targetBlue = 0;
        targetStart = 0; targetEnd = 0;
        transitionProgress = 0f;
        lastTickMs         = System.currentTimeMillis();
        clearPending       = true;
    }

    public static void reset() {
        active = false; clearPending = false;
        fromRed = fromGreen = fromBlue = 0f;
        fromStart = fromEnd = 0f;
        targetRed = targetGreen = targetBlue = 0f;
        targetStart = targetEnd = 0f;
        currentRed = currentGreen = currentBlue = 0f;
        currentStart = currentEnd = 0f;
        transitionProgress = 1.0f;
        lastTickMs = -1;
    }

    // =========================================================================
    // TICK
    // =========================================================================

    public static void tick() {
        if (!active && !clearPending) return;
        if (transitionProgress >= 1.0f) {
            if (clearPending) { clearPending = false; active = false; }
            return;
        }

        long now = System.currentTimeMillis();
        if (lastTickMs < 0) { lastTickMs = now; return; }
        float elapsed = now - lastTickMs;
        lastTickMs = now;

        transitionProgress = transitionDurationMs <= 0f
                ? 1.0f
                : Math.min(1.0f, transitionProgress + elapsed / transitionDurationMs);

        float t = easeInOut(transitionProgress);
        currentRed   = lerp(fromRed,   targetRed,   t);
        currentGreen = lerp(fromGreen, targetGreen, t);
        currentBlue  = lerp(fromBlue,  targetBlue,  t);
        currentStart = lerp(fromStart, targetStart, t);
        currentEnd   = lerp(fromEnd,   targetEnd,   t);
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public static boolean isActive()      { return active || clearPending; }
    public static float getCurrentRed()   { return currentRed; }
    public static float getCurrentGreen() { return currentGreen; }
    public static float getCurrentBlue()  { return currentBlue; }
    public static float getCurrentStart() { return currentStart; }
    public static float getCurrentEnd()   { return currentEnd; }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float easeInOut(float t) {
        return t < 0.5f ? 2f * t * t : 1f - (-2f * t + 2f) * (-2f * t + 2f) / 2f;
    }
}
