package net.rp.rpimmersion.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class HudConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    /** Identifiants acceptes pour hiddenElements. */
    public static final List<String> ELEMENTS = List.of(
            "crosshair", "hotbar", "health", "armor", "food", "air", "experience", "jump_bar",
            "vehicle_health", "item_name", "effects", "boss_bars", "scoreboard", "tab_list",
            "debug", "subtitles");

    public static final ModConfigSpec.BooleanValue ENABLE_HUD_CONTROL;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> HIDDEN_ELEMENTS;
    public static final ModConfigSpec.BooleanValue EXEMPT_STAFF;
    public static final ModConfigSpec.BooleanValue FORCE_FIRST_PERSON;

    static {
        BUILDER.push("Hud Control");

        ENABLE_HUD_CONTROL = BUILDER
                .comment("Master switch for server-controlled HUD hiding.",
                        "When false, hiddenElements is ignored and every player sees the full vanilla HUD.")
                .define("enableHudControl", false);

        HIDDEN_ELEMENTS = BUILDER
                .comment("HUD elements hidden on the client of every player.",
                        "Available elements: crosshair, hotbar, health, armor, food, air, experience, jump_bar,",
                        "vehicle_health, item_name, effects, boss_bars, scoreboard, tab_list, debug, subtitles",
                        "'debug' hides the F3 screen (coordinates, biome, etc.).",
                        "Example: [\"crosshair\", \"debug\", \"experience\"]")
                .defineList("hiddenElements", List.of(), obj -> obj instanceof String);

        EXEMPT_STAFF = BUILDER
                .comment("If true, staff members (as defined by RpEssentials) always see the full HUD",
                        "and are never locked in first person.")
                .define("exemptStaff", false);

        BUILDER.pop();

        BUILDER.push("Camera");

        FORCE_FIRST_PERSON = BUILDER
                .comment("If true, players are locked in first person view (the F5 key is ignored).")
                .define("forceFirstPerson", false);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
