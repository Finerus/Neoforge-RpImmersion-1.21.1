package net.rp.rpimmersion.fog;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.border.WorldBorder;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rp.rpimmersion.PatternUtils;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.config.FogConfig;
import net.rp.rpimmersion.network.FogSyncPacket;

import java.util.*;

/**
 * Gestionnaire serveur du fog.
 *
 1. Per-player override (staff command /rpimmersion fog set player)
 2. Global command override (staff command /rpimmersion fog set global)
 3. World border (unless a zone has a higher priority than worldBorderFogPriority)
 4. RPImmersion zone (priority descending, day/night variants)
 5. RpEssentials zone override (priority descending)
 6. Biome (priority descending, wildcards and tags)
 7. Dimension fog
 8. Global fog (day/night variants)
 9. Weather fog (CLEAR, RAIN, THUNDER)
 10. Inactive, vanilla fog
 If zonePriorityOverGlobal is false, global fog is evaluated before steps 4 to 7.
 *
 * Si zonePriorityOverGlobal = false : global s'insère avant 3-5.
 *
 * OPTIMISATION : les packets ne sont envoyés qu'à la connexion, au reload,
 * et sur changement détecté (toutes les ZONE_CHECK_INTERVAL_TICKS ticks).
 * Pas de packet si l'état n'a pas changé.
 */
public class FogManager {

    // =========================================================================
    // SINGLETON
    // =========================================================================

    private static FogManager instance;
    public static FogManager get() {
        if (instance == null) instance = new FogManager();
        return instance;
    }
    public static void reset() { instance = null; }

    // =========================================================================
    // STATE
    // =========================================================================

    private final Map<String, FogConfig.FogPreset>             presets      = new HashMap<>();
    private final List<FogConfig.FogZone>                       zones        = new ArrayList<>();
    private final List<FogConfig.FogBiome>                      biomes       = new ArrayList<>();
    private final List<FogConfig.RpEssentialsZoneOverride>      rpeOverrides = new ArrayList<>();

    private final Map<UUID, String> playerOverrides  = new HashMap<>();
    private String globalCommandOverride             = null;

    /** Dernier preset résolu envoyé par joueur — évite les doublons. */
    private final Map<UUID, FogSyncPacket> lastSentPacket = new HashMap<>();

    private final List<FogConfig.FogDimension> dimensions = new ArrayList<>();
    private final Map<UUID, Boolean> lastDaytime = new HashMap<>();
    private static java.lang.reflect.Field rpeZonesField;
    private static boolean rpeLookupDone = false;

    // =========================================================================
    // RELOAD
    // =========================================================================
    public void reload() {
        presets.clear(); zones.clear(); biomes.clear(); rpeOverrides.clear(); dimensions.clear();
        PatternUtils.clearCache();
        lastSentPacket.clear();

        try {
            for (String raw : FogConfig.FOG_PRESETS.get()) {
                try {
                    FogConfig.FogPreset p = FogConfig.FogPreset.parse(raw);
                    presets.put(p.name(), p);
                } catch (Exception e) {
                    RpImmersion.LOGGER.warn("[FogManager] Invalid preset: '{}' ({})", raw, e.getMessage());
                }
            }
            for (String raw : FogConfig.FOG_ZONES.get()) {
                try { zones.add(FogConfig.FogZone.parse(raw)); }
                catch (Exception e) { RpImmersion.LOGGER.warn("[FogManager] Invalid zone: '{}'", raw); }
            }
            zones.sort(Comparator.comparingInt(FogConfig.FogZone::priority).reversed());

            for (String raw : FogConfig.FOG_BIOMES.get()) {
                try { biomes.add(FogConfig.FogBiome.parse(raw)); }
                catch (Exception e) { RpImmersion.LOGGER.warn("[FogManager] Invalid biome: '{}'", raw); }
            }
            biomes.sort(Comparator.comparingInt(FogConfig.FogBiome::priority).reversed());

            for (String raw : FogConfig.RPESSENTIALS_ZONE_OVERRIDES.get()) {
                try { rpeOverrides.add(FogConfig.RpEssentialsZoneOverride.parse(raw)); }
                catch (Exception e) { RpImmersion.LOGGER.warn("[FogManager] Invalid RPE override: '{}'", raw); }
            }
            rpeOverrides.sort(Comparator.comparingInt(FogConfig.RpEssentialsZoneOverride::priority).reversed());

            for (String raw : FogConfig.FOG_DIMENSIONS.get()) {
                try { dimensions.add(FogConfig.FogDimension.parse(raw)); }
                catch (Exception e) { RpImmersion.LOGGER.warn("[FogManager] Invalid dimension fog: '{}'", raw); }
            }

            validateReferences();

            RpImmersion.LOGGER.info("[FogManager] {} presets, {} zones, {} biomes, {} RPE overrides, {} dimensions",
                    presets.size(), zones.size(), biomes.size(), rpeOverrides.size(), dimensions.size());

        } catch (IllegalStateException e) {
            RpImmersion.LOGGER.warn("[FogManager] Config not loaded yet.");
        }
    }

