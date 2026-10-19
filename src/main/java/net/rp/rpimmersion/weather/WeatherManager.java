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
    public static final String EXTRA_BOLT_TAG = "rpimmersion_extra_bolt";
    private boolean cycleStarted = false;

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
    private final List<WeatherConfig.RainEffect> rainEffects    = new ArrayList<>();
    private int rainEffectTick = 0;

    // =========================================================================
    // RELOAD
    // =========================================================================
    public void reload() {
        weatherFogPresets.clear();
        cycleSteps.clear();
        rainEffects.clear();
        cycleStarted = false;
        cycleStepIndex = 0;
        lastWeather = null;

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
            for (String raw : WeatherConfig.RAIN_EFFECTS.get()) {
                try { rainEffects.add(WeatherConfig.RainEffect.parse(raw)); }
                catch (Exception e) { RpImmersion.LOGGER.warn("[WeatherManager] Invalid rain effect: '{}'", raw); }
            }

            RpImmersion.LOGGER.info("[WeatherManager] {} weather fog presets, {} cycle steps, {} rain effects",
                    weatherFogPresets.size(), cycleSteps.size(), rainEffects.size());

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

        if (current != lastWeather) {
            onWeatherChanged(current, lastWeather, server);
            lastWeather = current;
        }

        tickCycle(server, overworld);

        if (++rainEffectTick >= 20) {
            rainEffectTick = 0;
            tickRainEffects(server);
        }

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
        currentWeatherFogPreset = weatherFogPresets.get(newWeather);
        FogManager.get().refreshAll(server);

        if (oldWeather == null) return;
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
            if (!WeatherConfig.WEATHER_CYCLE_ENABLED.get() || cycleSteps.isEmpty()) {
                cycleStarted = false;
                return;
            }
        } catch (IllegalStateException e) { return; }

        if (!cycleStarted) {
            cycleStarted = true;
            cycleStepStartMs = System.currentTimeMillis();
            applyStep(server, overworld, cycleSteps.get(cycleStepIndex % cycleSteps.size()), false);
            return;
        }

        WeatherConfig.CycleStep step = cycleSteps.get(cycleStepIndex % cycleSteps.size());
        long durationMs = (long) step.durationMinutes() * 60_000L;
        if (System.currentTimeMillis() - cycleStepStartMs < durationMs) return;

        cycleStepIndex = (cycleStepIndex + 1) % cycleSteps.size();
        cycleStepStartMs = System.currentTimeMillis();
        applyStep(server, overworld, cycleSteps.get(cycleStepIndex), true);
    }

    private void applyStep(MinecraftServer server, ServerLevel overworld,
                           WeatherConfig.CycleStep step, boolean announce) {
        applyWeather(overworld, step.weather(), step.durationMinutes() * 1200);
        if (!announce || step.message().isEmpty()) return;
        try {
            String mode = WeatherConfig.WEATHER_MESSAGE_MODE.get();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ImmersiveMessagesCompat.sendMessage(p, step.message(), mode);
            }
        } catch (IllegalStateException ignored) {}
    }

    private void applyWeather(ServerLevel level, WeatherConfig.WeatherType type, int durationTicks) {
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
        switch (type) {
            case CLEAR   -> level.setWeatherParameters(durationTicks, 0, false, false);
            case RAIN    -> level.setWeatherParameters(0, durationTicks, true, false);
            case THUNDER -> level.setWeatherParameters(0, durationTicks, true, true);
        }
    }

    // =========================================================================
    // EXTRA LIGHTNING
    // =========================================================================
    private static final Random RNG = new Random();

    private void tickExtraLightning(MinecraftServer server, ServerLevel overworld) {
        try {
            if (!WeatherConfig.LIGHTNING_CONTROL_ENABLED.get()) return;
            double mult = WeatherConfig.LIGHTNING_FREQUENCY_MULTIPLIER.get();
            if (mult <= 1.0) return;
            if (RNG.nextDouble() > (mult - 1.0) / 200.0) return;

            List<ServerPlayer> players = overworld.players();
            if (players.isEmpty()) return;
            ServerPlayer target = players.get(RNG.nextInt(players.size()));

            int radius;
            try { radius = WeatherConfig.LIGHTNING_SPAWN_RADIUS.get(); }
            catch (IllegalStateException e) { radius = 128; }

            double angle = RNG.nextDouble() * Math.PI * 2;
            double dist  = RNG.nextDouble() * radius;
            int targetX  = (int) (target.getX() + Math.cos(angle) * dist);
            int targetZ  = (int) (target.getZ() + Math.sin(angle) * dist);

            int surfaceY = overworld.getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    targetX, targetZ);

            net.minecraft.world.entity.LightningBolt entity =
                    net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(overworld);
            if (entity == null) return;
            entity.moveTo(targetX, surfaceY, targetZ);
            entity.addTag(EXTRA_BOLT_TAG);
            overworld.addFreshEntity(entity);

        } catch (IllegalStateException ignored) {}
    }

    private void tickRainEffects(MinecraftServer server) {
        try {
            if (!WeatherConfig.RAIN_EFFECTS_ENABLED.get()) return;
        } catch (IllegalStateException e) { return; }
        if (rainEffects.isEmpty()) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator()) continue;
            ServerLevel level = player.serverLevel();
            if (!level.isRaining()) continue;
            boolean thunder = level.isThundering();

            for (WeatherConfig.RainEffect cfg : rainEffects) {
                if (cfg.weatherType() == WeatherConfig.WeatherType.THUNDER && !thunder) continue;
                if (cfg.requireSkyExposure() && !level.isRainingAt(player.blockPosition())) continue;

                net.minecraft.resources.ResourceLocation id =
                        net.minecraft.resources.ResourceLocation.tryParse(cfg.effectId());
                if (id == null) continue;
                net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> holder =
                        net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
                if (holder == null) continue;

                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        holder, 40, cfg.amplifier(), false, false, false));
            }
        }
    }

    // =========================================================================
    // CAMERA SHAKE (appelé depuis WeatherServerEventHandler sur EntityStruckByLightningEvent)
    // =========================================================================
    public void onLightningStruck(BlockPos lightningPos, ServerLevel level) {
        try {
            if (!WeatherConfig.LIGHTNING_SHAKE_ENABLED.get()) return;

            double maxDist      = WeatherConfig.LIGHTNING_SHAKE_MAX_DISTANCE.get();
            double maxIntensity = WeatherConfig.LIGHTNING_SHAKE_MAX_INTENSITY.get();
            float  duration     = WeatherConfig.LIGHTNING_SHAKE_DURATION_SECONDS.get().floatValue();
            double maxDistSq    = maxDist * maxDist;

            for (ServerPlayer player : level.players()) {
                double distSq = player.blockPosition().distSqr(lightningPos);
                if (distSq > maxDistSq) continue;
                float intensity = (float) (maxIntensity * (1.0 - distSq / maxDistSq));
                PacketDistributor.sendToPlayer(player, new CameraShakePacket(intensity, duration));
            }
        } catch (IllegalStateException ignored) {}
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    /** Retourne le preset fog météo actif, null si aucun. */
    public String getCurrentWeatherFogPreset() {
        try {
            if (!WeatherConfig.ENABLE_WEATHER_SYSTEM.get()) return null;
        } catch (IllegalStateException e) { return null; }
        return currentWeatherFogPreset;
    }

    /** Preset de fog meteo pour la dimension d'un joueur (aucun dans le Nether ou l'End). */
    public String getWeatherFogPreset(ServerLevel level) {
        try {
            if (!WeatherConfig.ENABLE_WEATHER_SYSTEM.get()) return null;
        } catch (IllegalStateException e) { return null; }
        if (!level.dimensionType().hasSkyLight()) return null;
        return weatherFogPresets.get(getCurrentWeather(level));
    }
}