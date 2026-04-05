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
 * - Avance en fonction des ticks serveur
 * - Peut être mis en pause via commande
 * - Sync via CalendarSyncPacket à la connexion et à chaque changement
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

    private long  totalRpMinutes = 0; // Minutes RP écoulées depuis la date de début
    private boolean paused       = false;
    private int   tickAccumulator = 0;
    private File  dataFile        = null;

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
            // Initialiser depuis la config
            try {
                totalRpMinutes = 0;
                paused         = false;
            } catch (IllegalStateException e) {
                totalRpMinutes = 0;
            }
            save();
        }
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
        if (paused) return;
        try {
            int ticksPerMin = CalendarConfig.TICKS_PER_RP_MINUTE.get();
            tickAccumulator++;
            if (tickAccumulator < ticksPerMin) return;
            tickAccumulator = 0;
            totalRpMinutes++;
            // Sync et sauvegarde à chaque heure RP pour limiter les I/O
            int minsPerHour = CalendarConfig.RP_MINUTES_PER_HOUR.get();
            if (totalRpMinutes % minsPerHour == 0) {
                syncToAll(server);
                save();
            }
        } catch (IllegalStateException ignored) {}
    }

    // =========================================================================
    // DATE COMPUTATION
    // =========================================================================

    public DateSnapshot getSnapshot() {
        try {
            int minsPerHour  = Math.max(1, CalendarConfig.RP_MINUTES_PER_HOUR.get());
            int hoursPerDay  = Math.max(1, CalendarConfig.RP_HOURS_PER_DAY.get());
            int minsPerDay   = minsPerHour * hoursPerDay;

            long totalMinutes = totalRpMinutes;
            int  minute       = (int)(totalMinutes % minsPerHour);
            long totalHours   = totalMinutes / minsPerHour;
            int  hour         = (int)(totalHours % hoursPerDay);
            long totalDays    = totalHours / hoursPerDay;

            List<? extends String>  monthNames   = CalendarConfig.MONTH_NAMES.get();
            List<? extends Integer> daysPerMonth = CalendarConfig.DAYS_PER_MONTH.get();
            int numMonths = Math.max(1, monthNames.size());

            // Calculer année, mois, jour
            int startYear  = CalendarConfig.START_YEAR.get();
            int startMonth = CalendarConfig.START_MONTH.get() - 1; // 0-based
            int startDay   = CalendarConfig.START_DAY.get() - 1;   // 0-based

            long dayIndex = totalDays + startDay;

            // Avancer les mois depuis startMonth
            int month = startMonth;
            int year  = startYear;
            while (dayIndex >= getDaysInMonth(month, daysPerMonth, numMonths)) {
                dayIndex -= getDaysInMonth(month, daysPerMonth, numMonths);
                month++;
                if (month >= numMonths) { month = 0; year++; }
            }

            // Jour de la semaine
            List<? extends String> dayNames = CalendarConfig.DAY_NAMES.get();
            int dayOfWeek = dayNames.isEmpty() ? -1 : (int)(totalDays % dayNames.size());

            String monthName  = monthNames.get(Math.min(month, monthNames.size() - 1));
            String dayName    = (!dayNames.isEmpty() && dayOfWeek >= 0)
                    ? dayNames.get(dayOfWeek) : "";

            // Heure de départ
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

        if (withPause && paused) {
            try { result += CalendarConfig.PAUSED_SUFFIX.get(); }
            catch (IllegalStateException e) { result += " (paused)"; }
        }
        return result;
    }

    private CalendarSyncPacket buildPacket() {
        DateSnapshot s = getSnapshot();
        return new CalendarSyncPacket(
                s.year(), s.month(), s.day(), s.hour(), s.minute(), s.dayOfWeek(),
                paused, formatDisplay(true)
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

            // Calculer totalDays depuis le début
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
            save();
            syncToAll(server);
        } catch (IllegalStateException e) {
            RpImmersion.LOGGER.warn("[Calendar] Config unavailable for setDate");
        }
    }

    public void advance(int minutes, MinecraftServer server) {
        totalRpMinutes = Math.max(0, totalRpMinutes + minutes);
        save();
        syncToAll(server);
    }

    public boolean isPaused()         { return paused; }
    public long    getTotalRpMinutes() { return totalRpMinutes; }
}
