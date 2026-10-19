package net.rp.rpimmersion.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;

public class CalendarConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // =========================================================================
    // GENERAL
    // =========================================================================
    public static final ModConfigSpec.BooleanValue ENABLE_CALENDAR;
    public static final ModConfigSpec.BooleanValue PAUSE_WHEN_SERVER_CLOSED;
    public static final ModConfigSpec.BooleanValue SYNC_DAYLIGHT_CYCLE;
    public static final ModConfigSpec.BooleanValue MINECRAFT_DAY_SYNC;

    // =========================================================================
    // TIME RATIO
    // =========================================================================
    /** Nombre de ticks Minecraft (server) par minute RP. 1200 = 1 min réelle. */
    public static final ModConfigSpec.IntValue TICKS_PER_RP_MINUTE;
    /** Nombre de minutes RP par heure RP. */
    public static final ModConfigSpec.IntValue RP_MINUTES_PER_HOUR;
    /** Nombre d'heures RP par jour RP. */
    public static final ModConfigSpec.IntValue RP_HOURS_PER_DAY;

    // =========================================================================
    // CALENDAR STRUCTURE
    // =========================================================================
    /** Noms des mois (dans l'ordre). Détermine le nombre de mois. */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MONTH_NAMES;
    /** Nombre de jours par mois. Si un seul chiffre, appliqué à tous les mois. */
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> DAYS_PER_MONTH;
    /** Noms des jours de la semaine (optionnel — laissez vide pour désactiver). */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DAY_NAMES;

    // =========================================================================
    // START DATE
    // =========================================================================
    public static final ModConfigSpec.IntValue START_YEAR;
    public static final ModConfigSpec.IntValue START_MONTH;
    public static final ModConfigSpec.IntValue START_DAY;
    public static final ModConfigSpec.IntValue START_HOUR;

    // =========================================================================
    // DISPLAY
    // =========================================================================
    /** Mode d'affichage : HUD | ACTION_BAR | BOTH | NONE */
    public static final ModConfigSpec.ConfigValue<String> DISPLAY_MODE;
    /** Format de la date. Placeholders : {day} {dayName} {month} {monthName} {year} {hour} {minute} */
    public static final ModConfigSpec.ConfigValue<String> DATE_FORMAT;
    /** Format affiché quand le calendrier est en pause. */
    public static final ModConfigSpec.ConfigValue<String> PAUSED_SUFFIX;

    // =========================================================================
    // HUD & EVENTS
    // =========================================================================
    public static final ModConfigSpec.ConfigValue<String> HUD_POSITION;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;
    public static final ModConfigSpec.BooleanValue HUD_BACKGROUND;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EVENTS;
    public static final ModConfigSpec.ConfigValue<String> EVENT_MESSAGE_MODE;

    // =========================================================================
    // STATIC INIT
    // =========================================================================
    static {
        BUILDER.push("Calendar System");

        ENABLE_CALENDAR = BUILDER
                .comment("Enable the RPImmersion RP calendar.")
                .define("enableCalendar", true);

        PAUSE_WHEN_SERVER_CLOSED = BUILDER
                .comment("If true, the calendar automatically pauses when the server schedule (RpEssentials) is closed.",
                        "Resumes automatically when the server opens again.",
                        "Has no effect if RpEssentials is not present.")
                .define("pauseWhenServerClosed", true);

        SYNC_DAYLIGHT_CYCLE = BUILDER
                .comment("If true, the doDaylightCycle gamerule is automatically set to false when the calendar",
                        "is paused (manually or by schedule), and restored to true when it resumes.")
                .define("syncDaylightCycle", true);

        MINECRAFT_DAY_SYNC = BUILDER
                .comment("If true, the calendar follows the Minecraft day cycle.",
                        "  - The RP hour and minute are read from the Minecraft time of day.",
                        "  - The RP date advances by one day each time a Minecraft day completes (24 000 ticks).",
                        "  - Tick-based counting (ticksPerRpMinute) is disabled.",
                        "  - doDaylightCycle is managed on pause/resume, regardless of syncDaylightCycle.",
                        "  - /rpimmersion calendar set and advance also move the Minecraft time of day.")
                .define("minecraftDaySync", true);

        BUILDER.pop();

        // ── Time Ratio ────────────────────────────────────────────────────────
        BUILDER.push("Time Ratio");

        TICKS_PER_RP_MINUTE = BUILDER
                .comment("Server ticks per RP minute.",
                        "Default 20 × 50 min × 24 h = 24 000 ticks = 1 Minecraft day per calendar day.",
                        "1200 = 1 real minute per RP minute (1:1).",
                        "600  = 2 RP minutes per real minute.")
                .defineInRange("ticksPerRpMinute", 20, 1, 72000);

        RP_MINUTES_PER_HOUR = BUILDER
                .comment("RP minutes per RP hour. Default: 50 (combined with default ticksPerRpMinute gives exactly 1 MC day = 1 calendar day).")
                .defineInRange("rpMinutesPerHour", 50, 1, 3600);

        RP_HOURS_PER_DAY = BUILDER
                .comment("RP hours per RP day. Default: 24.")
                .defineInRange("rpHoursPerDay", 24, 1, 100);

        BUILDER.pop();

        // ── Calendar Structure ─────────────────────────────────────────────────
        BUILDER.push("Calendar Structure");

        MONTH_NAMES = BUILDER
                .comment("Names of the RP months, in order. The number of entries defines the number of months.")
                .defineList("monthNames",
                        Arrays.asList("January", "February", "March", "April", "May", "June",
                                "July", "August", "September", "October", "November", "December"),
                        obj -> obj instanceof String);

        DAYS_PER_MONTH = BUILDER
                .comment("Number of days per month. If one value: applied to all months.",
                        "If multiple values: one per month (must match monthNames count).")
                .defineList("daysPerMonth",
                        Arrays.asList(30),
                        obj -> obj instanceof Integer i && i > 0);

        DAY_NAMES = BUILDER
                .comment("Names of the days of the week (optional). Leave empty to disable week display.",
                        "Example: [\"Monday\", \"Tuesday\", \"Wednesday\", \"Thursday\", \"Friday\", \"Saturday\", \"Sunday\"]")
                .defineList("dayNames",
                        Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"),
                        obj -> obj instanceof String);

        BUILDER.pop();

        // ── Start Date ────────────────────────────────────────────────────────
        BUILDER.push("Start Date");

        START_YEAR  = BUILDER.comment("Starting year of the RP calendar.").defineInRange("startYear",  1, 0, Integer.MAX_VALUE);
        START_MONTH = BUILDER.comment("Starting month (1-based).").defineInRange("startMonth", 1, 1, 100);
        START_DAY   = BUILDER.comment("Starting day (1-based).").defineInRange("startDay",   1, 1, 365);
        START_HOUR  = BUILDER.comment("Starting hour (0-23).").defineInRange("startHour",  6, 0, 99);

        BUILDER.pop();

        // ── Display ───────────────────────────────────────────────────────────
        BUILDER.push("Display");

        DISPLAY_MODE = BUILDER
                .comment("Where to show the calendar.",
                        "HUD        : overlay on screen (see hudPosition, hudScale, hudBackground)",
                        "ACTION_BAR : Minecraft action bar (center bottom)",
                        "BOTH       : both",
                        "NONE       : hidden (use commands to query)",
                        "The value is sent by the server to the clients, changes apply after a reload.")
                .defineInList("displayMode", "ACTION_BAR",
                        Arrays.asList("HUD", "ACTION_BAR", "BOTH", "NONE"));

        DATE_FORMAT = BUILDER
                .comment("Format of the displayed date.",
                        "Placeholders: {day} {dayName} {month} {monthName} {year} {hour} {minute}",
                        "Example: §7{dayName} {day} {monthName}, An {year} — {hour}h{minute}")
                .define("dateFormat", "§7{dayName} {day} {monthName}, An {year} — {hour}h{minute}");

        PAUSED_SUFFIX = BUILDER
                .comment("Suffix appended to the date display when the calendar is paused.")
                .define("pausedSuffix", " §8(paused)");

        HUD_POSITION = BUILDER
                .comment("Screen anchor of the HUD calendar.",
                        "TOP_LEFT, TOP_CENTER, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT")
                .defineInList("hudPosition", "TOP_LEFT",
                        Arrays.asList("TOP_LEFT", "TOP_CENTER", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"));

        HUD_SCALE = BUILDER
                .comment("Text scale of the HUD calendar.")
                .defineInRange("hudScale", 1.0, 0.5, 3.0);

        HUD_BACKGROUND = BUILDER
                .comment("Draw a semi-transparent background behind the HUD calendar.")
                .define("hudBackground", true);

        BUILDER.pop();

        // ── Events ────────────────────────────────────────────────────────────
        BUILDER.push("Events");

        EVENTS = BUILDER
                .comment("Messages broadcast to all players when the RP calendar reaches a given date.",
                        "Format : month;day;message",
                        "  month, day : 1-based",
                        "  message    : supports & and § color codes. Placeholders: {year} {monthName} {day}",
                        "Example: 12;25;&6The winter festival begins today!")
                .defineList("events", List.of(),
                        obj -> obj instanceof String s && s.split(";", 3).length == 3);

        EVENT_MESSAGE_MODE = BUILDER
                .comment("Display mode of the event messages: CHAT, ACTION_BAR, TITLE, IMMERSIVE or IMMERSIVE:presetName.")
                .define("eventMessageMode", "CHAT");

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
