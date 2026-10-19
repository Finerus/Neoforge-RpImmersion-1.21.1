package net.rp.rpimmersion;

import net.minecraft.server.MinecraftServer;
import net.rp.rpimmersion.calendar.CalendarManager;
import net.rp.rpimmersion.fog.FogManager;
import net.rp.rpimmersion.network.ClientSyncManager;
import net.rp.rpimmersion.weather.WeatherManager;

public final class ImmersionReload {

    private ImmersionReload() {}

    public static void reloadAll(MinecraftServer server) {
        if (server == null || server.getPlayerList() == null) return;
        FogManager.get().reload();
        WeatherManager.get().reload();
        ClientSyncManager.syncToAll(server);
        FogManager.get().syncToAll(server);
        CalendarManager.get().syncToAll(server);
    }
}
