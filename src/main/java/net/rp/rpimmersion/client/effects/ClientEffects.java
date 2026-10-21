package net.rp.rpimmersion.client.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rp.rpimmersion.PatternUtils;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.config.ClientImmersionConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static net.rp.rpimmersion.network.EffectsSyncPacket.*;

@EventBusSubscriber(modid = RpImmersion.MODID, value = Dist.CLIENT)
public class ClientEffects {

    private static final Random RNG = new Random();
    private static final int STUMBLE_LEN = 14;
    private static final int IMPACT_LEN = 18;
    private static final int BLINK_LEN = 24;

    // Reçu du serveur
    private static float[] settings = new float[COUNT];
    private static List<String> stumbleBlocks = List.of();
    private static float fatigue, exertion;
    private static int respawnTotal, respawnLeft;
    private static final Set<UUID> heavyBreathers = new HashSet<>();

    private static int eventRgb;
    private static float eventOpacityTarget, eventDarknessTarget, eventOpacity, eventDarkness;
    private static float eventFade = 3f;
    private static String eventParticle = "";

    // Calculé en local
    private static float cold, hot, coldTarget, hotTarget, low, exertLevel, impactAmp, peakFall;
    private static int stumbleAge = STUMBLE_LEN, impactAge = IMPACT_LEN, blinkAge = BLINK_LEN, tick;

    // =========================================================================
    // API APPELÉE PAR LES PACKETS
    // =========================================================================

    public static void applySettings(float[] values, List<String> blocks) {
        if (values.length == COUNT) settings = values;
        stumbleBlocks = blocks;
    }

    public static void setState(float fat, float exert) { fatigue = fat; exertion = exert; }

    public static void startRespawnFade(int ticks) { respawnTotal = ticks; respawnLeft = ticks; }

    public static void setBreath(UUID id, boolean heavy) {
        if (heavy) heavyBreathers.add(id); else heavyBreathers.remove(id);
    }

    public static void setEvent(boolean active, int rgb, float opacity, float darkness, float fade, String particle) {
        eventFade = Math.max(0.1f, fade);
        if (active) {
            eventRgb = rgb;
            eventOpacityTarget = opacity;
            eventDarknessTarget = darkness;
            eventParticle = particle;
        } else {
            eventOpacityTarget = 0f;
            eventDarknessTarget = 0f;
            eventParticle = "";
        }
    }

    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        settings = new float[COUNT];
        stumbleBlocks = List.of();
        fatigue = exertion = 0f;
        respawnTotal = respawnLeft = 0;
        heavyBreathers.clear();
        eventOpacity = eventDarkness = eventOpacityTarget = eventDarknessTarget = 0f;
        eventParticle = "";
        cold = hot = coldTarget = hotTarget = low = exertLevel = peakFall = 0f;
        stumbleAge = STUMBLE_LEN;
        impactAge = IMPACT_LEN;
        blinkAge = BLINK_LEN;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private static boolean on(int i) { return settings[i] > 0.5f; }
    private static float f(int i) { return settings[i]; }
    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static float scale() {
        if (!on(ALLOW_OVERRIDE)) return 1f;
        try { return ClientImmersionConfig.EFFECTS_INTENSITY_MULTIPLIER.get().floatValue(); }
        catch (IllegalStateException e) { return 1f; }
    }

    private static float dip(float x) {
        x = Math.min(1f, x);
        return (float) (Math.sin(Math.PI * x) * (1f - x));
    }

