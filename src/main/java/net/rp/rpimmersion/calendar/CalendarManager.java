package net.rp.rpimmersion.calendar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.config.CalendarConfig;
import net.rp.rpimmersion.network.CalendarSyncPacket;

import java.io.*;
import java.nio.file.Path;
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
        File target = dataFile;
        CompletableFuture.runAsync(() -> {
            try (FileWriter w = new FileWriter(target)) { GSON.toJson(data, w); }
            catch (Exception e) { RpImmersion.LOGGER.error("[Calendar] Save failed: {}", e.getMessage()); }
        });
    }

    private record CalendarData(long totalRpMinutes, boolean paused) {}

    // =========================================================================
    // TICK
    // =========================================================================

    public void tick(MinecraftServer server) {
        if (isEffectivelyPaused()) return;
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
            // Sync à chaque minute RP — syncer uniquement par heure rendait le calendrier
            // invisible côté client pendant des dizaines de secondes
            syncToAll(server);
            int minsPerHour = CalendarConfig.RP_MINUTES_PER_HOUR.get();
            if (totalRpMinutes % minsPerHour == 0) save();
        } catch (IllegalStateException ignored) {}
    }

    private void tickMcDaySync(MinecraftServer server) {
        long dayTime = server.overworld().getDayTime();
        try {
            int minsPerHour = CalendarConfig.RP_MINUTES_PER_HOUR.get();
            int hoursPerDay = CalendarConfig.RP_HOURS_PER_DAY.get();
            long minsPerDay = (long) hoursPerDay * minsPerHour;

            long newTotal = dayTime * minsPerDay / 24000L;
            if (newTotal == totalRpMinutes) return;

            long prev      = totalRpMinutes;
            totalRpMinutes = newTotal;

            syncToAll(server);
            if (newTotal / minsPerDay != prev / minsPerDay) save();
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
            int  minute       = (int)(totalMinutes % minsPerHour);
            long totalHours   = totalMinutes / minsPerHour;
            int  hour         = (int)(totalHours % hoursPerDay);
            long totalDays    = totalHours / hoursPerDay;

            List<? extends String>  monthNames   = CalendarConfig.MONTH_NAMES.get();
            List<? extends Integer> daysPerMonth = CalendarConfig.DAYS_PER_MONTH.get();
            int numMonths = Math.max(1, monthNames.size());

            int startYear  = CalendarConfig.START_YEAR.get();
            int startMonth = CalendarConfig.START_MONTH.get() - 1;
            int startDay   = CalendarConfig.START_DAY.get() - 1;

            long dayIndex = totalDays + startDay;

            int month = startMonth;
            int year  = startYear;
            while (dayIndex >= getDaysInMonth(month, daysPerMonth, numMonths)) {
                dayIndex -= getDaysInMonth(month, daysPerMonth, numMonths);
                month++;
                if (month >= numMonths) { month = 0; year++; }
            }

            List<? extends String> dayNames = CalendarConfig.DAY_NAMES.get();
            int dayOfWeek = dayNames.isEmpty() ? -1 : (int)(totalDays % dayNames.size());

            String monthName = monthNames.get(Math.min(month, monthNames.size() - 1));
            String dayName   = (!dayNames.isEmpty() && dayOfWeek >= 0) ? dayNames.get(dayOfWeek) : "";

            hour = (hour + CalendarConfig.START_HOUR.get()) % Math.max(1, hoursPerDay);

            return new DateSnapshot(year, month + 1, (int) dayIndex + 1,
                    hour, minute, dayOfWeek, monthName, dayName);
        } catch (IllegalStateException e) {
            return new DateSnapshot(1, 1, 1, 0, 0, -1, "?", "?");
        }
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
        return new CalendarSyncPacket(
                s.year(), s.month(), s.day(), s.hour(), s.minute(), s.dayOfWeek(),
                isEffectivelyPaused(), formatDisplay(true)
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

    public void setDate(int year, int month, int day, int hour, int minute, MinecraftServer server) {
        try {
            int minsPerHour = Math.max(1, CalendarConfig.RP_MINUTES_PER_HOUR.get());
            int hoursPerDay = Math.max(1, CalendarConfig.RP_HOURS_PER_DAY.get());

            List<? extends String>  monthNames   = CalendarConfig.MONTH_NAMES.get();
            List<? extends Integer> daysPerMonth = CalendarConfig.DAYS_PER_MONTH.get();
            int numMonths = Math.max(1, monthNames.size());

            int startYear  = CalendarConfig.START_YEAR.get();
            int startMonth = CalendarConfig.START_MONTH.get() - 1;
            int startDay   = CalendarConfig.START_DAY.get() - 1;

            long days = 0;
            int y = startYear, m = startMonth;
            while (y < year || (y == year && m < month - 1)) {
                days += getDaysInMonth(m, daysPerMonth, numMonths);
                m++;
                if (m >= numMonths) { m = 0; y++; }
            }
            days += (day - 1) - startDay;
            days  = Math.max(0, days);

            int adjHour = Math.max(0, hour - CalendarConfig.START_HOUR.get());
            totalRpMinutes = days * hoursPerDay * minsPerHour + (long) adjHour * minsPerHour + minute;
            tickAccumulator = 0;

            // Reset de la baseline MC sinon tickMcDaySync écraserait la valeur qu'on vient de définir
            save();
            syncToAll(server);
        } catch (IllegalStateException e) {
            RpImmersion.LOGGER.warn("[Calendar] Config unavailable for setDate");
        }
    }

    public void advance(int minutes, MinecraftServer server) {
        totalRpMinutes = Math.max(0, totalRpMinutes + minutes);
        // Reset de la baseline MC sinon tickMcDaySync écraserait l'avance
        save();
        syncToAll(server);
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
            serverOpen = net.rp.rpessentials.RpEssentialsScheduleManager.getActiveSchedule() != null;
        } catch (Exception ignored) {}

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

    /**
     * Fixe la baseline MC au temps courant.
     * Appelé à l'init, après setDate et après advance, pour que tickMcDaySync
     * parte toujours du bon totalRpMinutes.
     */

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