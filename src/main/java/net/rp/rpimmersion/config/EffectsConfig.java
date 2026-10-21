package net.rp.rpimmersion.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EffectsConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_EFFECTS;

    public static final ModConfigSpec.BooleanValue TEMPERATURE_ENABLED;
    public static final ModConfigSpec.DoubleValue COLD_THRESHOLD;
    public static final ModConfigSpec.DoubleValue HOT_THRESHOLD;
    public static final ModConfigSpec.DoubleValue COLD_TINT;
    public static final ModConfigSpec.DoubleValue HOT_TINT;
    public static final ModConfigSpec.DoubleValue COLD_FOV_REDUCTION;
    public static final ModConfigSpec.DoubleValue HAND_TREMBLE;
    public static final ModConfigSpec.BooleanValue COLD_BREATH;

    public static final ModConfigSpec.BooleanValue LOW_HEALTH_ENABLED;
    public static final ModConfigSpec.DoubleValue LOW_HEALTH_THRESHOLD;
    public static final ModConfigSpec.DoubleValue LOW_HEALTH_VIGNETTE;
    public static final ModConfigSpec.DoubleValue LOW_HEALTH_CAMERA;
    public static final ModConfigSpec.DoubleValue LOW_HEALTH_SLOW;

    public static final ModConfigSpec.BooleanValue FATIGUE_ENABLED;
    public static final ModConfigSpec.IntValue FATIGUE_START_MINUTES;
    public static final ModConfigSpec.IntValue FATIGUE_FULL_MINUTES;
    public static final ModConfigSpec.DoubleValue FATIGUE_SLOW;
    public static final ModConfigSpec.DoubleValue FATIGUE_WEAKNESS;
    public static final ModConfigSpec.DoubleValue FATIGUE_VIGNETTE;
    public static final ModConfigSpec.BooleanValue FATIGUE_BLINK;

    public static final ModConfigSpec.BooleanValue RESPAWN_ENABLED;
    public static final ModConfigSpec.IntValue RESPAWN_IMMOBILE_SECONDS;
    public static final ModConfigSpec.IntValue RESPAWN_WEAK_SECONDS;
    public static final ModConfigSpec.DoubleValue RESPAWN_SLOW;

    public static final ModConfigSpec.BooleanValue EXERTION_ENABLED;
    public static final ModConfigSpec.IntValue SPRINT_EXHAUST_SECONDS;
    public static final ModConfigSpec.IntValue EXERTION_RECOVER_SECONDS;
    public static final ModConfigSpec.DoubleValue JUMP_EXERTION;
    public static final ModConfigSpec.DoubleValue EXHAUSTED_SLOW;
    public static final ModConfigSpec.DoubleValue EXHAUSTED_WEAKNESS;
    public static final ModConfigSpec.DoubleValue BREATH_THRESHOLD;

    public static final ModConfigSpec.BooleanValue STUMBLE_ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> STUMBLE_BLOCKS;
    public static final ModConfigSpec.DoubleValue STUMBLE_CHANCE;
    public static final ModConfigSpec.DoubleValue STUMBLE_INTENSITY;
    public static final ModConfigSpec.BooleanValue FALL_CAMERA_ENABLED;
    public static final ModConfigSpec.DoubleValue FALL_MIN_DISTANCE;
    public static final ModConfigSpec.DoubleValue FALL_CAMERA_INTENSITY;

    public static final ModConfigSpec.BooleanValue HIDE_POTION_PARTICLES;
    public static final ModConfigSpec.BooleanValue ALLOW_CLIENT_EFFECTS_OVERRIDE;

    private static ModConfigSpec.BooleanValue bool(String key, boolean def, String comment) {
        return BUILDER.comment(comment).define(key, def);
    }

    private static ModConfigSpec.DoubleValue dbl(String key, double def, double min, double max, String comment) {
        return BUILDER.comment(comment).defineInRange(key, def, min, max);
    }

    private static ModConfigSpec.IntValue num(String key, int def, int min, int max, String comment) {
        return BUILDER.comment(comment).defineInRange(key, def, min, max);
    }

    static {
        BUILDER.push("Effects System");
        ENABLE_EFFECTS = bool("enableEffects", true, "Master switch for every player state and sensory effect of this file.");
        BUILDER.pop();

        BUILDER.push("Temperature");
        TEMPERATURE_ENABLED = bool("temperatureEnabled", true,
                "Visual feedback of the biome temperature: tint, field of view, shivering hands, breath.");
        COLD_THRESHOLD = dbl("coldThreshold", 0.15, -2.0, 2.0,
                "Biome base temperature under which the cold effects start. Snowy biomes are 0.0 or less.");
        HOT_THRESHOLD = dbl("hotThreshold", 1.5, 0.0, 2.5,
                "Biome base temperature over which the heat effects start. Desert, badlands, savanna and the Nether are 2.0.");
        COLD_TINT = dbl("coldTint", 0.10, 0.0, 0.5, "Opacity of the cold tint at full intensity.");
        HOT_TINT = dbl("hotTint", 0.08, 0.0, 0.5, "Opacity of the heat tint at full intensity.");
        COLD_FOV_REDUCTION = dbl("coldFovReduction", 0.04, 0.0, 0.3, "Field of view reduction in the cold (0.04 = 4%).");
        HAND_TREMBLE = dbl("handTremble", 0.6, 0.0, 3.0, "Hand shivering strength in the cold. 0 disables it.");
        COLD_BREATH = bool("visibleBreathInCold", true, "Show breath clouds in cold biomes.");
        BUILDER.pop();

        BUILDER.push("Low Health");
        LOW_HEALTH_ENABLED = bool("lowHealthEnabled", true,
                "Breathing camera, red vignette and slower movement when health is low.");
        LOW_HEALTH_THRESHOLD = dbl("lowHealthThreshold", 0.35, 0.05, 1.0,
                "Fraction of max health under which the effects start. Intensity grows as health drops.");
        LOW_HEALTH_VIGNETTE = dbl("lowHealthVignette", 0.55, 0.0, 1.0, "Opacity of the red vignette at full intensity.");
        LOW_HEALTH_CAMERA = dbl("lowHealthCamera", 0.6, 0.0, 3.0, "Strength of the breathing camera sway. 0 disables it.");
        LOW_HEALTH_SLOW = dbl("lowHealthSlow", 0.10, 0.0, 0.9, "Movement speed reduction at full intensity (0.10 = 10%).");
        BUILDER.pop();

        BUILDER.push("Fatigue");
        FATIGUE_ENABLED = bool("fatigueEnabled", true,
                "Fatigue when a player stays awake too long. Uses the vanilla 'time since last rest' statistic (reset by sleeping or dying).");
        FATIGUE_START_MINUTES = num("fatigueStartMinutes", 90, 1, 1440, "Played minutes without sleeping before fatigue starts.");
        FATIGUE_FULL_MINUTES = num("fatigueFullMinutes", 180, 2, 2880, "Played minutes without sleeping at which fatigue is at full intensity.");
        FATIGUE_SLOW = dbl("fatigueSlow", 0.12, 0.0, 0.9, "Movement speed reduction at full fatigue.");
        FATIGUE_WEAKNESS = dbl("fatigueWeakness", 0.10, 0.0, 0.9, "Attack damage reduction at full fatigue.");
        FATIGUE_VIGNETTE = dbl("fatigueVignette", 0.45, 0.0, 1.0, "Opacity of the dark vignette at full fatigue.");
        FATIGUE_BLINK = bool("fatigueBlink", true, "Occasional micro sleeps (screen briefly goes dark) at high fatigue.");
        BUILDER.pop();

        BUILDER.push("Respawn");
        RESPAWN_ENABLED = bool("respawnEnabled", true, "Players wake up immobile then weakened after dying.");
        RESPAWN_IMMOBILE_SECONDS = num("respawnImmobileSeconds", 3, 0, 30,
                "Seconds the player cannot move or jump after respawning. The screen fades in from black during this time.");
        RESPAWN_WEAK_SECONDS = num("respawnWeakSeconds", 45, 0, 600, "Seconds of progressively fading weakness after the immobile phase.");
        RESPAWN_SLOW = dbl("respawnSlow", 0.25, 0.0, 0.9, "Movement speed reduction at the start of the weakness phase.");
        BUILDER.pop();

        BUILDER.push("Exertion");
        EXERTION_ENABLED = bool("exertionEnabled", true,
                "Long sprints and sprint jumps (bunny hopping) build up exhaustion: visible breath, then slowness and weakness.");
        SPRINT_EXHAUST_SECONDS = num("sprintExhaustSeconds", 60, 5, 600, "Seconds of continuous sprint to become exhausted.");
        EXERTION_RECOVER_SECONDS = num("exertionRecoverSeconds", 25, 5, 600, "Seconds without sprinting to fully recover.");
        JUMP_EXERTION = dbl("jumpExertion", 0.03, 0.0, 0.5,
                "Exhaustion added by each sprint jump (1.0 = exhausted). Raise it to punish bunny hopping harder.");
        EXHAUSTED_SLOW = dbl("exhaustedSlow", 0.30, 0.0, 0.9, "Movement speed reduction while exhausted.");
        EXHAUSTED_WEAKNESS = dbl("exhaustedWeakness", 0.20, 0.0, 0.9, "Attack damage reduction while exhausted.");
        BREATH_THRESHOLD = dbl("breathThreshold", 0.4, 0.0, 1.0, "Exhaustion level from which the breath becomes visible and the camera heaves.");
        BUILDER.pop();

        BUILDER.push("Micro Animations");
        STUMBLE_ENABLED = bool("stumbleEnabled", false, "Players occasionally stumble (camera dip) when moving on soft blocks.");
        STUMBLE_BLOCKS = BUILDER
                .comment("Blocks that make players stumble. Wildcards supported (minecraft:*_snow).")
                .defineList("stumbleBlocks",
                        Arrays.asList("minecraft:snow", "minecraft:snow_block", "minecraft:powder_snow", "minecraft:mud"),
                        obj -> obj instanceof String);
        STUMBLE_CHANCE = dbl("stumbleChancePerSecond", 0.08, 0.0, 2.0, "Chance per second to stumble while moving (doubled when sprinting).");
        STUMBLE_INTENSITY = dbl("stumbleIntensity", 0.5, 0.0, 3.0, "Strength of the stumble camera dip.");
        FALL_CAMERA_ENABLED = bool("fallCameraEnabled", true, "Damped camera impact after a fall.");
        FALL_MIN_DISTANCE = dbl("fallMinDistance", 3.0, 1.0, 50.0, "Minimum fall distance (blocks) to trigger the camera impact.");
        FALL_CAMERA_INTENSITY = dbl("fallCameraIntensity", 0.5, 0.0, 3.0, "Strength of the camera impact after a fall.");
        BUILDER.pop();

        BUILDER.push("Potion Particles");
        HIDE_POTION_PARTICLES = bool("hidePotionParticles", false,
                "Hide the particles of every potion effect for everyone. Applies as effects are applied or refreshed.");
        BUILDER.pop();

        BUILDER.push("Client Override");
        ALLOW_CLIENT_EFFECTS_OVERRIDE = bool("allowClientEffectsOverride", true,
                "If true, clients can reduce or disable camera movement and screen overlays in their own client config.");
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}