    // =========================================================================
    // TICK
    // =========================================================================

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.isPaused()) return;

        float k = Math.min(1f, 3f / Math.max(1f, eventFade * 20f));
        eventOpacity += (eventOpacityTarget - eventOpacity) * k;
        eventDarkness += (eventDarknessTarget - eventDarkness) * k;
        if (respawnLeft > 0) respawnLeft--;
        if (!eventParticle.isEmpty()) eventParticles(mc, p);

        if (!on(ENABLED)) {
            cold = hot = low = exertLevel = 0f;
            return;
        }
        tick++;

        if (on(TEMP)) {
            float temp = mc.level.getBiome(p.blockPosition()).value().getBaseTemperature();
            coldTarget = clamp01((f(COLD_THR) - temp) / 0.5f);
            hotTarget = clamp01((temp - f(HOT_THR)) / 0.5f);
        } else {
            coldTarget = 0f;
            hotTarget = 0f;
        }
        cold += (coldTarget - cold) * 0.03f;
        hot += (hotTarget - hot) * 0.03f;

        float lowTarget = 0f;
        if (on(LOW_ON) && p.getMaxHealth() > 0f)
            lowTarget = clamp01((f(LOW_THR) - p.getHealth() / p.getMaxHealth()) / Math.max(0.01f, f(LOW_THR)));
        low += (lowTarget - low) * 0.15f;

        exertLevel = on(EXERT_ON) ? clamp01((exertion - f(BREATH_THR)) / Math.max(0.01f, 1f - f(BREATH_THR))) : 0f;

        if (on(FALL_ON)) {
            if (!p.onGround() && !p.isInWater() && !p.getAbilities().flying && !p.isFallFlying()) {
                peakFall = Math.max(peakFall, p.fallDistance);
            } else {
                if (p.onGround() && peakFall >= f(FALL_MIN)) {
                    impactAge = 0;
                    impactAmp = Math.min(5f, 0.8f + (peakFall - f(FALL_MIN)) * 0.3f) * f(FALL_INT);
                }
                peakFall = 0f;
            }
        }
        if (impactAge < IMPACT_LEN) impactAge++;

        if (on(STUMBLE_ON) && stumbleAge >= STUMBLE_LEN && p.onGround() && !p.isInWater()
                && p.getDeltaMovement().horizontalDistanceSqr() > 0.0009) {
            float chance = f(STUMBLE_CHANCE) / 20f * (p.isSprinting() ? 2f : 1f);
            if (RNG.nextFloat() < chance && onStumbleBlock(mc, p)) stumbleAge = 0;
        }
        if (stumbleAge < STUMBLE_LEN) stumbleAge++;

        if (on(FAT_BLINK) && fatigue > 0.5f && blinkAge >= BLINK_LEN && RNG.nextFloat() < fatigue * fatigue * 0.002f)
            blinkAge = 0;
        if (blinkAge < BLINK_LEN) blinkAge++;

        if (tick % 5 == 0) breath(mc, p);
    }

    private static boolean onStumbleBlock(Minecraft mc, LocalPlayer p) {
        BlockPos pos = p.blockPosition();
        for (int i = 0; i < 2; i++) {
            BlockPos bp = i == 0 ? pos : pos.below();
            String id = BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(bp).getBlock()).toString();
            for (String pattern : stumbleBlocks) if (PatternUtils.matches(id, pattern)) return true;
        }
        return false;
    }

    private static void breath(Minecraft mc, LocalPlayer self) {
        for (AbstractClientPlayer q : mc.level.players()) {
            if (q.distanceToSqr(self) > 576.0 || q.isUnderWater() || q.isSpectator()) continue;
            boolean heavy = q == self
                    ? on(EXERT_ON) && exertion > 0.001f && exertion >= f(BREATH_THR)
                    : heavyBreathers.contains(q.getUUID());
            boolean chilly = on(TEMP) && on(COLD_BREATH)
                    && q.level().getBiome(q.blockPosition()).value().getBaseTemperature() < f(COLD_THR);
            if ((chilly && RNG.nextFloat() < 0.07f) || (heavy && RNG.nextFloat() < 0.22f)) puff(q);
        }
    }

    private static void puff(AbstractClientPlayer q) {
        Vec3 look = q.getLookAngle();
        Vec3 pos = q.getEyePosition().add(look.scale(0.35)).add(0.0, -0.22, 0.0);
        q.level().addParticle(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, look.x * 0.03, 0.012, look.z * 0.03);
    }

    private static void eventParticles(Minecraft mc, LocalPlayer p) {
        String raw = eventParticle.contains(":") ? eventParticle : "minecraft:" + eventParticle;
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null || !(BuiltInRegistries.PARTICLE_TYPE.get(id) instanceof SimpleParticleType type)) return;
        for (int i = 0; i < 3; i++) {
            double x = p.getX() + (RNG.nextDouble() - 0.5) * 24.0;
            double y = p.getY() + 2.0 + RNG.nextDouble() * 8.0;
            double z = p.getZ() + (RNG.nextDouble() - 0.5) * 24.0;
            mc.level.addParticle(type, x, y, z, 0.0, -0.02, 0.0);
        }
    }

    // =========================================================================
    // CAMERA, FOV, MAINS (additif, comme CameraShakeHandler)
    // =========================================================================

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!on(ENABLED)) return;
        float sc = scale();
        if (sc <= 0f) return;

        double t = System.nanoTime() / 1.0e9;
        float partial = (float) event.getPartialTick();
        float camLow = f(LOW_CAM);
        float pitch = 0f;
        float roll = 0f;

        pitch += (float) Math.sin(t * 2.1) * 0.45f * low * camLow;
        roll += (float) Math.sin(t * 1.1) * 0.8f * low * camLow;
        pitch += (float) Math.sin(t * 4.5) * 0.5f * exertLevel;
        roll += (float) Math.sin(t * 0.7) * 0.4f * hot;
        if (stumbleAge < STUMBLE_LEN) pitch += dip((stumbleAge + partial) / STUMBLE_LEN) * 2.5f * f(STUMBLE_INT);
        if (impactAge < IMPACT_LEN) pitch += dip((impactAge + partial) / IMPACT_LEN) * impactAmp;

        event.setPitch(event.getPitch() + pitch * sc);
        event.setRoll(event.getRoll() + roll * sc);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (!on(ENABLED) || cold <= 0.001f) return;
        event.setFOV(event.getFOV() * (1.0 - f(COLD_FOV) * cold * scale()));
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!on(ENABLED) || event.getHand() != InteractionHand.MAIN_HAND) return;
        float amp = Math.max(on(TEMP) ? cold * f(TREMBLE) : 0f, exertLevel * 0.8f) * 0.004f * scale();
        if (amp <= 0.0001f) return;
        double t = System.nanoTime() / 1.0e9;
        event.getPoseStack().translate(Math.sin(t * 37.0) * amp, Math.sin(t * 29.0 + 1.3) * amp, 0.0);
    }

    // =========================================================================
    // OVERLAYS
    // =========================================================================
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        float sc = scale();
        if (sc <= 0f) return;
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();
        double t = System.nanoTime() / 1.0e9;

        fill(g, w, h, 0x000000, eventDarkness * sc);
        fill(g, w, h, eventRgb, eventOpacity * sc);

        if (on(ENABLED)) {
            // La faible vie estompe les teintes de température
            float calm = 1f - Math.min(1f, low * 1.5f);
            fill(g, w, h, 0xA9BCD0, cold * f(COLD_TINT) * calm * sc);
            fill(g, w, h, 0xFFA040, hot * f(HOT_TINT) * calm * sc);

            // Vignettes fusionnées en un seul dessin
            float pulse = 0.8f + 0.2f * (float) Math.sin(t * (4.0 + 4.0 * low));
            float lowA = Math.min(1f, low * f(LOW_VIG) * pulse * sc);
            float fatA = Math.min(1f, fatigue * f(FAT_VIG) * sc);
            float total = 1f - (1f - lowA) * (1f - fatA);
            vignette(g, w, h, mix(0xAA0000, lowA, 0x000000, fatA), total);

            if (blinkAge < BLINK_LEN)
                fill(g, w, h, 0x000000, (float) Math.sin(Math.PI * blinkAge / BLINK_LEN) * 0.95f * sc);
        }
        if (respawnLeft > 0 && respawnTotal > 0)
            fill(g, w, h, 0x000000, (float) respawnLeft / respawnTotal * sc);
    }

    private static int mix(int c1, float a1, int c2, float a2) {
        float sum = a1 + a2;
        if (sum <= 0f) return 0;
        float k = a1 / sum;
        int r = (int) (((c1 >> 16) & 255) * k + ((c2 >> 16) & 255) * (1f - k));
        int gr = (int) (((c1 >> 8) & 255) * k + ((c2 >> 8) & 255) * (1f - k));
        int b = (int) ((c1 & 255) * k + (c2 & 255) * (1f - k));
        return (r << 16) | (gr << 8) | b;
    }

    private static void fill(GuiGraphics g, int w, int h, int rgb, float alpha) {
        int a = (int) (Math.min(1f, alpha) * 255f);
        if (a < 2) return;
        g.fill(0, 0, w, h, (a << 24) | (rgb & 0xFFFFFF));
    }

    private static void vignette(GuiGraphics g, int w, int h, int rgb, float alpha) {
        if (alpha < 0.01f) return;
        int steps = 16;
        int band = Math.max(2, (int) (Math.min(w, h) * 0.3f / steps));
        for (int i = 0; i < steps; i++) {
            float k = 1f - i / (float) steps;
            int a = (int) (Math.min(1f, alpha) * k * k * 255f);
            if (a < 2) break;
            int c = (a << 24) | (rgb & 0xFFFFFF);
            int o = i * band;
            g.fill(o, o, w - o, o + band, c);
            g.fill(o, h - o - band, w - o, h - o, c);
            g.fill(o, o + band, o + band, h - o - band, c);
            g.fill(w - o - band, o + band, w - o, h - o - band, c);
        }
    }
}