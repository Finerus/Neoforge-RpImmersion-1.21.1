package net.rp.rpimmersion;

import net.minecraft.server.MinecraftServer;
import net.rp.rpimmersion.calendar.CalendarManager;
import net.rp.rpimmersion.effects.EventManager;
import net.rp.rpimmersion.effects.PlayerStateManager;
import net.rp.rpimmersion.fog.FogManager;
import net.rp.rpimmersion.network.ClientSyncManager;
import net.rp.rpimmersion.weather.WeatherManager;

public final class ImmersionReload {

    private ImmersionReload() {}

    public static void reloadAll(MinecraftServer server) {
        if (server == null || server.getPlayerList() == null) return;
        FogManager.get().reload();
        WeatherManager.get().reload();
        EventManager.get().reload();
        ClientSyncManager.syncToAll(server);
        PlayerStateManager.get().syncSettingsToAll(server);
        FogManager.get().syncToAll(server);
        CalendarManager.get().syncToAll(server);
    }
}
