package net.rp.rpimmersion.compat;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rp.rpessentials.config.ConfigInspector;
import net.rp.rpimmersion.ImmersionReload;
import net.rp.rpimmersion.config.*;

public class ConfigGuiCompat {

    private static final int PURPLE = 0xA855F7;

    public static void register() {
        add("imm_fog", "Imm. Fog", FogConfig.class, FogConfig.SPEC);
        add("imm_weather", "Imm. Weather", WeatherConfig.class, WeatherConfig.SPEC);
        add("imm_calendar", "Imm. Calendar", CalendarConfig.class, CalendarConfig.SPEC);
        add("imm_hud", "Imm. HUD", HudConfig.class, HudConfig.SPEC);
        add("imm_effects", "Imm. Effects", EffectsConfig.class, EffectsConfig.SPEC);
        add("imm_events", "Imm. Events", EventsConfig.class, EventsConfig.SPEC);
    }

    private static void add(String id, String name, Class<?> clazz, ModConfigSpec spec) {
        ConfigInspector.register(id, name, PURPLE, clazz, spec, ImmersionReload::reloadAll);
    }
}