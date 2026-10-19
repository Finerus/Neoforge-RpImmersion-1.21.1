package net.rp.rpimmersion.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LightningBolt;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.config.WeatherConfig;
import net.rp.rpimmersion.weather.WeatherManager;

import java.util.concurrent.ThreadLocalRandom;

public class WeatherServerEventHandler {

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        WeatherManager.reset();
        WeatherManager.get().reload();
        RpImmersion.LOGGER.info("[RPImmersion] Weather system ready.");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        WeatherManager.reset();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        try {
            if (!WeatherConfig.ENABLE_WEATHER_SYSTEM.get()) return;
        } catch (IllegalStateException e) { return; }
        WeatherManager.get().tick(event.getServer());
    }

    @SubscribeEvent
    public void onLightningJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LightningBolt bolt)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        try {
            if (!WeatherConfig.ENABLE_WEATHER_SYSTEM.get()) return;

            boolean extra = bolt.getTags().contains(WeatherManager.EXTRA_BOLT_TAG);
            if (!extra && bolt.getCause() == null && level.isThundering()
                    && WeatherConfig.LIGHTNING_CONTROL_ENABLED.get()) {
                double multiplier = WeatherConfig.LIGHTNING_FREQUENCY_MULTIPLIER.get();
                if (multiplier < 1.0 && ThreadLocalRandom.current().nextDouble() > multiplier) {
                    event.setCanceled(true);
                    return;
                }
            }
        } catch (IllegalStateException e) { return; }

        WeatherManager.get().onLightningStruck(bolt.blockPosition(), level);
    }
}
