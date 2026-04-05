package net.rp.rpimmersion.fog;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.border.WorldBorder;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rp.rpimmersion.PatternUtils;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.config.FogConfig;
import net.rp.rpimmersion.network.FogSyncPacket;

import java.util.*;

/**
 * Gestionnaire serveur du fog.
 *
 * Ordre de priorité (du plus fort au plus faible) :
 *   1. Override individuel (commande staff /rpimmersion fog set player)
 *   2. World border (si activé et joueur dehors)
 *   3. Zone RPImmersion (priority décroissante, avec variants jour/nuit)
 *   4. Override zone RpEssentials (priority décroissante)
 *   5. Biome (priority décroissante, avec variants jour/nuit)
 *   6. Fog global (avec variants jour/nuit)
 *   7. Inactive → fog vanilla
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

    // =========================================================================
    // RELOAD
    // =========================================================================

    public void reload() {
        presets.clear(); zones.clear(); biomes.clear(); rpeOverrides.clear();
        PatternUtils.clearCache();
        lastSentPacket.clear(); // Force resync à tous après reload

        try {
            for (String raw : FogConfig.FOG_PRESETS.get()) {
                try {
                    FogConfig.FogPreset p = FogConfig.FogPreset.parse(raw);
                    presets.put(p.name(), p);
                } catch (Exception e) {
                    RpImmersion.LOGGER.warn("[FogManager] Invalid preset: '{}' — {}", raw, e.getMessage());
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

            RpImmersion.LOGGER.info("[FogManager] {} presets, {} zones, {} biomes, {} RPE overrides",
                    presets.size(), zones.size(), biomes.size(), rpeOverrides.size());

        } catch (IllegalStateException e) {
            RpImmersion.LOGGER.warn("[FogManager] Config not loaded yet.");
        }
    }

    // =========================================================================
    // RÉSOLUTION
    // =========================================================================

    public FogSyncPacket resolve(ServerPlayer player) {
        // 1. Override individuel (priorité absolue)
        if (playerOverrides.containsKey(player.getUUID()))
            return buildPacket(playerOverrides.get(player.getUUID()));

        // 2. World border (priorité très haute, avant tout le reste)
        FogSyncPacket border = resolveWorldBorder(player);
        if (border != null) return border;

        boolean isDaytime = isDaytime(player);
        boolean zonePriority = getZonePriority();

        if (zonePriority) {
            FogSyncPacket zone = resolveZone(player, isDaytime);
            if (zone != null) return zone;
            FogSyncPacket rpe = resolveRpEssentials(player, isDaytime);
            if (rpe != null) return rpe;
            FogSyncPacket biome = resolveBiome(player, isDaytime);
            if (biome != null) return biome;
            FogSyncPacket global = resolveGlobal(isDaytime);
            if (global != null) return global;
        } else {
            FogSyncPacket global = resolveGlobal(isDaytime);
            if (global != null) return global;
            FogSyncPacket zone = resolveZone(player, isDaytime);
            if (zone != null) return zone;
            FogSyncPacket rpe = resolveRpEssentials(player, isDaytime);
            if (rpe != null) return rpe;
            FogSyncPacket biome = resolveBiome(player, isDaytime);
            if (biome != null) return biome;
            String weatherPreset = net.rp.rpimmersion.weather.WeatherManager.get().getCurrentWeatherFogPreset();
            if (weatherPreset != null && presets.containsKey(weatherPreset))
                return buildPacket(weatherPreset);
        }

        return FogSyncPacket.inactive();
    }

    // ── Resolvers ─────────────────────────────────────────────────────────────

    private FogSyncPacket resolveWorldBorder(ServerPlayer player) {
        try {
            if (!FogConfig.WORLD_BORDER_FOG_ENABLED.get()) return null;
            WorldBorder border = player.serverLevel().getWorldBorder();
            if (border.isWithinBounds(player.blockPosition())) return null;
            String preset = FogConfig.WORLD_BORDER_FOG_PRESET.get();
            return preset.isEmpty() ? null : buildPacket(preset);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private FogSyncPacket resolveZone(ServerPlayer player, boolean isDaytime) {
        double px = player.getX(), pz = player.getZ();
        for (FogConfig.FogZone zone : zones) {
            if (!zone.contains(px, pz)) continue;
            String preset = pickDayNight(zone.presetName(), zone.dayPreset(), zone.nightPreset(), isDaytime);
            return buildPacket(preset);
        }
        return null;
    }

    private FogSyncPacket resolveRpEssentials(ServerPlayer player, boolean isDaytime) {
        if (rpeOverrides.isEmpty()) return null;
        try {
            Class<?> cfgClass = Class.forName("net.rp.rpessentials.config.RpEssentialsConfig");
            @SuppressWarnings("unchecked")
            List<String> namedZones = (List<String>)
                    ((net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<?>) cfgClass
                            .getField("NAMED_ZONES").get(null)).get();

            double px = player.getX(), pz = player.getZ();
            for (String def : namedZones) {
                String[] p = def.split(";");
                if (p.length < 4) continue;
                double cx = Double.parseDouble(p[1].trim()), cz = Double.parseDouble(p[2].trim());
                double r  = Double.parseDouble(p[3].trim());
                double dx = px - cx, dz = pz - cz;
                if (dx * dx + dz * dz > r * r) continue;

                String zoneName = p[0].trim();
                for (FogConfig.RpEssentialsZoneOverride o : rpeOverrides) {
                    if (o.zoneName().equalsIgnoreCase(zoneName)) {
                        String preset = pickDayNight(o.presetName(), o.dayPreset(), o.nightPreset(), isDaytime);
                        return buildPacket(preset);
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            RpImmersion.LOGGER.debug("[FogManager] RPE zone check error: {}", e.getMessage());
        }
        return null;
    }

    private FogSyncPacket resolveBiome(ServerPlayer player, boolean isDaytime) {
        if (biomes.isEmpty()) return null;
        ResourceLocation biomeKey = player.level()
                .getBiome(player.blockPosition())
                .unwrapKey().map(k -> k.location()).orElse(null);
        if (biomeKey == null) return null;
        String biomeId = biomeKey.toString();
        for (FogConfig.FogBiome fb : biomes) {
            if (!PatternUtils.matches(biomeId, fb.biomePattern())) continue;
            String preset = pickDayNight(fb.presetName(), fb.dayPreset(), fb.nightPreset(), isDaytime);
            return buildPacket(preset);
        }
        return null;
    }

    private FogSyncPacket resolveGlobal(boolean isDaytime) {
        if (globalCommandOverride != null) return buildPacket(globalCommandOverride);
        try {
            if (!FogConfig.GLOBAL_FOG_ENABLED.get()) return null;
            String day   = FogConfig.GLOBAL_FOG_DAY_PRESET.get();
            String night = FogConfig.GLOBAL_FOG_NIGHT_PRESET.get();
            String base  = FogConfig.GLOBAL_FOG_PRESET.get();
            String preset = pickDayNight(base, day, night, isDaytime);
            return preset.isEmpty() ? null : buildPacket(preset);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String pickDayNight(String base, String dayPreset, String nightPreset, boolean isDaytime) {
        if (isDaytime && !dayPreset.isEmpty())   return dayPreset;
        if (!isDaytime && !nightPreset.isEmpty()) return nightPreset;
        return base;
    }

    private boolean isDaytime(ServerPlayer player) {
        long time = player.serverLevel().getDayTime() % 24000;
        try {
            int dayStart   = FogConfig.DAY_START_TICK.get();
            int nightStart = FogConfig.NIGHT_START_TICK.get();
            return time >= dayStart && time < nightStart;
        } catch (IllegalStateException e) {
            return time >= 1000 && time < 13000;
        }
    }

    private FogSyncPacket buildPacket(String presetName) {
        if (presetName == null || presetName.isEmpty()) return FogSyncPacket.inactive();
        FogConfig.FogPreset p = presets.get(presetName);
        return p != null ? FogSyncPacket.from(presetName, p) : FogSyncPacket.inactive();
    }

    private boolean getZonePriority() {
        try { return FogConfig.ZONE_PRIORITY_OVER_GLOBAL.get(); }
        catch (IllegalStateException e) { return true; }
    }

    // =========================================================================
    // SYNC — envoie seulement si l'état a changé
    // =========================================================================

    public void syncToPlayer(ServerPlayer player) {
        FogSyncPacket packet = resolve(player);
        FogSyncPacket last   = lastSentPacket.get(player.getUUID());
        if (!packet.equals(last)) {
            PacketDistributor.sendToPlayer(player, packet);
            lastSentPacket.put(player.getUUID(), packet);
            RpImmersion.LOGGER.debug("[Fog] → {} '{}'", player.getName().getString(), packet.presetName());
        }
    }

    public void syncToAll(MinecraftServer server) {
        lastSentPacket.clear(); // Force le renvoi à tous
        for (ServerPlayer p : server.getPlayerList().getPlayers()) syncToPlayer(p);
    }

    public void clearPlayerCache(UUID uuid) {
        lastSentPacket.remove(uuid);
        playerOverrides.remove(uuid);
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
