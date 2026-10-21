package net.rp.rpimmersion.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;

public class EventsConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_EVENTS;
    public static final ModConfigSpec.ConfigValue<String> EVENT_MESSAGE_MODE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EVENTS;
    public static final ModConfigSpec.BooleanValue RANDOM_EVENTS_ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> RANDOM_EVENTS;
    public static final ModConfigSpec.IntValue RANDOM_COOLDOWN_MINUTES;

    static {
        BUILDER.push("Atmospheric Events");

        ENABLE_EVENTS = BUILDER
                .comment("Master switch for atmospheric events (commands and random occurrences).")
                .define("enableEvents", true);

        EVENT_MESSAGE_MODE = BUILDER
                .comment("Display mode for event messages: CHAT, ACTION_BAR, IMMERSIVE.")
                .define("eventMessageMode", "ACTION_BAR");

        BUILDER.pop();

        BUILDER.push("Event Definitions");

        EVENTS = BUILDER
                .comment(
                        "Format : id;minutes;fogPreset;weather;tintRGB;tintOpacity;darkness;transitionSeconds;shake;particle[;startMessage;endMessage]",
                        "  minutes     : real-time duration",
                        "  fogPreset   : preset from rpimmersion-fog.toml, empty for none",
                        "  weather     : CLEAR, RAIN, THUNDER, or NONE to leave the weather untouched",
                        "  tintRGB     : screen tint color in hex (RRGGBB)",
                        "  tintOpacity : screen tint opacity, 0.0 - 1.0",
                        "  darkness    : extra black overlay opacity, 0.0 - 1.0",
                        "  transition  : seconds to fade the tint and darkness in and out",
                        "  shake       : camera shake intensity at the start, 0.0 - 1.0 (0 = none)",
                        "  particle    : ambient particle around players (e.g. white_ash), empty for none",
                        "Start one with /rpimmersion event start <id> [minutes]")
                .defineList("events",
                        Arrays.asList(
                                "bloodmoon;20;blood_sky;CLEAR;8B0000;0.20;0.10;8;0.15;;&4The moon turns blood red...;&7The moon slowly regains its color.",
                                "storm_omen;12;green_sky;THUNDER;3C7A3C;0.12;0.10;10;0.0;;&2The sky turns a sickly green...;&7The air feels lighter.",
                                "ash_rain;15;ash_fall;RAIN;6B6B6B;0.15;0.12;8;0.0;white_ash;&8Ashes fall from the sky.;&7The ashes stop falling.",
                                "dense_fog;20;dense_fog;CLEAR;A0A8B0;0.05;0.0;10;0.0;;&7A thick fog rolls in...;&7The fog lifts.",
                                "silent_sky;5;;CLEAR;000000;0.0;0.30;15;0.0;;&8Everything falls silent.;"
                        ),
                        obj -> obj instanceof String s && s.split(";").length >= 10);

        BUILDER.pop();

        BUILDER.push("Random Events");

        RANDOM_EVENTS_ENABLED = BUILDER
                .comment("Let events start by themselves, rarely, while players are online.")
                .define("randomEventsEnabled", false);

        RANDOM_EVENTS = BUILDER
                .comment(
                        "Format : eventId;chancePerMinute;time",
                        "  chancePerMinute : probability (0.0 - 1.0) rolled once per real minute. 0.004 is about once every 4 hours.",
                        "  time            : DAY, NIGHT or ANY (Minecraft time, thresholds from rpimmersion-fog.toml)")
                .defineList("randomEvents",
                        Arrays.asList(
                                "bloodmoon;0.004;NIGHT",
                                "storm_omen;0.003;NIGHT",
                                "ash_rain;0.003;ANY",
                                "dense_fog;0.006;ANY"
                        ),
                        obj -> obj instanceof String s && s.split(";").length >= 3);

        RANDOM_COOLDOWN_MINUTES = BUILDER
                .comment("Minimum real minutes between the end of an event and the next random one.")
                .defineInRange("randomCooldownMinutes", 60, 0, 1440);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public record EventDef(String id, int minutes, String fogPreset, String weather, int rgb,
                           float opacity, float darkness, float transitionSeconds, float shake,
                           String particle, String startMessage, String endMessage) {
        public static EventDef parse(String raw) {
            String[] p = raw.split(";");
            return new EventDef(p[0].trim().toLowerCase(), Integer.parseInt(p[1].trim()), p[2].trim(),
                    p[3].trim().toUpperCase(), Integer.parseInt(p[4].trim().replace("#", ""), 16),
                    Float.parseFloat(p[5].trim()), Float.parseFloat(p[6].trim()),
                    Float.parseFloat(p[7].trim()), Float.parseFloat(p[8].trim()),
                    p[9].trim(),
                    p.length > 10 ? p[10].trim() : "",
                    p.length > 11 ? p[11].trim() : "");
        }
    }

    public record RandomEntry(String eventId, double chance, String time) {
        public static RandomEntry parse(String raw) {
            String[] p = raw.split(";");
            return new RandomEntry(p[0].trim().toLowerCase(), Double.parseDouble(p[1].trim()), p[2].trim().toUpperCase());
        }
    }
}