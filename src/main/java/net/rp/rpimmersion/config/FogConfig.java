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

    // =========================================================================
    // GLOBAL FOG
    // =========================================================================
    public static final ModConfigSpec.BooleanValue GLOBAL_FOG_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> GLOBAL_FOG_PRESET;

    // =========================================================================
    // PRIORITY
    // =========================================================================
    public static final ModConfigSpec.BooleanValue ZONE_PRIORITY_OVER_GLOBAL;

    // =========================================================================
    // PRESETS
    // Format : name;red;green;blue;start;end;transitionSeconds
    // =========================================================================
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FOG_PRESETS;

    // =========================================================================
    // ZONES RPIMMERSION (cercles custom)
    // Format : zoneName;centerX;centerZ;radius;presetName;priority
    // =========================================================================
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FOG_ZONES;

    // =========================================================================
    // BIOMES
    // Format : biomeId;presetName;priority
    // Supports wildcards : minecraft:* ou *:dark_*
    // =========================================================================
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FOG_BIOMES;

    // =========================================================================
    // OVERRIDES RPESSENTIALS
    // Format : rpEssentialsZoneName;presetName;priority
    // =========================================================================
    public static final ModConfigSpec.ConfigValue<List<? extends String>> RPESSENTIALS_ZONE_OVERRIDES;

    // =========================================================================
    // STATIC INIT
    // =========================================================================
    static {
        BUILDER.push("Fog System");

        ENABLE_FOG_SYSTEM = BUILDER
                .comment("Master switch for the RPImmersion fog system.")
                .define("enableFogSystem", true);

        BUILDER.pop();

        // ── Global Fog ────────────────────────────────────────────────────────
        BUILDER.push("Global Fog");

        GLOBAL_FOG_ENABLED = BUILDER
                .comment("If true, applies the global fog preset to all players",
                        "(unless overridden by a zone or biome).")
                .define("globalFogEnabled", false);

        GLOBAL_FOG_PRESET = BUILDER
                .comment("Name of the fog preset to apply globally.",
                        "Must match a preset name defined in [Fog Presets].")
                .define("globalFogPreset", "");

        ZONE_PRIORITY_OVER_GLOBAL = BUILDER
                .comment("If true, zone/biome fog always overrides the global fog.",
                        "If false, global wins over zones and biomes.")
                .define("zonePriorityOverGlobal", true);

        BUILDER.pop();

        // ── Presets ───────────────────────────────────────────────────────────
        BUILDER.push("Fog Presets");

        FOG_PRESETS = BUILDER
                .comment(
                        "Named fog presets referenced by zones, biomes and commands.",
                        "Format : name;red;green;blue;start;end;transitionSeconds",
                        "  name              : identifier (used in zones/biomes/commands)",
                        "  red/green/blue    : fog color, range 0.0 - 1.0",
                        "  start             : distance (blocks) where fog begins",
                        "  end               : distance (blocks) where fog is fully opaque",
                        "  transitionSeconds : smooth fade duration when switching presets",
                        "",
                        "Example: night_fog;0.05;0.05;0.1;5.0;30.0;3.0"
                )
                .defineList("presets",
                        Arrays.asList(
                                "night_fog;0.05;0.05;0.1;5.0;30.0;3.0",
                                "morning_mist;0.7;0.75;0.8;2.0;20.0;5.0",
                                "cursed_forest;0.1;0.05;0.0;3.0;15.0;4.0",
                                "purge;0.3;0.0;0.0;1.0;10.0;2.0",
                                "thick_swamp;0.2;0.25;0.15;1.0;12.0;6.0"
                        ),
                        obj -> obj instanceof String s && s.split(";").length >= 7
                );

        BUILDER.pop();

        // ── Zones ─────────────────────────────────────────────────────────────
        BUILDER.push("Fog Zones");

        FOG_ZONES = BUILDER
                .comment(
                        "Circular fog zones defined directly in RPImmersion.",
                        "Format : zoneName;centerX;centerZ;radius;presetName;priority",
                        "  priority : integer — higher = wins over lower priority zones.",
                        "             Zone always beats biome at equal priority.",
                        "",
                        "Example: dark_forest_zone;100;-200;80;cursed_forest;10"
                )
                .defineList("zones",
                        List.of(),
                        obj -> obj instanceof String s && s.split(";").length >= 5
                );

        RPESSENTIALS_ZONE_OVERRIDES = BUILDER
                .comment(
                        "Apply a fog preset to zones already defined in RpEssentials (namedZones).",
                        "Format : rpEssentialsZoneName;presetName;priority",
                        "",
                        "Example: taverne;morning_mist;5"
                )
                .defineList("rpEssentialsZoneOverrides",
                        List.of(),
                        obj -> obj instanceof String s && s.split(";").length >= 3
                );

        BUILDER.pop();

        // ── Biomes ────────────────────────────────────────────────────────────
        BUILDER.push("Fog Biomes");

        FOG_BIOMES = BUILDER
                .comment(
                        "Apply a fog preset based on the biome the player is standing in.",
                        "Format : biomeId;presetName;priority",
                        "  biomeId  : Minecraft resource location (e.g. minecraft:dark_forest)",
                        "             Supports wildcards: minecraft:* or *:dark_*",
                        "  priority : integer — higher = wins. Zones win over biomes at equal priority.",
                        "",
                        "Examples:",
                        "  minecraft:dark_forest;cursed_forest;5",
                        "  minecraft:swamp;thick_swamp;3",
                        "  minecraft:*_forest;morning_mist;1"
                )
                .defineList("biomes",
                        Arrays.asList(
                                "minecraft:dark_forest;cursed_forest;5",
                                "minecraft:swamp;thick_swamp;3"
                        ),
                        obj -> obj instanceof String s && s.split(";").length >= 3
                );

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    // =========================================================================
    // PARSED RECORD TYPES
    // =========================================================================

    public record FogPreset(
            String name,
            float red, float green, float blue,
            float start, float end,
            float transitionSeconds
    ) {
        public static FogPreset parse(String raw) {
            String[] p = raw.split(";");
            return new FogPreset(
                    p[0].trim(),
                    Float.parseFloat(p[1].trim()),
                    Float.parseFloat(p[2].trim()),
                    Float.parseFloat(p[3].trim()),
                    Float.parseFloat(p[4].trim()),
                    Float.parseFloat(p[5].trim()),
                    Float.parseFloat(p[6].trim())
            );
        }
    }

    public record FogZone(
            String name,
            double centerX, double centerZ,
            double radius,
            String presetName,
            int priority
    ) {
        public static FogZone parse(String raw) {
            String[] p = raw.split(";");
            int prio = p.length >= 6 ? Integer.parseInt(p[5].trim()) : 0;
            return new FogZone(
                    p[0].trim(),
                    Double.parseDouble(p[1].trim()),
                    Double.parseDouble(p[2].trim()),
                    Double.parseDouble(p[3].trim()),
                    p[4].trim(),
                    prio
            );
        }

        public boolean contains(double x, double z) {
            double dx = x - centerX;
            double dz = z - centerZ;
            return dx * dx + dz * dz <= radius * radius;
        }
    }

    public record FogBiome(
            String biomePattern,
            String presetName,
            int priority
    ) {
        public static FogBiome parse(String raw) {
            String[] p = raw.split(";");
            return new FogBiome(
                    p[0].trim(), p[1].trim(),
                    Integer.parseInt(p[2].trim())
            );
        }
    }

    public record RpEssentialsZoneOverride(
            String zoneName,
            String presetName,
            int priority
    ) {
        public static RpEssentialsZoneOverride parse(String raw) {
            String[] p = raw.split(";");
            return new RpEssentialsZoneOverride(
                    p[0].trim(), p[1].trim(),
                    Integer.parseInt(p[2].trim())
            );
        }
    }
}