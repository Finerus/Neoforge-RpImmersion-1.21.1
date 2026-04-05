package net.rp.rpimmersion.weather;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.compat.ImmersiveMessagesCompat;
import net.rp.rpimmersion.config.WeatherConfig;
import net.rp.rpimmersion.fog.FogManager;
import net.rp.rpimmersion.network.CameraShakePacket;

import java.util.*;

/**
 * Gestionnaire météo côté serveur.
 * - Intégration fog météo (CLEAR/RAIN/THUNDER → preset fog)
 * - Contrôle de fréquence des éclairs
 * - Cycle météo automatique
 * - Messages narratifs
 */
public class WeatherManager {

    // =========================================================================
    // SINGLETON
    // =========================================================================

    private static WeatherManager instance;
    public static WeatherManager get() {
        if (instance == null) instance = new WeatherManager();
        return instance;
    }
    public static void reset() { instance = null; }

    // =========================================================================
    // STATE
    // =========================================================================

    private final Map<WeatherConfig.WeatherType, String> weatherFogPresets = new HashMap<>();
    private final List<WeatherConfig.CycleStep>          cycleSteps        = new ArrayList<>();

    private int  cycleStepIndex         = 0;
    private long cycleStepStartMs       = System.currentTimeMillis();
    private WeatherConfig.WeatherType lastWeather = null;

    // Fog météo override — appliqué SEULEMENT si aucun autre fog n'est actif
    private String currentWeatherFogPreset = null;

    // =========================================================================
    // RELOAD
    // =========================================================================

    public void reload() {
        weatherFogPresets.clear();
        cycleSteps.clear();

        try {
            for (String raw : WeatherConfig.WEATHER_FOG_PRESETS.get()) {
                try {
                    WeatherConfig.WeatherFogPreset p = WeatherConfig.WeatherFogPreset.parse(raw);
                    weatherFogPresets.put(p.weather(), p.presetName());
                } catch (Exception e) {
                    RpImmersion.LOGGER.warn("[WeatherManager] Invalid fog preset: '{}'", raw);
                }
            }

            for (String raw : WeatherConfig.WEATHER_CYCLE_STEPS.get()) {
                try { cycleSteps.add(WeatherConfig.CycleStep.parse(raw)); }
                catch (Exception e) { RpImmersion.LOGGER.warn("[WeatherManager] Invalid cycle step: '{}'", raw); }
            }

            RpImmersion.LOGGER.info("[WeatherManager] {} weather fog presets, {} cycle steps",
                    weatherFogPresets.size(), cycleSteps.size());

        } catch (IllegalStateException e) {
            RpImmersion.LOGGER.warn("[WeatherManager] Config not loaded.");
        }
    }

    // =========================================================================
    // TICK
    // =========================================================================

    public void tick(MinecraftServer server) {
        try {
            if (!WeatherConfig.ENABLE_WEATHER_SYSTEM.get()) return;
        } catch (IllegalStateException e) { return; }

        ServerLevel overworld = server.overworld();
        WeatherConfig.WeatherType current = getCurrentWeather(overworld);

        // Changement de météo détecté
        if (current != lastWeather) {
            onWeatherChanged(current, lastWeather, server);
            lastWeather = current;
        }

        // Auto-cycle
        tickCycle(server, overworld);

        // Extra lightning si THUNDER et multiplier > 1.0
        if (current == WeatherConfig.WeatherType.THUNDER) {
            tickExtraLightning(server, overworld);
        }
    }

    private WeatherConfig.WeatherType getCurrentWeather(ServerLevel level) {
        if (level.isThundering()) return WeatherConfig.WeatherType.THUNDER;
        if (level.isRaining())    return WeatherConfig.WeatherType.RAIN;
        return WeatherConfig.WeatherType.CLEAR;
    }

    // =========================================================================
    // WEATHER CHANGE
    // =========================================================================

