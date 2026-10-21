package net.rp.rpimmersion.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration CLIENT de RpImmersion.
 * Permet au joueur de personnaliser son expérience si le serveur l'autorise.
 * Accessible dans le menu Options → Mods de NeoForge.
 */
public class ClientImmersionConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // =========================================================================
    // CAMERA SHAKE
    // =========================================================================
    /** Activer/désactiver le shake caméra côté client. */
    public static final ModConfigSpec.BooleanValue CAMERA_SHAKE_ENABLED;
    /** Multiplicateur d'intensité (0.0 = off, 1.0 = 100%, 2.0 = double). */
    public static final ModConfigSpec.DoubleValue  CAMERA_SHAKE_INTENSITY_MULTIPLIER;

    // =========================================================================
    // FOG (client override si le serveur le permet)
    // =========================================================================
    /** Si true, le client peut ignorer le fog du serveur. */
    public static final ModConfigSpec.BooleanValue DISABLE_SERVER_FOG;
    public static final ModConfigSpec.DoubleValue EFFECTS_INTENSITY_MULTIPLIER;

    // =========================================================================
    // STATIC INIT
    // =========================================================================
    static {
        BUILDER.push("Camera Shake");

        CAMERA_SHAKE_ENABLED = BUILDER
                .comment("Enable camera shake effects (e.g. on lightning strike).",
                        "Only takes effect if the server allows client override.")
                .define("cameraShakeEnabled", true);

        CAMERA_SHAKE_INTENSITY_MULTIPLIER = BUILDER
                .comment("Multiplier for camera shake intensity.",
                        "1.0 = server value, 0.0 = disabled, 0.5 = half intensity.")
                .defineInRange("cameraShakeIntensityMultiplier", 1.0, 0.0, 2.0);

        BUILDER.pop();

        BUILDER.push("Fog");

        DISABLE_SERVER_FOG = BUILDER
                .comment("Disable all server-controlled fog effects for this client.",
                        "Only takes effect if the server allows client override.")
                .define("disableServerFog", false);

        BUILDER.pop();

        BUILDER.push("Effects");

        EFFECTS_INTENSITY_MULTIPLIER = BUILDER
                .comment("Multiplier for camera movement and screen overlays (low health, fatigue, temperature, events).",
                        "1.0 = server value, 0.0 = disabled. Useful against motion sickness.",
                        "Only takes effect if the server allows client override.")
                .defineInRange("effectsIntensityMultiplier", 1.0, 0.0, 1.0);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