    private void validateReferences() {
        for (FogConfig.FogZone z : zones)
            warnUnknown("zone " + z.name(), z.presetName(), z.dayPreset(), z.nightPreset());
        for (FogConfig.FogBiome b : biomes)
            warnUnknown("biome " + b.biomePattern(), b.presetName(), b.dayPreset(), b.nightPreset());
        for (FogConfig.RpEssentialsZoneOverride o : rpeOverrides)
            warnUnknown("RPE zone " + o.zoneName(), o.presetName(), o.dayPreset(), o.nightPreset());
        for (FogConfig.FogDimension d : dimensions)
            warnUnknown("dimension " + d.dimension(), d.presetName(), d.dayPreset(), d.nightPreset());
        warnUnknown("global fog", FogConfig.GLOBAL_FOG_PRESET.get(),
                FogConfig.GLOBAL_FOG_DAY_PRESET.get(), FogConfig.GLOBAL_FOG_NIGHT_PRESET.get());
        warnUnknown("world border fog", FogConfig.WORLD_BORDER_FOG_PRESET.get());
    }

    private void warnUnknown(String source, String... names) {
        for (String n : names) {
            if (n != null && !n.isEmpty() && !presets.containsKey(n)) {
                RpImmersion.LOGGER.warn("[FogManager] {} references unknown preset '{}'", source, n);
            }
        }
    }

    // =========================================================================
    // RÉSOLUTION
    // =========================================================================
    public FogSyncPacket resolve(ServerPlayer player) {
        if (!isSystemEnabled()) return FogSyncPacket.inactive();

        FogSyncPacket forced = buildPacket(playerOverrides.get(player.getUUID()));
        if (forced != null) return forced;
        forced = buildPacket(globalCommandOverride);
        if (forced != null) return forced;

        FogSyncPacket border = resolveWorldBorder(player);
        if (border != null) return border;

        boolean isDaytime = isDaytime(player);
        if (getZonePriority()) {
            FogSyncPacket local = resolveLocal(player, isDaytime);
            if (local != null) return local;
            FogSyncPacket global = resolveGlobal(isDaytime);
            if (global != null) return global;
        } else {
            FogSyncPacket global = resolveGlobal(isDaytime);
            if (global != null) return global;
            FogSyncPacket local = resolveLocal(player, isDaytime);
            if (local != null) return local;
        }

        FogSyncPacket weather = resolveWeather(player);
        return weather != null ? weather : FogSyncPacket.inactive();
    }

    private FogSyncPacket resolveLocal(ServerPlayer player, boolean isDaytime) {
        FogSyncPacket packet = resolveZone(player, isDaytime);
        if (packet == null) packet = resolveRpEssentials(player, isDaytime);
        if (packet == null) packet = resolveBiome(player, isDaytime);
        if (packet == null) packet = resolveDimension(player, isDaytime);
        return packet;
    }

    // ── Resolvers ─────────────────────────────────────────────────────────────