    private void onWeatherChanged(WeatherConfig.WeatherType newWeather,
                                  WeatherConfig.WeatherType oldWeather,
                                  MinecraftServer server) {
        // Fog météo
        String fogPreset = weatherFogPresets.get(newWeather);
        currentWeatherFogPreset = fogPreset; // null = pas de fog météo

        // Les FogManager.syncToPlayer re-resolve automatiquement avec le weather fog
        FogManager.get().syncToAll(server);

        // Messages narratifs
        try {
            if (!WeatherConfig.WEATHER_MESSAGES_ENABLED.get()) return;
            String msg = switch (newWeather) {
                case CLEAR   -> WeatherConfig.MSG_WEATHER_CLEAR.get();
                case RAIN    -> WeatherConfig.MSG_WEATHER_RAIN.get();
                case THUNDER -> WeatherConfig.MSG_WEATHER_THUNDER.get();
            };
            String mode = WeatherConfig.WEATHER_MESSAGE_MODE.get();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ImmersiveMessagesCompat.sendMessage(p, msg, mode);
            }
        } catch (IllegalStateException ignored) {}
    }

    // =========================================================================
    // AUTO CYCLE
    // =========================================================================

    private int lightningTickAcc = 0;

    private void tickCycle(MinecraftServer server, ServerLevel overworld) {
        try {
            if (!WeatherConfig.WEATHER_CYCLE_ENABLED.get() || cycleSteps.isEmpty()) return;
        } catch (IllegalStateException e) { return; }

        WeatherConfig.CycleStep step = cycleSteps.get(cycleStepIndex % cycleSteps.size());
        long elapsedMs = System.currentTimeMillis() - cycleStepStartMs;
        long durationMs = (long) step.durationMinutes() * 60_000L;

        if (elapsedMs < durationMs) return;

        // Passer à l'étape suivante
        cycleStepIndex = (cycleStepIndex + 1) % cycleSteps.size();
        cycleStepStartMs = System.currentTimeMillis();
        WeatherConfig.CycleStep next = cycleSteps.get(cycleStepIndex);

        applyWeather(overworld, next.weather());

        if (!next.message().isEmpty()) {
            try {
                String mode = WeatherConfig.WEATHER_MESSAGE_MODE.get();
                for (ServerPlayer p : server.getPlayerList().getPlayers())
                    ImmersiveMessagesCompat.sendMessage(p, next.message(), mode);
            } catch (IllegalStateException ignored) {}
        }
    }

    private void applyWeather(ServerLevel level, WeatherConfig.WeatherType type) {
        switch (type) {
            case CLEAR   -> { level.setRainLevel(0); level.setThunderLevel(0);
                level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
                level.setWeatherParameters(6000, 0, false, false); }
            case RAIN    -> level.setWeatherParameters(0, 6000, true, false);
            case THUNDER -> level.setWeatherParameters(0, 6000, true, true);
        }
    }

    // =========================================================================
    // EXTRA LIGHTNING
    // =========================================================================

    private static final Random RNG = new Random();

    private void tickExtraLightning(MinecraftServer server, ServerLevel overworld) {
        try {
            double mult = WeatherConfig.LIGHTNING_FREQUENCY_MULTIPLIER.get();
            if (mult <= 1.0) return; // Pas d'éclair supplémentaire
            if (!WeatherConfig.LIGHTNING_CONTROL_ENABLED.get()) return;

            // Probabilité d'un éclair supplémentaire ce tick
            // Vanilla spawn environ 1 éclair tous les ~200 ticks par chunk chargé.
            // On simplifie : chance = (mult - 1.0) / 200
            double chance = (mult - 1.0) / 200.0;
            if (RNG.nextDouble() > chance) return;

            // Choisir un joueur aléatoire
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (players.isEmpty()) return;
            ServerPlayer target = players.get(RNG.nextInt(players.size()));

            int radius;
            try { radius = WeatherConfig.LIGHTNING_SPAWN_RADIUS.get(); }
            catch (IllegalStateException e) { radius = 128; }

            double angle  = RNG.nextDouble() * Math.PI * 2;
            double dist   = RNG.nextDouble() * radius;
            BlockPos bolt = BlockPos.containing(
                    target.getX() + Math.cos(angle) * dist,
                    target.getY(),
                    target.getZ() + Math.sin(angle) * dist
            );

            net.minecraft.world.entity.LightningBolt entity =
                    net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(overworld);
            if (entity == null) return;
            entity.moveTo(bolt.getX(), bolt.getY(), bolt.getZ());
            overworld.addFreshEntity(entity);

        } catch (IllegalStateException ignored) {}
    }

    // =========================================================================
    // CAMERA SHAKE (appelé depuis WeatherServerEventHandler sur EntityStruckByLightningEvent)
    // =========================================================================

    public void onLightningStruck(BlockPos lightningPos, MinecraftServer server) {
        try {
            if (!WeatherConfig.LIGHTNING_SHAKE_ENABLED.get()) return;

            double maxDist      = WeatherConfig.LIGHTNING_SHAKE_MAX_DISTANCE.get();
            double maxIntensity = WeatherConfig.LIGHTNING_SHAKE_MAX_INTENSITY.get();
            float  duration     = WeatherConfig.LIGHTNING_SHAKE_DURATION_SECONDS.get().floatValue();
            boolean allowOverride = WeatherConfig.ALLOW_CLIENT_SHAKE_OVERRIDE.get();

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                double dist = player.blockPosition().distSqr(lightningPos);
                double maxDistSq = maxDist * maxDist;
                if (dist > maxDistSq) continue;

                // Intensité inversement proportionnelle à la distance
                double ratio     = 1.0 - (dist / maxDistSq);
                float  intensity = (float)(maxIntensity * ratio);

                PacketDistributor.sendToPlayer(player, new CameraShakePacket(intensity, duration));
            }
        } catch (IllegalStateException ignored) {}
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    /** Retourne le preset fog météo actif, null si aucun. */
    public String getCurrentWeatherFogPreset() { return currentWeatherFogPreset; }
}