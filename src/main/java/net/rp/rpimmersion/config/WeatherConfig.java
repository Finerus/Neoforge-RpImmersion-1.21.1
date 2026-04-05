package net.rp.rpimmersion.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;

public class WeatherConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // =========================================================================
    // GENERAL
    // =========================================================================
    public static final ModConfigSpec.BooleanValue ENABLE_WEATHER_SYSTEM;

    // =========================================================================
    // FOG INTEGRATION
    // Format : weatherType;fogPreset (weatherType = CLEAR | RAIN | THUNDER)
    // =========================================================================
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEATHER_FOG_PRESETS;

    // =========================================================================
    // LIGHTNING
    // =========================================================================
    public static final ModConfigSpec.BooleanValue  LIGHTNING_CONTROL_ENABLED;
    /** Multiplicateur du nombre d'éclairs spawné par le jeu. >1 = plus, <1 = moins. */
    public static final ModConfigSpec.DoubleValue   LIGHTNING_FREQUENCY_MULTIPLIER;
    /** Rayon max (blocs) à partir du joueur pour spawner des éclairs supplémentaires. */
    public static final ModConfigSpec.IntValue      LIGHTNING_SPAWN_RADIUS;

    // =========================================================================
    // CAMERA SHAKE (lightning)
    // =========================================================================
    public static final ModConfigSpec.BooleanValue  LIGHTNING_SHAKE_ENABLED;
    /** Distance max (blocs) à partir de laquelle un éclair provoque un shake. */
    public static final ModConfigSpec.DoubleValue   LIGHTNING_SHAKE_MAX_DISTANCE;
    /** Intensité max du shake (au contact). */
    public static final ModConfigSpec.DoubleValue   LIGHTNING_SHAKE_MAX_INTENSITY;
    /** Durée du shake en secondes. */
    public static final ModConfigSpec.DoubleValue   LIGHTNING_SHAKE_DURATION_SECONDS;
    /** Si true, le client peut désactiver le shake dans sa propre config. */
    public static final ModConfigSpec.BooleanValue  ALLOW_CLIENT_SHAKE_OVERRIDE;

    // =========================================================================
    // AUTO CYCLE
    // =========================================================================
    public static final ModConfigSpec.BooleanValue  WEATHER_CYCLE_ENABLED;
    /**
     * Liste des étapes du cycle météo.
     * Format : weatherType;durationMinutes[;message]
     * weatherType = CLEAR | RAIN | THUNDER
     */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEATHER_CYCLE_STEPS;

    // =========================================================================
    // MESSAGES NARRATIFS
    // =========================================================================
    public static final ModConfigSpec.BooleanValue  WEATHER_MESSAGES_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> MSG_WEATHER_CLEAR;
    public static final ModConfigSpec.ConfigValue<String> MSG_WEATHER_RAIN;
    public static final ModConfigSpec.ConfigValue<String> MSG_WEATHER_THUNDER;
    public static final ModConfigSpec.ConfigValue<String> WEATHER_MESSAGE_MODE;

    // =========================================================================
    // STATIC INIT
    // =========================================================================
    static {
        BUILDER.push("Weather System");

        ENABLE_WEATHER_SYSTEM = BUILDER
                .comment("Enable the RPImmersion weather system (fog integration, lightning control, etc.).")
                .define("enableWeatherSystem", true);

        BUILDER.pop();

        // ── Fog Integration ───────────────────────────────────────────────────
        BUILDER.push("Weather Fog Integration");

        WEATHER_FOG_PRESETS = BUILDER
                .comment(
                        "Apply fog presets based on current weather.",
                        "Format : weatherType;fogPreset",
                        "  weatherType : CLEAR | RAIN | THUNDER",
                        "  fogPreset   : name of a preset from rpimmersion-fog.toml",
                        "Weather fog has LOWER priority than zones/biomes by default.",
                        "It acts as a fallback when no zone/biome fog is active.",
                        "",
                        "Example:",
                        "  RAIN;morning_mist",
                        "  THUNDER;purge"
                )
                .defineList("weatherFogPresets",
                        Arrays.asList("RAIN;morning_mist", "THUNDER;purge"),
                        obj -> obj instanceof String s && s.split(";").length >= 2
                );

        BUILDER.pop();

        // ── Lightning ─────────────────────────────────────────────────────────
        BUILDER.push("Lightning Control");

        LIGHTNING_CONTROL_ENABLED = BUILDER
                .comment("Enable custom lightning frequency control during thunderstorms.")
                .define("lightningControlEnabled", true);

        LIGHTNING_FREQUENCY_MULTIPLIER = BUILDER
                .comment("Multiplier applied to vanilla lightning spawn rate.",
                        "1.0 = vanilla, 2.0 = twice as many, 0.5 = half as many, 0.0 = none.")
                .defineInRange("lightningFrequencyMultiplier", 1.5, 0.0, 20.0);

        LIGHTNING_SPAWN_RADIUS = BUILDER
                .comment("Radius (blocks) around each online player where extra lightning can spawn.",
                        "Used when frequencyMultiplier > 1.0 to place additional bolts.")
                .defineInRange("lightningSpawnRadius", 128, 16, 512);

        BUILDER.pop();

        // ── Camera Shake ──────────────────────────────────────────────────────
        BUILDER.push("Lightning Camera Shake");

        LIGHTNING_SHAKE_ENABLED = BUILDER
                .comment("Shake the player's camera when lightning strikes nearby.")
                .define("lightningShakeEnabled", true);

        LIGHTNING_SHAKE_MAX_DISTANCE = BUILDER
                .comment("Maximum distance (blocks) at which a lightning strike causes camera shake.",
                        "Beyond this distance: no effect.")
                .defineInRange("lightningShakeMaxDistance", 100.0, 10.0, 500.0);

        LIGHTNING_SHAKE_MAX_INTENSITY = BUILDER
                .comment("Maximum shake intensity (at direct proximity). Range 0.0 - 1.0.",
                        "0.0 = no shake, 1.0 = very strong shake.")
                .defineInRange("lightningShakeMaxIntensity", 0.6, 0.0, 1.0);

        LIGHTNING_SHAKE_DURATION_SECONDS = BUILDER
                .comment("Duration of the camera shake in seconds after a lightning strike.")
                .defineInRange("lightningShakeDurationSeconds", 0.8, 0.1, 5.0);

        ALLOW_CLIENT_SHAKE_OVERRIDE = BUILDER
                .comment("If true, clients can disable or reduce camera shake via their own client config.",
                        "If false, server settings are enforced.")
                .define("allowClientShakeOverride", true);

        BUILDER.pop();

        // ── Auto Cycle ────────────────────────────────────────────────────────
        BUILDER.push("Weather Auto Cycle");

        WEATHER_CYCLE_ENABLED = BUILDER
                .comment("Enable automatic weather cycling.")
                .define("weatherCycleEnabled", false);

        WEATHER_CYCLE_STEPS = BUILDER
                .comment(
                        "Steps of the automatic weather cycle.",
                        "Format : weatherType;durationMinutes[;broadcastMessage]",
                        "  weatherType : CLEAR | RAIN | THUNDER",
                        "  duration    : real-time minutes for this step",
                        "  message     : optional narrative message broadcast when step starts",
                        "",
                        "Example:",
                        "  CLEAR;30;§7Le ciel se dégage...",
                        "  RAIN;15",
                        "  THUNDER;5;§8Un orage gronde au loin."
                )
                .defineList("weatherCycleSteps",
                        Arrays.asList("CLEAR;30", "RAIN;15", "THUNDER;5"),
                        obj -> obj instanceof String s && s.split(";").length >= 2
                );

        BUILDER.pop();

        // ── Messages ──────────────────────────────────────────────────────────
        BUILDER.push("Weather Messages");

        WEATHER_MESSAGES_ENABLED = BUILDER
                .comment("Broadcast a narrative message when weather changes naturally.")
                .define("weatherMessagesEnabled", false);

        MSG_WEATHER_CLEAR = BUILDER
                .comment("Message broadcast when weather clears.")
                .define("msgWeatherClear", "§7The sky is clearing...");

        MSG_WEATHER_RAIN = BUILDER
                .comment("Message broadcast when it starts raining.")
                .define("msgWeatherRain", "§8Rain begins to fall.");

        MSG_WEATHER_THUNDER = BUILDER
                .comment("Message broadcast when a thunderstorm begins.")
                .define("msgWeatherThunder", "§8A storm is coming...");

        WEATHER_MESSAGE_MODE = BUILDER
                .comment("Display mode for weather messages: CHAT, ACTION_BAR, IMMERSIVE.")
                .define("weatherMessageMode", "ACTION_BAR");

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    // =========================================================================
    // PARSED RECORDS
    // =========================================================================

    public enum WeatherType { CLEAR, RAIN, THUNDER }

    public record WeatherFogPreset(WeatherType weather, String presetName) {
        public static WeatherFogPreset parse(String raw) {
            String[] p = raw.split(";");
            return new WeatherFogPreset(WeatherType.valueOf(p[0].trim().toUpperCase()), p[1].trim());
        }
    }

    public record CycleStep(WeatherType weather, int durationMinutes, String message) {
        public static CycleStep parse(String raw) {
            String[] p = raw.split(";");
            String msg = p.length >= 3 ? p[2].trim() : "";
            return new CycleStep(WeatherType.valueOf(p[0].trim().toUpperCase()),
                    Integer.parseInt(p[1].trim()), msg);
        }
    }
}
