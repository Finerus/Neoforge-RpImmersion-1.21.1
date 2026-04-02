package net.rp.rpimmersion.fog;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
 *   1. Override individuel joueur (commandes staff)
 *   2. Zone RPImmersion (priority décroissante)
 *   3. Override zone RpEssentials (priority décroissante)
 *   4. Biome (priority décroissante)
 *   5. Fog global (config ou override commande)
 *   6. Inactive → fog vanilla
 *
 * Si zonePriorityOverGlobal = false, le global passe avant 2-4.
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

    private final Map<String, FogConfig.FogPreset>              presets           = new HashMap<>();
    private final List<FogConfig.FogZone>                        zones             = new ArrayList<>();
    private final List<FogConfig.FogBiome>                       biomes            = new ArrayList<>();
    private final List<FogConfig.RpEssentialsZoneOverride>       rpeOverrides      = new ArrayList<>();

    /** Override temporaire par joueur. null value = retirer l'override. */
    private final Map<UUID, String> playerOverrides = new HashMap<>();

    /** Override global via commande. null = suivre config. "" = désactiver. */
    private String globalCommandOverride = null;

    /** Cache du dernier packet envoyé par joueur pour éviter les doublons. */
    private final Map<UUID, FogSyncPacket> lastSentPacket = new HashMap<>();

    // =========================================================================
    // RELOAD
    // =========================================================================

    public void reload() {
        presets.clear();
        zones.clear();
        biomes.clear();
        rpeOverrides.clear();
        PatternUtils.clearCache();

        try {
            // Presets
            for (String raw : FogConfig.FOG_PRESETS.get()) {
                try {
                    FogConfig.FogPreset p = FogConfig.FogPreset.parse(raw);
                    presets.put(p.name(), p);
                } catch (Exception e) {
                    RpImmersion.LOGGER.warn("[FogManager] Invalid preset: '{}' — {}", raw, e.getMessage());
                }
            }

            // Zones (triées par priorité décroissante)
            for (String raw : FogConfig.FOG_ZONES.get()) {
                try { zones.add(FogConfig.FogZone.parse(raw)); }
                catch (Exception e) { RpImmersion.LOGGER.warn("[FogManager] Invalid zone: '{}' — {}", raw, e.getMessage()); }
            }
            zones.sort(Comparator.comparingInt(FogConfig.FogZone::priority).reversed());

            // Biomes (triés par priorité décroissante)
            for (String raw : FogConfig.FOG_BIOMES.get()) {
                try { biomes.add(FogConfig.FogBiome.parse(raw)); }
                catch (Exception e) { RpImmersion.LOGGER.warn("[FogManager] Invalid biome: '{}' — {}", raw, e.getMessage()); }
            }
            biomes.sort(Comparator.comparingInt(FogConfig.FogBiome::priority).reversed());

            // RpEssentials overrides
            for (String raw : FogConfig.RPESSENTIALS_ZONE_OVERRIDES.get()) {
                try { rpeOverrides.add(FogConfig.RpEssentialsZoneOverride.parse(raw)); }
                catch (Exception e) { RpImmersion.LOGGER.warn("[FogManager] Invalid RPE override: '{}' — {}", raw, e.getMessage()); }
            }
            rpeOverrides.sort(Comparator.comparingInt(FogConfig.RpEssentialsZoneOverride::priority).reversed());

            RpImmersion.LOGGER.info("[FogManager] Loaded {} presets, {} zones, {} biomes, {} RPE overrides",
                    presets.size(), zones.size(), biomes.size(), rpeOverrides.size());

        } catch (IllegalStateException e) {
            RpImmersion.LOGGER.warn("[FogManager] Config not loaded yet, skipping reload.");
        }
    }

    // =========================================================================
    // RÉSOLUTION
    // =========================================================================

    public FogSyncPacket resolve(ServerPlayer player) {
        // 1. Override individuel (priorité absolue)
        if (playerOverrides.containsKey(player.getUUID())) {
            String name = playerOverrides.get(player.getUUID());
            return buildPacket(name);
        }

        boolean zonePriority = getZonePriority();

        if (zonePriority) {
            // Zone → RpEssentials → Biome → Global
            FogSyncPacket zone = resolveZone(player);
            if (zone != null) return zone;

            FogSyncPacket rpe = resolveRpEssentials(player);
            if (rpe != null) return rpe;

            FogSyncPacket biome = resolveBiome(player);
            if (biome != null) return biome;

            FogSyncPacket global = resolveGlobal();
            if (global != null) return global;
        } else {
            // Global → Zone → RpEssentials → Biome
            FogSyncPacket global = resolveGlobal();
            if (global != null) return global;

            FogSyncPacket zone = resolveZone(player);
            if (zone != null) return zone;

            FogSyncPacket rpe = resolveRpEssentials(player);
            if (rpe != null) return rpe;

            FogSyncPacket biome = resolveBiome(player);
            if (biome != null) return biome;
        }

        return FogSyncPacket.inactive();
    }

    // ── Resolvers ─────────────────────────────────────────────────────────────

    private FogSyncPacket resolveZone(ServerPlayer player) {
        double px = player.getX(), pz = player.getZ();
        for (FogConfig.FogZone zone : zones) {
            if (zone.contains(px, pz)) return buildPacket(zone.presetName());
        }
        return null;
    }

    private FogSyncPacket resolveRpEssentials(ServerPlayer player) {
        if (rpeOverrides.isEmpty()) return null;
        // Lecture des zones RpEssentials via son propre système si présent
        try {
            Class<?> configClass = Class.forName("net.rp.rpessentials.config.RpEssentialsConfig");
            @SuppressWarnings("unchecked")
            List<String> namedZones = (List<String>)
                    ((net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<?>) configClass
                            .getField("NAMED_ZONES").get(null)).get();

            double px = player.getX(), pz = player.getZ();

            for (String zoneDef : namedZones) {
                String[] parts = zoneDef.split(";");
                if (parts.length < 4) continue;
                String zoneName = parts[0].trim();
                double cx = Double.parseDouble(parts[1].trim());
                double cz = Double.parseDouble(parts[2].trim());
                double r  = Double.parseDouble(parts[3].trim());
                double dx = px - cx, dz = pz - cz;
                if (dx * dx + dz * dz > r * r) continue;

                // Chercher un override pour cette zone
                for (FogConfig.RpEssentialsZoneOverride override : rpeOverrides) {
                    if (override.zoneName().equalsIgnoreCase(zoneName))
                        return buildPacket(override.presetName());
                }
            }
        } catch (ClassNotFoundException ignored) {
            // RpEssentials non installé — silencieux
        } catch (Exception e) {
            RpImmersion.LOGGER.debug("[FogManager] RpEssentials zone check failed: {}", e.getMessage());
        }
        return null;
    }

    private FogSyncPacket resolveBiome(ServerPlayer player) {
        if (biomes.isEmpty()) return null;
        ResourceLocation biomeKey = player.level()
                .getBiome(player.blockPosition())
                .unwrapKey()
                .map(k -> k.location())
                .orElse(null);
        if (biomeKey == null) return null;

        String biomeId = biomeKey.toString();
        for (FogConfig.FogBiome fogBiome : biomes) {
            if (PatternUtils.matches(biomeId, fogBiome.biomePattern()))
                return buildPacket(fogBiome.presetName());
        }
        return null;
    }

    private FogSyncPacket resolveGlobal() {
        if (globalCommandOverride != null) return buildPacket(globalCommandOverride);

        try {
            if (!FogConfig.GLOBAL_FOG_ENABLED.get()) return null;
            String name = FogConfig.GLOBAL_FOG_PRESET.get();
            return name.isEmpty() ? null : buildPacket(name);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private FogSyncPacket buildPacket(String presetName) {
        if (presetName == null || presetName.isEmpty()) return FogSyncPacket.inactive();
        FogConfig.FogPreset preset = presets.get(presetName);
        return preset != null ? FogSyncPacket.from(presetName, preset) : FogSyncPacket.inactive();
    }

    private boolean getZonePriority() {
        try { return FogConfig.ZONE_PRIORITY_OVER_GLOBAL.get(); }
        catch (IllegalStateException e) { return true; }
    }

    // =========================================================================
    // SYNC
    // =========================================================================

    public void syncToPlayer(ServerPlayer player) {
        FogSyncPacket packet = resolve(player);
        FogSyncPacket last   = lastSentPacket.get(player.getUUID());
        if (!packet.equals(last)) {
            PacketDistributor.sendToPlayer(player, packet);
            lastSentPacket.put(player.getUUID(), packet);
            RpImmersion.LOGGER.debug("[FogManager] → {} preset='{}'",
                    player.getName().getString(), packet.presetName());
        }
    }

    public void syncToAll(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            lastSentPacket.remove(p.getUUID());
            syncToPlayer(p);
        }
    }

    public void clearPlayerCache(UUID uuid) {
        lastSentPacket.remove(uuid);
        playerOverrides.remove(uuid);
    }

    // =========================================================================
    // COMMANDES STAFF
    // =========================================================================

    /** null = désactiver l'override global. "" = forcer inactive. */
    public void setGlobalOverride(String presetName) { this.globalCommandOverride = presetName; }

    /** null = retirer l'override individuel. */
    public void setPlayerOverride(UUID uuid, String presetName) {
        if (presetName == null) playerOverrides.remove(uuid);
        else playerOverrides.put(uuid, presetName);
    }

    public String getGlobalOverride()                               { return globalCommandOverride; }
    public Map<String, FogConfig.FogPreset> getPresets()           { return Collections.unmodifiableMap(presets); }
    public List<FogConfig.FogZone> getZones()                      { return Collections.unmodifiableList(zones); }
    public List<FogConfig.FogBiome> getBiomes()                    { return Collections.unmodifiableList(biomes); }
    public boolean hasPreset(String name)                           { return presets.containsKey(name); }
}