package net.rp.rpimmersion.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rp.rpimmersion.compat.StaffChecker;
import net.rp.rpimmersion.config.FogConfig;
import net.rp.rpimmersion.config.HudConfig;
import net.rp.rpimmersion.config.WeatherConfig;

import java.util.List;
import java.util.Locale;

public final class ClientSyncManager {

    private ClientSyncManager() {}

    public static void syncToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, build(player));
    }

    public static void syncToAll(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) syncToPlayer(p);
    }

    private static ClientSettingsPacket build(ServerPlayer player) {
        boolean allowShake = true;
        boolean allowFog = false;
        boolean firstPerson = false;
        List<String> hidden = List.of();

        try { allowShake = WeatherConfig.ALLOW_CLIENT_SHAKE_OVERRIDE.get(); }
        catch (IllegalStateException ignored) {}

        try { allowFog = FogConfig.ALLOW_CLIENT_FOG_OVERRIDE.get(); }
        catch (IllegalStateException ignored) {}

        try {
            boolean exempt = HudConfig.EXEMPT_STAFF.get() && StaffChecker.isStaff(player);
            if (!exempt) {
                firstPerson = HudConfig.FORCE_FIRST_PERSON.get();
                if (HudConfig.ENABLE_HUD_CONTROL.get()) hidden = clean(HudConfig.HIDDEN_ELEMENTS.get());
            }
        } catch (IllegalStateException ignored) {}

        return new ClientSettingsPacket(allowShake, allowFog, firstPerson, hidden);
    }

    private static List<String> clean(List<? extends String> raw) {
        return raw.stream()
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(HudConfig.ELEMENTS::contains)
                .distinct()
                .toList();
    }
}
