package net.rp.rpimmersion.calendar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.compat.ImmersiveMessagesCompat;
import net.rp.rpimmersion.config.CalendarConfig;
import net.rp.rpimmersion.network.CalendarSyncPacket;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Gestionnaire du calendrier RP.
 * - État persisté dans world/data/rpimmersion/calendar.json
 * - Avance en fonction des ticks serveur ou du temps MC (minecraftDaySync)
 * - Peut être mis en pause via commande ou via le planning RpEssentials
 * - Sync via CalendarSyncPacket à la connexion et à chaque minute RP
 */
public class CalendarManager {

    // =========================================================================
    // SINGLETON
    // =========================================================================

    private static CalendarManager instance;
    public static CalendarManager get() {
        if (instance == null) instance = new CalendarManager();
        return instance;
    }
    public static void reset() { instance = null; }

    // =========================================================================
    // STATE
    // =========================================================================

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private long    totalRpMinutes       = 0;
    private boolean paused               = false;
    private boolean schedulePaused       = false;

    // Compteur tick pour le mode normal
    private int  tickAccumulator    = 0;

    private File dataFile = null;

    private long   lastDayIndex  = -1;
    private long   lastTimeOfDay = 0;
    private String lastEventKey  = "";

    // =========================================================================
    // INIT / PERSIST
    // =========================================================================

    public void initForServer(MinecraftServer server) {
        Path worldData = server.getWorldPath(LevelResource.ROOT)
                .resolve("data/rpimmersion");
        worldData.toFile().mkdirs();
        dataFile = worldData.resolve("calendar.json").toFile();

        if (dataFile.exists()) {
            load();
        } else {
            totalRpMinutes = 0;
            paused         = false;
            save();
        }

        // Applique l'état du gamerule selon la pause actuelle dès le démarrage
        updateDaylightCycle(server);

        RpImmersion.LOGGER.info("[Calendar] Initialized — {}", formatDisplay(false));
    }

    private void load() {
        try (FileReader r = new FileReader(dataFile)) {
            CalendarData data = GSON.fromJson(r, CalendarData.class);
            if (data != null) {
                totalRpMinutes = data.totalRpMinutes;
                paused         = data.paused;
            }
        } catch (Exception e) {
            RpImmersion.LOGGER.error("[Calendar] Failed to load: {}", e.getMessage());
        }
    }

    public void save() {
        if (dataFile == null) return;
        CalendarData data = new CalendarData(totalRpMinutes, paused);
        Path target = dataFile.toPath();
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.writeString(tmp, GSON.toJson(data), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            RpImmersion.LOGGER.error("[Calendar] Save failed: {}", e.getMessage());
        }
    }

    private record CalendarData(long totalRpMinutes, boolean paused) {}

    // =========================================================================
    // TICK
    // =========================================================================
    public void tick(MinecraftServer server) {
        if (isEffectivelyPaused()) { lastDayIndex = -1; return; }
        try {
            if (CalendarConfig.MINECRAFT_DAY_SYNC.get()) {
                tickMcDaySync(server);
                return;
            }
            int ticksPerMin = CalendarConfig.TICKS_PER_RP_MINUTE.get();
            tickAccumulator++;
            if (tickAccumulator < ticksPerMin) return;
            tickAccumulator = 0;
            totalRpMinutes++;
            onTimeChanged(server);
            int minsPerHour = CalendarConfig.RP_MINUTES_PER_HOUR.get();
            if (totalRpMinutes % minsPerHour == 0) save();
        } catch (IllegalStateException ignored) {}
    }

    private void tickMcDaySync(MinecraftServer server) {
        long dayTime   = server.overworld().getDayTime();
        long dayIndex  = Math.floorDiv(dayTime, 24000L);
        long timeOfDay = Math.floorMod(dayTime, 24000L);
        try {
            int minsPerDay = Math.max(1,
                    CalendarConfig.RP_HOURS_PER_DAY.get() * CalendarConfig.RP_MINUTES_PER_HOUR.get());

            long dayCount = totalRpMinutes / minsPerDay;
            if (lastDayIndex >= 0) {
                long days = dayIndex - lastDayIndex;
                if (days > 0) dayCount += days;
                else if (lastTimeOfDay - timeOfDay >= 12000L) dayCount++;
            }
            lastDayIndex  = dayIndex;
            lastTimeOfDay = timeOfDay;

            long newTotal = dayCount * minsPerDay + timeOfDay * minsPerDay / 24000L;
            if (newTotal == totalRpMinutes) return;

            boolean newDay = newTotal / minsPerDay != totalRpMinutes / minsPerDay;
            totalRpMinutes = newTotal;
            onTimeChanged(server);
            if (newDay) save();
        } catch (IllegalStateException ignored) {}
    }

