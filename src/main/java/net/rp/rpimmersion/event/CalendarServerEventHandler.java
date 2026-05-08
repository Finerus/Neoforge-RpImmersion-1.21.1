package net.rp.rpimmersion.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.calendar.CalendarManager;
import net.rp.rpimmersion.config.CalendarConfig;

public class CalendarServerEventHandler {

    private int scheduleCheckTick = 0;

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        CalendarManager.reset();
        CalendarManager.get().initForServer(event.getServer());
        RpImmersion.LOGGER.info("[RPImmersion] Calendar system ready.");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        CalendarManager.get().save();
        CalendarManager.reset();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        try {
            if (!CalendarConfig.ENABLE_CALENDAR.get()) return;
        } catch (IllegalStateException e) { return; }

        // Vérification du planning RpEssentials ~1x/seconde
        if (++scheduleCheckTick >= 20) {
            scheduleCheckTick = 0;
            CalendarManager.get().updateSchedulePause(event.getServer());
        }

        CalendarManager.get().tick(event.getServer());
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            if (!CalendarConfig.ENABLE_CALENDAR.get()) return;
        } catch (IllegalStateException e) { return; }
        CalendarManager.get().syncToPlayer(player);
    }
}