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
    // STATIC INIT
    // =========================================================================
    static {
        BUILDER.push("Calendar System");

        ENABLE_CALENDAR = BUILDER
                .comment("Enable the RPImmersion RP calendar.")
                .define("enableCalendar", true);

        BUILDER.pop();

        // ── Time Ratio ────────────────────────────────────────────────────────
        BUILDER.push("Time Ratio");

        TICKS_PER_RP_MINUTE = BUILDER
                .comment("Server ticks per RP minute.",
                        "1200 = 1 real minute per RP minute (1:1).",
                        "600  = 2 RP minutes per real minute (2x speed).",
                        "100  = 12 RP minutes per real minute.")
                .defineInRange("ticksPerRpMinute", 1200, 1, 72000);

        RP_MINUTES_PER_HOUR = BUILDER
                .comment("RP minutes per RP hour. Default: 60.")
                .defineInRange("rpMinutesPerHour", 60, 1, 3600);

        RP_HOURS_PER_DAY = BUILDER
                .comment("RP hours per RP day. Default: 24.")
                .defineInRange("rpHoursPerDay", 24, 1, 100);

        BUILDER.pop();

        // ── Calendar Structure ─────────────────────────────────────────────────
        BUILDER.push("Calendar Structure");

        MONTH_NAMES = BUILDER
                .comment("Names of the RP months, in order. The number of entries defines the number of months.")
                .defineList("monthNames",
                        Arrays.asList("Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"),
                        obj -> obj instanceof String);

        DAYS_PER_MONTH = BUILDER
                .comment("Number of days per month. If one value: applied to all months.",
                        "If multiple values: one per month (must match monthNames count).")
                .defineList("daysPerMonth",
                        Arrays.asList(30),
                        obj -> obj instanceof Integer i && i > 0);

        DAY_NAMES = BUILDER
                .comment("Names of the days of the week (optional). Leave empty to disable week display.",
                        "Example: [\"Lundi\", \"Mardi\", \"Mercredi\", \"Jeudi\", \"Vendredi\", \"Samedi\", \"Dimanche\"]")
                .defineList("dayNames",
                        Arrays.asList("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"),
                        obj -> obj instanceof String);

        BUILDER.pop();

        // ── Start Date ────────────────────────────────────────────────────────
        BUILDER.push("Start Date");

        START_YEAR  = BUILDER.comment("Starting year of the RP calendar.").defineInRange("startYear",  1, 0, Integer.MAX_VALUE);
        START_MONTH = BUILDER.comment("Starting month (1-based).").defineInRange("startMonth", 1, 1, 100);
        START_DAY   = BUILDER.comment("Starting day (1-based).").defineInRange("startDay",   1, 1, 365);
        START_HOUR  = BUILDER.comment("Starting hour (0-23).").defineInRange("startHour",  8, 0, 99);

        BUILDER.pop();

        // ── Display ───────────────────────────────────────────────────────────
        BUILDER.push("Display");

        DISPLAY_MODE = BUILDER
                .comment("Where to show the calendar.",
                        "HUD        : overlay in corner (requires client-side rendering)",
                        "ACTION_BAR : Minecraft action bar (center bottom)",
                        "BOTH       : both",
                        "NONE       : hidden (use commands to query)")
                .define("displayMode", "ACTION_BAR");

        DATE_FORMAT = BUILDER
                .comment("Format of the displayed date.",
                        "Placeholders: {day} {dayName} {month} {monthName} {year} {hour} {minute}",
                        "Example: §7{dayName} {day} {monthName}, An {year} — {hour}h{minute}")
                .define("dateFormat", "§7{dayName} {day} {monthName}, An {year} — {hour}h{minute}");

        PAUSED_SUFFIX = BUILDER
                .comment("Suffix appended to the date display when the calendar is paused.")
                .define("pausedSuffix", " §8(en pause)");

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
