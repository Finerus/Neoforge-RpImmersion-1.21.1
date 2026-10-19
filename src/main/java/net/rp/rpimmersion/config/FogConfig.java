package net.rp.rpimmersion.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;

public class FogConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // =========================================================================
    // GENERAL
    // =========================================================================
    public static final ModConfigSpec.BooleanValue ENABLE_FOG_SYSTEM;
    public static final ModConfigSpec.BooleanValue ALLOW_CLIENT_FOG_OVERRIDE;

    // =========================================================================
    // GLOBAL FOG
    // =========================================================================
    public static final ModConfigSpec.BooleanValue  GLOBAL_FOG_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> GLOBAL_FOG_PRESET;
    public static final ModConfigSpec.ConfigValue<String> GLOBAL_FOG_DAY_PRESET;
    public static final ModConfigSpec.ConfigValue<String> GLOBAL_FOG_NIGHT_PRESET;
    public static final ModConfigSpec.BooleanValue  ZONE_PRIORITY_OVER_GLOBAL;

    // =========================================================================
    // WORLD BORDER FOG
    // =========================================================================
    public static final ModConfigSpec.BooleanValue  WORLD_BORDER_FOG_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> WORLD_BORDER_FOG_PRESET;
    /** Priority of world border fog. Usually very high to override everything. */
    public static final ModConfigSpec.IntValue      WORLD_BORDER_FOG_PRIORITY;


    // =========================================================================
    // FOG DIMENSIONS
    // =========================================================================
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FOG_DIMENSIONS;

    // =========================================================================
    // DAY/NIGHT THRESHOLDS
    // =========================================================================
    /** Minecraft day time (0-24000) at which "day" starts. Default: 1000 (sunrise). */
    public static final ModConfigSpec.IntValue DAY_START_TICK;
    /** Minecraft day time at which "night" starts. Default: 13000 (sunset). */
    public static final ModConfigSpec.IntValue NIGHT_START_TICK;
    /** Transition duration (seconds) for day/night fog changes. */
    public static final ModConfigSpec.DoubleValue DAY_NIGHT_TRANSITION_SECONDS;

    // =========================================================================
    // PRESETS
    // Format : name;red;green;blue;start;end;transitionSeconds
    // =========================================================================
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FOG_PRESETS;

    // =========================================================================
    // ZONES
    // Format : zoneName;centerX;centerZ;radius;presetName;priority[;dayPreset;nightPreset]
    // =========================================================================
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FOG_ZONES;

    // =========================================================================
    // BIOMES
    // Format : biomeId;presetName;priority[;dayPreset;nightPreset]
    // Wildcards supportés : minecraft:* ou *:dark_*
    // =========================================================================
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FOG_BIOMES;

    // =========================================================================
    // RPESSENTIALS ZONE OVERRIDES
    // Format : rpEssentialsZoneName;presetName;priority[;dayPreset;nightPreset]
    // =========================================================================
    public static final ModConfigSpec.ConfigValue<List<? extends String>> RPESSENTIALS_ZONE_OVERRIDES;

    // =========================================================================
    // OPTIMIZATION
    // =========================================================================
    /** Interval in ticks between zone/biome checks per player. Default 60 = 3s. */
    public static final ModConfigSpec.IntValue ZONE_CHECK_INTERVAL_TICKS;

    // =========================================================================
    // STATIC INIT
    // =========================================================================
    static {
        BUILDER.push("Fog System");
        ENABLE_FOG_SYSTEM = BUILDER
                .comment("Master switch for the RPImmersion fog system.")
                .define("enableFogSystem", true);
        ALLOW_CLIENT_FOG_OVERRIDE = BUILDER
                .comment("If true, clients can disable the server-controlled fog from their own client config.",
                        "If false, the server fog is always enforced.")
                .define("allowClientFogOverride", false);
        BUILDER.pop();

        // ── Global Fog ────────────────────────────────────────────────────────
        BUILDER.push("Global Fog");

        GLOBAL_FOG_ENABLED = BUILDER
                .comment("Apply a fog preset to all players (unless overridden by zone/biome).")
                .define("globalFogEnabled", false);
        GLOBAL_FOG_PRESET = BUILDER
                .comment("Fog preset applied globally at all times if no day/night preset is set.")
                .define("globalFogPreset", "");
        GLOBAL_FOG_DAY_PRESET = BUILDER
                .comment("Fog preset for daytime globally. Overrides globalFogPreset during day.",
                        "Leave empty to use globalFogPreset for both day and night.")
                .define("globalFogDayPreset", "");
        GLOBAL_FOG_NIGHT_PRESET = BUILDER
                .comment("Fog preset for nighttime globally. Overrides globalFogPreset during night.")
                .define("globalFogNightPreset", "");

        ZONE_PRIORITY_OVER_GLOBAL = BUILDER
                .comment("If true: zone/biome fog beats global. If false: global wins.")
                .define("zonePriorityOverGlobal", true);

        BUILDER.pop();

        // ── World Border Fog ──────────────────────────────────────────────────
        BUILDER.push("World Border Fog");

        WORLD_BORDER_FOG_ENABLED = BUILDER
                .comment("Apply a special fog preset when a player is outside the world border.")
                .define("worldBorderFogEnabled", true);
        WORLD_BORDER_FOG_PRESET = BUILDER
                .comment("Fog preset applied outside the world border.")
                .define("worldBorderFogPreset", "border_void");
        WORLD_BORDER_FOG_PRIORITY = BUILDER
                .comment("Priority of the world border fog. Higher than all zones by default.")
                .defineInRange("worldBorderFogPriority", 9999, 0, Integer.MAX_VALUE);

        BUILDER.pop();

        // ── Day/Night ─────────────────────────────────────────────────────────
        BUILDER.push("Day Night Cycle");

        DAY_START_TICK = BUILDER
                .comment("Minecraft day time tick (0-24000) at which DAY begins.",
                        "Default 1000 = just after sunrise.")
                .defineInRange("dayStartTick", 1000, 0, 24000);
        NIGHT_START_TICK = BUILDER
                .comment("Minecraft day time tick at which NIGHT begins.",
                        "Default 13000 = around sunset.")
                .defineInRange("nightStartTick", 13000, 0, 24000);
        DAY_NIGHT_TRANSITION_SECONDS = BUILDER
                .comment("Duration (seconds) of the fog transition when day/night changes.",
                        "Set to 0 for instant switch.")
                .defineInRange("dayNightTransitionSeconds", 30.0, 0.0, 300.0);

        BUILDER.pop();

        // ── Presets ───────────────────────────────────────────────────────────
        BUILDER.push("Fog Presets");

        FOG_PRESETS = BUILDER
                .comment(
                        "Named fog presets.",
                        "Format : name;red;green;blue;start;end;transitionSeconds[;overrides]",
                        "  red/green/blue : fog color, 0.0 - 1.0",
                        "  start/end      : fog distance (in block)",
                        "  transition     : fade duration (in seconds)",
                        "  overrides      : fog type (optional).",
                        "                   Possible values : TERRAIN, SKY, WATER, LAVA, POWDER_SNOW, ALL",
                        "                   TERRAIN      = distance fog (overworld, nether, end, pluie)",
                        "                   SKY          = atmospheric fog derrière les blocs",
                        "                   WATER        = underwater fog",
                        "                   LAVA         = in-lava fog",
                        "                   POWDER_SNOW  = in-snow-powder fog",
                        "                   ALL          = all the type above",
                        "                   Default if missing : TERRAIN",
                        "  Full example : border_void;0;0;0;1;8;2;ALL",
                        "  Simple example  : night_fog;0.05;0.05;0.1;5;30;3"
                )
                .defineList("presets",
                        Arrays.asList(
                                "night_fog;0.05;0.05;0.1;5.0;30.0;3.0",
                                "morning_mist;0.7;0.75;0.8;2.0;20.0;5.0",
                                "cursed_forest;0.1;0.05;0.0;3.0;15.0;4.0",
                                "purge;0.3;0.0;0.0;1.0;10.0;2.0",
                                "thick_swamp;0.2;0.25;0.15;1.0;12.0;6.0",
                                "border_void;0.0;0.0;0.0;1.0;8.0;2.0;ALL"
                        ),
                        obj -> obj instanceof String s && s.split(";").length >= 7
                );

        BUILDER.pop();

        // ── Zones ─────────────────────────────────────────────────────────────
        BUILDER.push("Fog Zones");

        FOG_ZONES = BUILDER
                .comment(
                        "Circular fog zones. Format : zoneName;centerX;centerZ;radius;presetName;priority[;dayPreset;nightPreset[;dimension]]",
                        "  priority   : higher = overrides lower priority zones",
                        "  dayPreset  : optional, overrides presetName during day",
                        "  nightPreset: optional, overrides presetName during night",
                        "  dimension  : optional, restricts the zone to one dimension (e.g. minecraft:overworld)",
                        "Example: dark_forest;100;-200;80;cursed_forest;10;morning_mist;night_fog;minecraft:overworld"
                )
                .defineList("zones", List.of(),
                        obj -> obj instanceof String s && s.split(";").length >= 5);

        RPESSENTIALS_ZONE_OVERRIDES = BUILDER
                .comment(
                        "Apply fog to RpEssentials named zones. Format : zoneName;presetName;priority[;dayPreset;nightPreset]"
                )
                .defineList("rpEssentialsZoneOverrides", List.of(),
                        obj -> obj instanceof String s && s.split(";").length >= 3);

        BUILDER.pop();

        // ── Biomes ────────────────────────────────────────────────────────────
        BUILDER.push("Fog Biomes");

        FOG_BIOMES = BUILDER
                .comment(
                        "Biome-based fog. Format : biomeId;presetName;priority[;dayPreset;nightPreset]",
                        "  Wildcards: minecraft:* or *:dark_*",
                        "Example: minecraft:dark_forest;cursed_forest;5;morning_mist;night_fog"
                )
                .defineList("biomes",
                        Arrays.asList(
                                "minecraft:dark_forest;cursed_forest;5",
                                "minecraft:swamp;thick_swamp;3"
                        ),
                        obj -> obj instanceof String s && s.split(";").length >= 3);

        BUILDER.pop();

        // ── Dimensions ────────────────────────────────────────────────────────
        BUILDER.push("Fog Dimensions");

        FOG_DIMENSIONS = BUILDER
                .comment(
                        "Dimension-based fog. Format : dimensionId;presetName[;dayPreset;nightPreset]",
                        "Applied after zones, RpEssentials overrides and biomes, but before the global fog.",
                        "Example: minecraft:the_nether;thick_swamp"
                )
                .defineList("dimensions", List.of(),
                        obj -> obj instanceof String s && s.split(";").length >= 2);

        BUILDER.pop();

        // ── Optimization ──────────────────────────────────────────────────────
        BUILDER.push("Optimization");

        ZONE_CHECK_INTERVAL_TICKS = BUILDER
                .comment("How often (in ticks) the server checks each player's zone/biome.",
                        "Lower = more responsive, higher = better performance.",
                        "Default: 60 (3 seconds). Minimum: 20 (1 second).")
                .defineInRange("zoneCheckIntervalTicks", 60, 20, 1200);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    // =========================================================================
    // PARSED RECORDS
    // =========================================================================

    public record FogPreset(String name,
                            float red, float green, float blue,
                            float start, float end, float transitionSeconds,
                            int overrideFlags) {

        public static final int FLAG_TERRAIN     = 1;
        public static final int FLAG_SKY         = 1 << 1;
        public static final int FLAG_WATER       = 1 << 2;
        public static final int FLAG_LAVA        = 1 << 3;
        public static final int FLAG_POWDER_SNOW = 1 << 4;
        public static final int FLAG_ALL         = FLAG_TERRAIN | FLAG_SKY | FLAG_WATER | FLAG_LAVA | FLAG_POWDER_SNOW;

        public boolean overrides(int flag) { return (overrideFlags & flag) != 0; }

        public static int parseFlags(String raw) {
            if (raw == null || raw.isBlank()) return FLAG_TERRAIN;
            int flags = 0;
            for (String part : raw.toUpperCase().split(",")) {
                flags |= switch (part.trim()) {
                    case "TERRAIN"     -> FLAG_TERRAIN;
                    case "SKY"         -> FLAG_SKY;
                    case "WATER"       -> FLAG_WATER;
                    case "LAVA"        -> FLAG_LAVA;
                    case "POWDER_SNOW" -> FLAG_POWDER_SNOW;
                    case "ALL"         -> FLAG_ALL;
                    default            -> 0;
                };
            }
            return flags == 0 ? FLAG_TERRAIN : flags;
        }

        public static FogPreset parse(String raw) {
            String[] p = raw.split(";");
            int flags = p.length >= 8 ? parseFlags(p[7].trim()) : FLAG_TERRAIN;
            return new FogPreset(p[0].trim(),
                    Float.parseFloat(p[1].trim()), Float.parseFloat(p[2].trim()), Float.parseFloat(p[3].trim()),
                    Float.parseFloat(p[4].trim()), Float.parseFloat(p[5].trim()), Float.parseFloat(p[6].trim()),
                    flags);
        }
    }

    public record FogZone(String name, double centerX, double centerZ, double radius,
                          String presetName, int priority,
                          String dayPreset, String nightPreset, String dimension) {
        public static FogZone parse(String raw) {
            String[] p = raw.split(";");
            return new FogZone(p[0].trim(),
                    Double.parseDouble(p[1].trim()), Double.parseDouble(p[2].trim()),
                    Double.parseDouble(p[3].trim()),
                    p[4].trim(), p.length >= 6 ? Integer.parseInt(p[5].trim()) : 0,
                    p.length >= 7 ? p[6].trim() : "",
                    p.length >= 8 ? p[7].trim() : "",
                    p.length >= 9 ? p[8].trim() : "");
        }
        public boolean contains(double x, double z) {
            double dx = x - centerX, dz = z - centerZ;
            return dx * dx + dz * dz <= radius * radius;
        }
    }

    public record FogDimension(String dimension, String presetName, String dayPreset, String nightPreset) {
        public static FogDimension parse(String raw) {
            String[] p = raw.split(";");
            return new FogDimension(p[0].trim(), p[1].trim(),
                    p.length >= 3 ? p[2].trim() : "",
                    p.length >= 4 ? p[3].trim() : "");
        }
    }

    public record FogBiome(String biomePattern, String presetName, int priority,
                           String dayPreset, String nightPreset) {
        public static FogBiome parse(String raw) {
            String[] p = raw.split(";");
            return new FogBiome(p[0].trim(), p[1].trim(), Integer.parseInt(p[2].trim()),
                    p.length >= 4 ? p[3].trim() : "",
                    p.length >= 5 ? p[4].trim() : "");
        }
    }

    public record RpEssentialsZoneOverride(String zoneName, String presetName, int priority,
                                           String dayPreset, String nightPreset) {
        public static RpEssentialsZoneOverride parse(String raw) {
            String[] p = raw.split(";");
            return new RpEssentialsZoneOverride(p[0].trim(), p[1].trim(), Integer.parseInt(p[2].trim()),
                    p.length >= 4 ? p[3].trim() : "",
                    p.length >= 5 ? p[4].trim() : "");
        }
    }
}
