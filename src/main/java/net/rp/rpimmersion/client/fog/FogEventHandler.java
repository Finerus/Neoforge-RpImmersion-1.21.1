package net.rp.rpimmersion.client.fog;

import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.client.ClientPermissions;
import net.rp.rpimmersion.config.ClientImmersionConfig;
import net.rp.rpimmersion.config.FogConfig;

@EventBusSubscriber(modid = RpImmersion.MODID, value = Dist.CLIENT)
public class FogEventHandler {

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        // Sampler le vanilla en premier dans tous les cas.
        ClientFogCache.sampleVanillaColor(
                (float) event.getRed(),
                (float) event.getGreen(),
                (float) event.getBlue()
        );

        if (!shouldApply()) return;

        int flag = colorFlagFor(event.getCamera().getFluidInCamera());
        if ((ClientFogCache.getOverrideFlags() & flag) == 0) return;

        event.setRed(ClientFogCache.getCurrentRed());
        event.setGreen(ClientFogCache.getCurrentGreen());
        event.setBlue(ClientFogCache.getCurrentBlue());
    }

    @SubscribeEvent
    public static void onFogRender(ViewportEvent.RenderFog event) {
        int flag = renderFlagFor(
                event.getCamera().getFluidInCamera(),
                event.getMode()
        );

        boolean applies = shouldApply() && (ClientFogCache.getOverrideFlags() & flag) != 0;

        if (!applies) {
            ClientFogCache.sampleVanillaDistances(
                    event.getNearPlaneDistance(),
                    event.getFarPlaneDistance()
            );
            return;
        }

        event.setNearPlaneDistance(ClientFogCache.getCurrentStart());
        event.setFarPlaneDistance(ClientFogCache.getCurrentEnd());
        event.setCanceled(true);
    }

    private static boolean shouldApply() {
        if (!ClientFogCache.isActive()) return false;
        if (ClientPermissions.canOverrideFog()) {
            try {
                if (ClientImmersionConfig.DISABLE_SERVER_FOG.get()) return false;
            } catch (IllegalStateException ignored) {}
        }
        return true;
    }

    /**
     * Retourne le flag de couleur correspondant au fluide dans lequel se trouve la caméra.
     * Pour FogType.NONE (air), on utilise TERRAIN par défaut car la couleur de terrain
     * et la couleur de ciel sont gérées ensemble côté ComputeFogColor.
     */
    private static int colorFlagFor(FogType type) {
        return switch (type) {
            case WATER       -> FogConfig.FogPreset.FLAG_WATER;
            case LAVA        -> FogConfig.FogPreset.FLAG_LAVA;
            case POWDER_SNOW -> FogConfig.FogPreset.FLAG_POWDER_SNOW;
            default          -> FogConfig.FogPreset.FLAG_TERRAIN;
        };
    }

    /**
     * Retourne le flag de rendu, en distinguant TERRAIN et SKY pour le cas air (NONE).
     */
    private static int renderFlagFor(FogType type, FogRenderer.FogMode mode) {
        return switch (type) {
            case WATER       -> FogConfig.FogPreset.FLAG_WATER;
            case LAVA        -> FogConfig.FogPreset.FLAG_LAVA;
            case POWDER_SNOW -> FogConfig.FogPreset.FLAG_POWDER_SNOW;
            default          -> mode == FogRenderer.FogMode.FOG_SKY
                    ? FogConfig.FogPreset.FLAG_SKY
                    : FogConfig.FogPreset.FLAG_TERRAIN;
        };
    }
}