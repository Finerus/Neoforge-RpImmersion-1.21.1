package net.rp.rpimmersion.effects;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.config.EffectsConfig;
import net.rp.rpimmersion.network.BreathPacket;
import net.rp.rpimmersion.network.EffectsSyncPacket;
import net.rp.rpimmersion.network.RespawnFadePacket;
import net.rp.rpimmersion.network.StateSyncPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.rp.rpimmersion.network.EffectsSyncPacket.*;

public class PlayerStateManager {

    public static final int INTERVAL = 10;

    private static final ResourceLocation SLOW_ID = id("state_slow");
    private static final ResourceLocation WEAK_ID = id("state_weakness");
    private static final ResourceLocation FREEZE_ID = id("respawn_freeze");
    private static final ResourceLocation NOJUMP_ID = id("respawn_nojump");

    private static PlayerStateManager instance;
    public static PlayerStateManager get() {
        if (instance == null) instance = new PlayerStateManager();
        return instance;
    }
    public static void reset() { instance = null; }

    private static class State {
        float exertion;
        float sentFatigue = -1f;
        float sentExertion = -1f;
        boolean heavy;
        long immobileUntil;
        long weakUntil;
        long weakLength = 1;
    }

    private final Map<UUID, State> states = new HashMap<>();

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RpImmersion.MODID, path);
    }

    private static boolean enabled() {
        try { return EffectsConfig.ENABLE_EFFECTS.get(); }
        catch (IllegalStateException e) { return false; }
    }

    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    private static double combine(double... parts) {
        double keep = 1.0;
        for (double x : parts) keep *= 1.0 - x;
        return 1.0 - keep;
    }

    // =========================================================================
    // TICK (toutes les INTERVAL ticks)
    // =========================================================================

    public void tick(MinecraftServer server) {
        boolean on = enabled();
        long now = server.getTickCount();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!on || p.isCreative() || p.isSpectator()) { clearModifiers(p); continue; }
            if (p.isAlive()) update(p, states.computeIfAbsent(p.getUUID(), k -> new State()), now);
        }
    }

    private void update(ServerPlayer p, State s, long now) {
        try {
            double breathThr = EffectsConfig.BREATH_THRESHOLD.get();

            if (EffectsConfig.EXERTION_ENABLED.get()) {
                float dt = INTERVAL / 20f;
                s.exertion += p.isSprinting()
                        ? dt / EffectsConfig.SPRINT_EXHAUST_SECONDS.get()
                        : -dt / EffectsConfig.EXERTION_RECOVER_SECONDS.get();
                s.exertion = Math.max(0f, Math.min(1f, s.exertion));
            } else {
                s.exertion = 0f;
            }
            double exertLevel = clamp01((s.exertion - breathThr) / Math.max(0.01, 1.0 - breathThr));

            double low = 0.0;
            if (EffectsConfig.LOW_HEALTH_ENABLED.get() && p.getMaxHealth() > 0f) {
                double thr = EffectsConfig.LOW_HEALTH_THRESHOLD.get();
                low = clamp01((thr - p.getHealth() / p.getMaxHealth()) / thr);
            }

            float fatigue = 0f;
            if (EffectsConfig.FATIGUE_ENABLED.get()) {
                double minutes = p.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)) / 1200.0;
                double start = EffectsConfig.FATIGUE_START_MINUTES.get();
                double full = Math.max(start + 1.0, EffectsConfig.FATIGUE_FULL_MINUTES.get());
                fatigue = (float) clamp01((minutes - start) / (full - start));
            }

            boolean frozen = now < s.immobileUntil;
            double respawnFactor = (!frozen && now < s.weakUntil) ? (double) (s.weakUntil - now) / s.weakLength : 0.0;

            double slow = combine(low * EffectsConfig.LOW_HEALTH_SLOW.get(),
                    fatigue * EffectsConfig.FATIGUE_SLOW.get(),
                    exertLevel * EffectsConfig.EXHAUSTED_SLOW.get(),
                    respawnFactor * EffectsConfig.RESPAWN_SLOW.get());
            double weak = combine(fatigue * EffectsConfig.FATIGUE_WEAKNESS.get(),
                    exertLevel * EffectsConfig.EXHAUSTED_WEAKNESS.get());

            setModifier(p, Attributes.MOVEMENT_SPEED, SLOW_ID, -slow);
            setModifier(p, Attributes.ATTACK_DAMAGE, WEAK_ID, -weak);
            setModifier(p, Attributes.MOVEMENT_SPEED, FREEZE_ID, frozen ? -1.0 : 0.0);
            setModifier(p, Attributes.JUMP_STRENGTH, NOJUMP_ID, frozen ? -1.0 : 0.0);

            float fq = Math.round(fatigue * 20f) / 20f;
            float eq = Math.round(s.exertion * 20f) / 20f;
            if (fq != s.sentFatigue || eq != s.sentExertion) {
                s.sentFatigue = fq;
                s.sentExertion = eq;
                PacketDistributor.sendToPlayer(p, new StateSyncPacket(fq, eq));
            }

            boolean heavy = s.exertion > 0.001f && s.exertion >= breathThr;
            if (heavy != s.heavy) {
                s.heavy = heavy;
                PacketDistributor.sendToPlayersTrackingEntity(p, new BreathPacket(p.getUUID(), heavy));
            }
        } catch (IllegalStateException ignored) {
            // config pas encore chargée
        }
    }

    private static void setModifier(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id, double amount) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier current = inst.getModifier(id);
        if (amount > -0.005) {
            if (current != null) inst.removeModifier(id);
            return;
        }
        if (current != null && Math.abs(current.amount() - amount) < 0.005) return;
        inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void clearModifiers(ServerPlayer p) {
        setModifier(p, Attributes.MOVEMENT_SPEED, SLOW_ID, 0.0);
        setModifier(p, Attributes.ATTACK_DAMAGE, WEAK_ID, 0.0);
        setModifier(p, Attributes.MOVEMENT_SPEED, FREEZE_ID, 0.0);
        setModifier(p, Attributes.JUMP_STRENGTH, NOJUMP_ID, 0.0);
    }

    // =========================================================================
    // ÉVÉNEMENTS JOUEUR
    // =========================================================================

    public void onLogin(ServerPlayer p) {
        states.put(p.getUUID(), new State());
        sendSettings(p);
    }

    public void onLogout(ServerPlayer p) {
        states.remove(p.getUUID());
    }

    public void onJump(ServerPlayer p) {
        State s = states.get(p.getUUID());
        if (s == null || !p.isSprinting()) return;
        try {
            if (enabled() && EffectsConfig.EXERTION_ENABLED.get())
                s.exertion = (float) Math.min(1.0, s.exertion + EffectsConfig.JUMP_EXERTION.get());
        } catch (IllegalStateException ignored) {}
    }

    public void onRespawn(ServerPlayer p) {
        try {
            if (!enabled() || !EffectsConfig.RESPAWN_ENABLED.get()) return;
            int frozenTicks = EffectsConfig.RESPAWN_IMMOBILE_SECONDS.get() * 20;
            int weakTicks = EffectsConfig.RESPAWN_WEAK_SECONDS.get() * 20;
            State s = new State();
            long now = p.getServer().getTickCount();
            s.immobileUntil = now + frozenTicks;
            s.weakLength = Math.max(1, weakTicks);
            s.weakUntil = s.immobileUntil + weakTicks;
            states.put(p.getUUID(), s);
            if (frozenTicks > 0) {
                setModifier(p, Attributes.MOVEMENT_SPEED, FREEZE_ID, -1.0);
                setModifier(p, Attributes.JUMP_STRENGTH, NOJUMP_ID, -1.0);
                PacketDistributor.sendToPlayer(p, new RespawnFadePacket(frozenTicks));
            }
        } catch (IllegalStateException ignored) {}
    }

    public void resetPlayer(ServerPlayer p) {
        states.put(p.getUUID(), new State());
        clearModifiers(p);
    }

    public int trackedCount() { return states.size(); }

    // =========================================================================
    // SYNC DES RÉGLAGES
    // =========================================================================

    public void sendSettings(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, buildSettings());
    }

    public void syncSettingsToAll(MinecraftServer server) {
        EffectsSyncPacket packet = buildSettings();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) PacketDistributor.sendToPlayer(p, packet);
    }

    private static float b(boolean v) { return v ? 1f : 0f; }

    private static EffectsSyncPacket buildSettings() {
        float[] v = new float[COUNT];
        List<String> blocks = new ArrayList<>();
        try {
            v[ENABLED] = b(EffectsConfig.ENABLE_EFFECTS.get());
            v[ALLOW_OVERRIDE] = b(EffectsConfig.ALLOW_CLIENT_EFFECTS_OVERRIDE.get());
            v[TEMP] = b(EffectsConfig.TEMPERATURE_ENABLED.get());
            v[COLD_THR] = EffectsConfig.COLD_THRESHOLD.get().floatValue();
            v[HOT_THR] = EffectsConfig.HOT_THRESHOLD.get().floatValue();
            v[COLD_TINT] = EffectsConfig.COLD_TINT.get().floatValue();
            v[HOT_TINT] = EffectsConfig.HOT_TINT.get().floatValue();
            v[COLD_FOV] = EffectsConfig.COLD_FOV_REDUCTION.get().floatValue();
            v[TREMBLE] = EffectsConfig.HAND_TREMBLE.get().floatValue();
            v[COLD_BREATH] = b(EffectsConfig.COLD_BREATH.get());
            v[LOW_ON] = b(EffectsConfig.LOW_HEALTH_ENABLED.get());
            v[LOW_THR] = EffectsConfig.LOW_HEALTH_THRESHOLD.get().floatValue();
            v[LOW_VIG] = EffectsConfig.LOW_HEALTH_VIGNETTE.get().floatValue();
            v[LOW_CAM] = EffectsConfig.LOW_HEALTH_CAMERA.get().floatValue();
            v[FAT_VIG] = EffectsConfig.FATIGUE_VIGNETTE.get().floatValue();
            v[FAT_BLINK] = b(EffectsConfig.FATIGUE_BLINK.get());
            v[EXERT_ON] = b(EffectsConfig.EXERTION_ENABLED.get());
            v[BREATH_THR] = EffectsConfig.BREATH_THRESHOLD.get().floatValue();
            v[STUMBLE_ON] = b(EffectsConfig.STUMBLE_ENABLED.get());
            v[STUMBLE_CHANCE] = EffectsConfig.STUMBLE_CHANCE.get().floatValue();
            v[STUMBLE_INT] = EffectsConfig.STUMBLE_INTENSITY.get().floatValue();
            v[FALL_ON] = b(EffectsConfig.FALL_CAMERA_ENABLED.get());
            v[FALL_MIN] = EffectsConfig.FALL_MIN_DISTANCE.get().floatValue();
            v[FALL_INT] = EffectsConfig.FALL_CAMERA_INTENSITY.get().floatValue();
            blocks.addAll(EffectsConfig.STUMBLE_BLOCKS.get());
        } catch (IllegalStateException e) {
            v = new float[COUNT];
        }
        return new EffectsSyncPacket(v, blocks);
    }
}