    private FogSyncPacket resolveWorldBorder(ServerPlayer player) {
        try {
            if (!FogConfig.WORLD_BORDER_FOG_ENABLED.get()) return null;
            WorldBorder border = player.serverLevel().getWorldBorder();
            if (border.isWithinBounds(player.blockPosition())) return null;
            int priority = FogConfig.WORLD_BORDER_FOG_PRIORITY.get();
            double px = player.getX(), pz = player.getZ();
            for (FogConfig.FogZone zone : zones) {
                if (zone.priority() > priority && zone.contains(px, pz)) return null;
            }
            return buildPacket(FogConfig.WORLD_BORDER_FOG_PRESET.get());
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private FogSyncPacket resolveZone(ServerPlayer player, boolean isDaytime) {
        String dimId = player.level().dimension().location().toString();
        double px = player.getX(), pz = player.getZ();
        for (FogConfig.FogZone zone : zones) {
            if (!zone.dimension().isEmpty() && !zone.dimension().equals(dimId)) continue;
            if (!zone.contains(px, pz)) continue;
            return buildPacket(pickDayNight(zone.presetName(), zone.dayPreset(), zone.nightPreset(), isDaytime));
        }
        return null;
    }

    private FogSyncPacket resolveRpEssentials(ServerPlayer player, boolean isDaytime) {
        if (rpeOverrides.isEmpty()) return null;
        List<? extends String> namedZones = rpeNamedZones();
        if (namedZones.isEmpty()) return null;

        String dimId = player.level().dimension().location().toString();
        double px = player.getX(), pz = player.getZ();
        Set<String> inside = new HashSet<>();

        for (String def : namedZones) {
            String[] p = def.split(";");
            if (p.length < 4) continue;
            try {
                if (p.length >= 6 && p[4].contains(":") && !p[4].trim().equals(dimId)) continue;
                double dx = px - Double.parseDouble(p[1].trim());
                double dz = pz - Double.parseDouble(p[2].trim());
                double r  = Double.parseDouble(p[3].trim());
                if (dx * dx + dz * dz <= r * r) inside.add(p[0].trim().toLowerCase());
            } catch (NumberFormatException ignored) {}
        }
        if (inside.isEmpty()) return null;

        for (FogConfig.RpEssentialsZoneOverride o : rpeOverrides) {
            if (!inside.contains(o.zoneName().toLowerCase())) continue;
            return buildPacket(pickDayNight(o.presetName(), o.dayPreset(), o.nightPreset(), isDaytime));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<? extends String> rpeNamedZones() {
        try {
            if (!rpeLookupDone) {
                rpeLookupDone = true;
                Class<?> cfgClass = Class.forName("net.rp.rpessentials.config.RpEssentialsConfig");
                rpeZonesField = cfgClass.getField("NAMED_ZONES");
            }
            if (rpeZonesField == null) return List.of();
            return ((ModConfigSpec.ConfigValue<List<? extends String>>) rpeZonesField.get(null)).get();
        } catch (ReflectiveOperationException | IllegalStateException e) {
            return List.of();
        }
    }

    private FogSyncPacket resolveBiome(ServerPlayer player, boolean isDaytime) {
        if (biomes.isEmpty()) return null;
        Holder<Biome> holder = player.level().getBiome(player.blockPosition());
        String biomeId = holder.unwrapKey().map(k -> k.location().toString()).orElse(null);
        if (biomeId == null) return null;
        for (FogConfig.FogBiome fb : biomes) {
            if (!matchesBiome(holder, biomeId, fb.biomePattern())) continue;
            return buildPacket(pickDayNight(fb.presetName(), fb.dayPreset(), fb.nightPreset(), isDaytime));
        }
        return null;
    }

    private boolean matchesBiome(Holder<Biome> holder, String biomeId, String pattern) {
        if (pattern.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(pattern.substring(1));
            return tag != null && holder.is(TagKey.create(Registries.BIOME, tag));
        }
        return PatternUtils.matches(biomeId, pattern);
    }

    private FogSyncPacket resolveDimension(ServerPlayer player, boolean isDaytime) {
        if (dimensions.isEmpty()) return null;
        String dimId = player.level().dimension().location().toString();
        for (FogConfig.FogDimension d : dimensions) {
            if (!d.dimension().equals(dimId)) continue;
            return buildPacket(pickDayNight(d.presetName(), d.dayPreset(), d.nightPreset(), isDaytime));
        }
        return null;
    }

    private FogSyncPacket resolveGlobal(boolean isDaytime) {
        try {
            if (!FogConfig.GLOBAL_FOG_ENABLED.get()) return null;
            String preset = pickDayNight(FogConfig.GLOBAL_FOG_PRESET.get(),
                    FogConfig.GLOBAL_FOG_DAY_PRESET.get(),
                    FogConfig.GLOBAL_FOG_NIGHT_PRESET.get(), isDaytime);
            return buildPacket(preset);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private FogSyncPacket resolveWeather(ServerPlayer player) {
        String preset = net.rp.rpimmersion.weather.WeatherManager.get().getWeatherFogPreset(player.serverLevel());
        return preset == null ? null : buildPacket(preset);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String pickDayNight(String base, String dayPreset, String nightPreset, boolean isDaytime) {
        if (isDaytime && !dayPreset.isEmpty())   return dayPreset;
        if (!isDaytime && !nightPreset.isEmpty()) return nightPreset;
        return base;
    }

    private boolean isDaytime(ServerPlayer player) {
        if (!player.serverLevel().dimensionType().hasSkyLight()) return true;
        long time = player.serverLevel().getDayTime() % 24000;
        try {
            int dayStart   = FogConfig.DAY_START_TICK.get();
            int nightStart = FogConfig.NIGHT_START_TICK.get();
            if (dayStart <= nightStart) return time >= dayStart && time < nightStart;
            return time >= dayStart || time < nightStart;
        } catch (IllegalStateException e) {
            return time >= 1000 && time < 13000;
        }
    }

    private FogSyncPacket buildPacket(String presetName) {
        if (presetName == null || presetName.isEmpty()) return null;
        FogConfig.FogPreset p = presets.get(presetName);
        return p != null ? FogSyncPacket.from(presetName, p) : null;
    }

    private boolean isSystemEnabled() {
        try { return FogConfig.ENABLE_FOG_SYSTEM.get(); }
        catch (IllegalStateException e) { return true; }
    }

    private float dayNightTransition() {
        try { return FogConfig.DAY_NIGHT_TRANSITION_SECONDS.get().floatValue(); }
        catch (IllegalStateException e) { return 30f; }
    }

    private boolean getZonePriority() {
        try { return FogConfig.ZONE_PRIORITY_OVER_GLOBAL.get(); }
        catch (IllegalStateException e) { return true; }
    }

    // =========================================================================
    // SYNC — envoie seulement si l'état a changé
    // =========================================================================
    public void syncToPlayer(ServerPlayer player) {
        UUID id = player.getUUID();
        FogSyncPacket packet = resolve(player);
        FogSyncPacket last = lastSentPacket.get(id);

        boolean day = isDaytime(player);
        Boolean wasDay = lastDaytime.put(id, day);

        if (packet.equals(last)) return;

        FogSyncPacket toSend = packet;
        if (packet.active() && last != null && last.active() && wasDay != null && wasDay != day) {
            toSend = packet.withTransition(dayNightTransition());
        }
        PacketDistributor.sendToPlayer(player, toSend);
        lastSentPacket.put(id, packet);
        RpImmersion.LOGGER.debug("[Fog] -> {} '{}'", player.getName().getString(), packet.presetName());
    }

    public void refreshAll(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) syncToPlayer(p);
    }

    public void clearPlayerCache(UUID uuid) {
        lastSentPacket.remove(uuid);
        lastDaytime.remove(uuid);
        playerOverrides.remove(uuid);
    }

    public void syncToAll(MinecraftServer server) {
        lastSentPacket.clear(); // Force le renvoi à tous
        for (ServerPlayer p : server.getPlayerList().getPlayers()) syncToPlayer(p);
    }

    // =========================================================================
    // COMMANDES STAFF
    // =========================================================================

    public void setGlobalOverride(String presetName) { this.globalCommandOverride = presetName; }
    public void setPlayerOverride(UUID uuid, String presetName) {
        if (presetName == null) playerOverrides.remove(uuid);
        else playerOverrides.put(uuid, presetName);
    }

    public String                            getGlobalOverride() { return globalCommandOverride; }
    public Map<String, FogConfig.FogPreset>  getPresets()        { return Collections.unmodifiableMap(presets); }
    public List<FogConfig.FogZone>           getZones()          { return Collections.unmodifiableList(zones); }
    public List<FogConfig.FogBiome>          getBiomes()         { return Collections.unmodifiableList(biomes); }
    public boolean                           hasPreset(String n) { return presets.containsKey(n); }
}
