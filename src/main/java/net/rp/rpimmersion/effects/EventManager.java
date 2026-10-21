package net.rp.rpimmersion.effects;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.compat.ImmersiveMessagesCompat;
import net.rp.rpimmersion.config.EventsConfig;
import net.rp.rpimmersion.config.FogConfig;
import net.rp.rpimmersion.fog.FogManager;
import net.rp.rpimmersion.network.CameraShakePacket;
import net.rp.rpimmersion.network.EventSyncPacket;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class EventManager {

    private static EventManager instance;
    public static EventManager get() {
        if (instance == null) instance = new EventManager();
        return instance;
    }
    public static void reset() { instance = null; }

    private static final Random RNG = new Random();

    private final Map<String, EventsConfig.EventDef> defs = new LinkedHashMap<>();
    private EventsConfig.EventDef active = null;
    private long endMs = 0L;
    private long lastEndMs = 0L;
    private long lastRollMs = 0L;

    private static boolean enabled() {
        try { return EventsConfig.ENABLE_EVENTS.get(); }
        catch (IllegalStateException e) { return false; }
    }

    public void reload() {
        defs.clear();
        try {
            for (String raw : EventsConfig.EVENTS.get()) {
                try {
                    EventsConfig.EventDef d = EventsConfig.EventDef.parse(raw);
                    defs.put(d.id(), d);
                } catch (Exception e) {
                    RpImmersion.LOGGER.warn("[EventManager] Invalid event: '{}'", raw);
                }
            }
        } catch (IllegalStateException e) {
            RpImmersion.LOGGER.warn("[EventManager] Config not loaded yet.");
        }
        lastRollMs = System.currentTimeMillis();
        RpImmersion.LOGGER.info("[EventManager] {} events", defs.size());
    }

    // =========================================================================
    // DÉMARRAGE / FIN
    // =========================================================================

    public boolean start(String id, MinecraftServer server, int minutesOverride) {
        if (!enabled()) return false;
        EventsConfig.EventDef d = defs.get(id.toLowerCase());
        if (d == null) return false;
        if (active != null) end(server, false);

        int minutes = minutesOverride > 0 ? minutesOverride : d.minutes();
        active = d;
        endMs = System.currentTimeMillis() + minutes * 60_000L;

        if (!d.fogPreset().isEmpty() && !FogManager.get().hasPreset(d.fogPreset()))
            RpImmersion.LOGGER.warn("[EventManager] Event '{}' uses unknown fog preset '{}'", d.id(), d.fogPreset());
        FogManager.get().setEventFog(d.fogPreset().isEmpty() ? null : d.fogPreset());
        FogManager.get().syncToAll(server);

        forceWeather(server, d.weather(), minutes);

        EventSyncPacket packet = EventSyncPacket.from(d);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(p, packet);
            if (d.shake() > 0f) PacketDistributor.sendToPlayer(p, new CameraShakePacket(d.shake(), 3f));
        }
        announce(server, d.startMessage());
        RpImmersion.LOGGER.info("[EventManager] Event '{}' started for {} min", d.id(), minutes);
        return true;
    }

    public boolean stop(MinecraftServer server) {
        if (active == null) return false;
        end(server, true);
        return true;
    }

    private void end(MinecraftServer server, boolean announceEnd) {
        EventsConfig.EventDef d = active;
        if (d == null) return;
        active = null;
        lastEndMs = System.currentTimeMillis();

        FogManager.get().setEventFog(null);
        FogManager.get().syncToAll(server);

        EventSyncPacket off = EventSyncPacket.inactive(d.transitionSeconds());
        for (ServerPlayer p : server.getPlayerList().getPlayers()) PacketDistributor.sendToPlayer(p, off);
        if (announceEnd) announce(server, d.endMessage());
        RpImmersion.LOGGER.info("[EventManager] Event '{}' ended", d.id());
    }

    private static void forceWeather(MinecraftServer server, String weather, int minutes) {
        ServerLevel level = server.overworld();
        int ticks = Math.max(1200, minutes * 1200);
        switch (weather) {
            case "CLEAR" -> level.setWeatherParameters(ticks, 0, false, false);
            case "RAIN" -> level.setWeatherParameters(0, ticks, true, false);
            case "THUNDER" -> level.setWeatherParameters(0, ticks, true, true);
            default -> {}
        }
    }

    private static void announce(MinecraftServer server, String msg) {
        if (msg.isEmpty()) return;
        String mode = "ACTION_BAR";
        try { mode = EventsConfig.EVENT_MESSAGE_MODE.get(); } catch (IllegalStateException ignored) {}
        for (ServerPlayer p : server.getPlayerList().getPlayers()) ImmersiveMessagesCompat.sendMessage(p, msg, mode);
    }

    // =========================================================================
    // TICK (toutes les 20 ticks)
    // =========================================================================

    public void tick(MinecraftServer server) {
        if (!enabled()) {
            if (active != null) end(server, false);
            return;
        }
        long now = System.currentTimeMillis();
        if (active != null) {
            if (now >= endMs) end(server, true);
            return;
        }
        if (now - lastRollMs < 60_000L) return;
        lastRollMs = now;
        rollRandom(server, now);
    }

    private void rollRandom(MinecraftServer server, long now) {
        try {
            if (!EventsConfig.RANDOM_EVENTS_ENABLED.get()) return;
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            if (now - lastEndMs < EventsConfig.RANDOM_COOLDOWN_MINUTES.get() * 60_000L) return;
            boolean day = isDay(server);
            for (String raw : EventsConfig.RANDOM_EVENTS.get()) {
                try {
                    EventsConfig.RandomEntry r = EventsConfig.RandomEntry.parse(raw);
                    boolean timeOk = r.time().equals("ANY") || r.time().equals("DAY") == day;
                    if (timeOk && RNG.nextDouble() < r.chance() && start(r.eventId(), server, 0)) return;
                } catch (Exception e) {
                    RpImmersion.LOGGER.warn("[EventManager] Invalid random entry: '{}'", raw);
                }
            }
        } catch (IllegalStateException ignored) {}
    }

    private static boolean isDay(MinecraftServer server) {
        long time = server.overworld().getDayTime() % 24000;
        try { return time >= FogConfig.DAY_START_TICK.get() && time < FogConfig.NIGHT_START_TICK.get(); }
        catch (IllegalStateException e) { return time >= 1000 && time < 13000; }
    }

    // =========================================================================
    // SYNC / GETTERS
    // =========================================================================

    public void syncToPlayer(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, active == null ? EventSyncPacket.inactive(1f) : EventSyncPacket.from(active));
    }

    public String getActiveId() { return active != null ? active.id() : null; }

    public long getRemainingSeconds() {
        return active != null ? Math.max(0L, (endMs - System.currentTimeMillis()) / 1000L) : 0L;
    }

    public Set<String> getIds() { return Collections.unmodifiableSet(defs.keySet()); }
}