package net.rp.rpimmersion.compat;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rp.rpessentials.api.RpEssentialsAPI;
import net.rp.rpessentials.config.ConfigInspector;
import net.rp.rpimmersion.ImmersionReload;
import net.rp.rpimmersion.config.*;

/**
 * Adds the RpImmersion config files to the RpEssentials Config Manager, grouped under an "RpImmersion"
 * title in purple (public API: {@link RpEssentialsAPI#registerConfig}). Changes made there are applied
 * live through {@link ImmersionReload#reloadAll}.
 */
public class ConfigGuiCompat {

    private static final String MOD = "RpImmersion";
    private static final int PURPLE = 0xA855F7;
    /** Accent bar, section titles and window title of these files in purple shades. */
    private static final ConfigInspector.Theme THEME = new ConfigInspector.Theme(PURPLE, 0xC084FC, 0xD8B4FE);

    public static void register() {
        add("imm_fog", "Fog", FogConfig.class, FogConfig.SPEC);
        add("imm_weather", "Weather", WeatherConfig.class, WeatherConfig.SPEC);
        add("imm_calendar", "Calendar", CalendarConfig.class, CalendarConfig.SPEC);
        add("imm_hud", "HUD", HudConfig.class, HudConfig.SPEC);
        add("imm_effects", "Effects", EffectsConfig.class, EffectsConfig.SPEC);
        add("imm_events", "Events", EventsConfig.class, EventsConfig.SPEC);
    }

    private static void add(String id, String name, Class<?> clazz, ModConfigSpec spec) {
        RpEssentialsAPI.registerConfig(MOD, PURPLE, id, name, PURPLE, THEME, clazz, spec, ImmersionReload::reloadAll);
    }
}