    private void onTimeChanged(MinecraftServer server) {
        syncToAll(server);
        checkEvents(server);
    }

    private void checkEvents(MinecraftServer server) {
        try {
            List<? extends String> events = CalendarConfig.EVENTS.get();
            DateSnapshot s = getSnapshot();
            String key = s.year() + "-" + s.month() + "-" + s.day();
            if (key.equals(lastEventKey)) return;
            boolean firstCheck = lastEventKey.isEmpty();
            lastEventKey = key;
            if (firstCheck || events.isEmpty()) return;

            String mode = CalendarConfig.EVENT_MESSAGE_MODE.get();
            for (String raw : events) {
                String[] p = raw.split(";", 3);
                if (p.length < 3) continue;
                try {
                    if (Integer.parseInt(p[0].trim()) != s.month()
                            || Integer.parseInt(p[1].trim()) != s.day()) continue;
                } catch (NumberFormatException e) { continue; }

                String msg = p[2]
                        .replace("{year}", String.valueOf(s.year()))
                        .replace("{monthName}", s.monthName())
                        .replace("{day}", String.valueOf(s.day()));
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    ImmersiveMessagesCompat.sendMessage(player, msg, mode);
                }
            }
        } catch (IllegalStateException ignored) {}
    }

    private void alignWorldTime(MinecraftServer server) {
        try {
            if (!CalendarConfig.MINECRAFT_DAY_SYNC.get()) return;
            int minsPerDay = Math.max(1,
                    CalendarConfig.RP_HOURS_PER_DAY.get() * CalendarConfig.RP_MINUTES_PER_HOUR.get());
            long minuteOfDay = totalRpMinutes % minsPerDay;
            ServerLevel overworld = server.overworld();
            long base = Math.floorDiv(overworld.getDayTime(), 24000L) * 24000L;
            overworld.setDayTime(base + minuteOfDay * 24000L / minsPerDay);
            lastDayIndex = -1;
        } catch (IllegalStateException ignored) {}
    }

    // =========================================================================
    // DATE COMPUTATION
    // =========================================================================
    public DateSnapshot getSnapshot() {
        try {
            int minsPerHour = Math.max(1, CalendarConfig.RP_MINUTES_PER_HOUR.get());
            int hoursPerDay = Math.max(1, CalendarConfig.RP_HOURS_PER_DAY.get());

            long totalMinutes = totalRpMinutes;
            int  minute       = (int) (totalMinutes % minsPerHour);
            long totalHours   = totalMinutes / minsPerHour;
            int  hour         = (int) (totalHours % hoursPerDay);
            long totalDays    = totalHours / hoursPerDay;

            List<? extends String>  monthNames   = CalendarConfig.MONTH_NAMES.get();
            List<? extends Integer> daysPerMonth = CalendarConfig.DAYS_PER_MONTH.get();
            int numMonths = Math.max(1, monthNames.size());

            int startYear  = CalendarConfig.START_YEAR.get();
            int startMonth = Math.min(CalendarConfig.START_MONTH.get() - 1, numMonths - 1);
            int startDay   = CalendarConfig.START_DAY.get() - 1;

            long yearLength = yearLength(daysPerMonth, numMonths);
            long dayIndex   = totalDays + monthOffset(startMonth, daysPerMonth, numMonths) + startDay;
            long year       = startYear + dayIndex / yearLength;
            dayIndex %= yearLength;

            int month = 0;
            while (dayIndex >= getDaysInMonth(month, daysPerMonth, numMonths)) {
                dayIndex -= getDaysInMonth(month, daysPerMonth, numMonths);
                month++;
            }

            List<? extends String> dayNames = CalendarConfig.DAY_NAMES.get();
            int dayOfWeek = dayNames.isEmpty() ? -1 : (int) (totalDays % dayNames.size());

            String monthName = monthNames.isEmpty() ? "?" : monthNames.get(Math.min(month, monthNames.size() - 1));
            String dayName   = (!dayNames.isEmpty() && dayOfWeek >= 0) ? dayNames.get(dayOfWeek) : "";

            hour = (hour + CalendarConfig.START_HOUR.get()) % hoursPerDay;

            return new DateSnapshot((int) Math.min(year, Integer.MAX_VALUE), month + 1, (int) dayIndex + 1,
                    hour, minute, dayOfWeek, monthName, dayName);
        } catch (IllegalStateException e) {
            return new DateSnapshot(1, 1, 1, 0, 0, -1, "?", "?");
        }
    }

    private long monthOffset(int monthIndex, List<? extends Integer> daysPerMonth, int numMonths) {
        long sum = 0;
        for (int i = 0; i < monthIndex; i++) sum += getDaysInMonth(i, daysPerMonth, numMonths);
        return sum;
    }

    private long yearLength(List<? extends Integer> daysPerMonth, int numMonths) {
        return Math.max(1, monthOffset(numMonths, daysPerMonth, numMonths));
    }

    private int getDaysInMonth(int month, List<? extends Integer> daysPerMonth, int numMonths) {
        if (daysPerMonth.isEmpty()) return 30;
        if (daysPerMonth.size() == 1) return daysPerMonth.get(0);
        return daysPerMonth.get(month % Math.min(daysPerMonth.size(), numMonths));
    }

    public record DateSnapshot(int year, int month, int day,
                               int hour, int minute, int dayOfWeek,
                               String monthName, String dayName) {}

    // =========================================================================
    // DISPLAY
    // =========================================================================

    public String formatDisplay(boolean withPause) {
        DateSnapshot s = getSnapshot();
        String fmt;
        try { fmt = CalendarConfig.DATE_FORMAT.get(); }
        catch (IllegalStateException e) { fmt = "{dayName} {day} {monthName}, An {year}"; }

        String result = fmt
                .replace("{year}",      String.valueOf(s.year()))
                .replace("{month}",     String.format("%02d", s.month()))
                .replace("{monthName}", s.monthName())
                .replace("{day}",       String.format("%02d", s.day()))
                .replace("{dayName}",   s.dayName())
                .replace("{hour}",      String.format("%02d", s.hour()))
                .replace("{minute}",    String.format("%02d", s.minute()));

        // Utilise isEffectivelyPaused pour afficher le suffixe même en pause automatique
        if (withPause && isEffectivelyPaused()) {
            try { result += CalendarConfig.PAUSED_SUFFIX.get(); }
            catch (IllegalStateException e) { result += " (paused)"; }
        }
        return result;
    }

    private CalendarSyncPacket buildPacket() {
        DateSnapshot s = getSnapshot();
        boolean enabled = true;
        String mode = "ACTION_BAR";
        String position = "TOP_LEFT";
        float scale = 1.0f;
        boolean background = true;
        try {
            enabled    = CalendarConfig.ENABLE_CALENDAR.get();
            mode       = CalendarConfig.DISPLAY_MODE.get().toUpperCase();
            position   = CalendarConfig.HUD_POSITION.get();
            scale      = CalendarConfig.HUD_SCALE.get().floatValue();
            background = CalendarConfig.HUD_BACKGROUND.get();
        } catch (IllegalStateException ignored) {}

        if (!enabled) mode = "NONE";
        return new CalendarSyncPacket(
                s.year(), s.month(), s.day(), s.hour(), s.minute(), s.dayOfWeek(),
                isEffectivelyPaused(), enabled ? formatDisplay(true) : "",
                mode, position, scale, background
        );
    }

    // =========================================================================
    // SYNC
    // =========================================================================

    public void syncToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, buildPacket());
    }

    public void syncToAll(MinecraftServer server) {
        CalendarSyncPacket packet = buildPacket();
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            PacketDistributor.sendToPlayer(p, packet);
    }

    // =========================================================================
    // COMMANDES
    // =========================================================================

    public void setPaused(boolean pause, MinecraftServer server) {
        this.paused = pause;
        save();
        updateDaylightCycle(server);
        syncToAll(server);
    }

    public boolean setDate(int year, int month, int day, int hour, int minute, MinecraftServer server) {
        try {
            int minsPerHour = Math.max(1, CalendarConfig.RP_MINUTES_PER_HOUR.get());
            int hoursPerDay = Math.max(1, CalendarConfig.RP_HOURS_PER_DAY.get());

            List<? extends String>  monthNames   = CalendarConfig.MONTH_NAMES.get();
            List<? extends Integer> daysPerMonth = CalendarConfig.DAYS_PER_MONTH.get();
            int numMonths = Math.max(1, monthNames.size());

            if (month < 1 || month > numMonths) return false;
            if (day < 1 || day > getDaysInMonth(month - 1, daysPerMonth, numMonths)) return false;

            int startYear  = CalendarConfig.START_YEAR.get();
            int startMonth = Math.min(CalendarConfig.START_MONTH.get() - 1, numMonths - 1);
            int startDay   = CalendarConfig.START_DAY.get() - 1;

            long yearLength = yearLength(daysPerMonth, numMonths);
            long days = (long) (year - startYear) * yearLength
                    + monthOffset(month - 1, daysPerMonth, numMonths) + (day - 1)
                    - monthOffset(startMonth, daysPerMonth, numMonths) - startDay;
            days = Math.max(0, days);

            int adjHour    = Math.floorMod(hour - CalendarConfig.START_HOUR.get(), hoursPerDay);
            int safeMinute = Math.max(0, Math.min(minute, minsPerHour - 1));
            totalRpMinutes = days * hoursPerDay * minsPerHour + (long) adjHour * minsPerHour + safeMinute;
            tickAccumulator = 0;

            alignWorldTime(server);
            save();
            onTimeChanged(server);
            return true;
        } catch (IllegalStateException e) {
            RpImmersion.LOGGER.warn("[Calendar] Config unavailable for setDate");
            return false;
        }
    }

    public void advance(int minutes, MinecraftServer server) {
        totalRpMinutes = Math.max(0, totalRpMinutes + minutes);
        alignWorldTime(server);
        save();
        onTimeChanged(server);
    }

    // =========================================================================
    // SCHEDULE PAUSE (appelé depuis CalendarServerEventHandler toutes les ~20 ticks)
    // =========================================================================
    public void updateSchedulePause(MinecraftServer server) {
        try {
            if (!CalendarConfig.PAUSE_WHEN_SERVER_CLOSED.get()) {
                if (schedulePaused) {
                    schedulePaused = false;
                    updateDaylightCycle(server);
                }
                return;
            }
        } catch (IllegalStateException e) { return; }

        boolean serverOpen = true;
        try {
            serverOpen = net.rp.rpessentials.RpEssentialsScheduleManager.isServerOpen();
        } catch (Exception | LinkageError ignored) {}

        boolean shouldPause = !serverOpen;
        if (shouldPause != schedulePaused) {
            schedulePaused = shouldPause;
            RpImmersion.LOGGER.info("[Calendar] Schedule-based pause: {}", schedulePaused);
            updateDaylightCycle(server);
            syncToAll(server);
        }
    }

    // =========================================================================
    // HELPERS PRIVÉS
    // =========================================================================
    private void updateDaylightCycle(MinecraftServer server) {
        try {
            boolean mcSync = CalendarConfig.MINECRAFT_DAY_SYNC.get();
            if (!mcSync && !CalendarConfig.SYNC_DAYLIGHT_CYCLE.get()) return;
        } catch (IllegalStateException e) { return; }
        boolean shouldRun = !isEffectivelyPaused();
        server.overworld().getGameRules()
                .getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT)
                .set(shouldRun, server);
        RpImmersion.LOGGER.info("[Calendar] doDaylightCycle → {}", shouldRun);
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public boolean isEffectivelyPaused() { return paused || schedulePaused; }
    public boolean isPaused()            { return paused; }
    public long    getTotalRpMinutes()   { return totalRpMinutes; }
}