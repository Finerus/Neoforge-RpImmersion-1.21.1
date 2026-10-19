package net.rp.rpimmersion.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rp.rpimmersion.RpImmersion;

// Pas de bus= car on register via modEventBus.register(NetworkHandler.class) dans RpImmersion
public class NetworkHandler {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("2");

        reg.playToClient(
                FogSyncPacket.TYPE,
                FogSyncPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(FogSyncPacket::handleOnClient, null)
        );
        reg.playToClient(
                CameraShakePacket.TYPE,
                CameraShakePacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(CameraShakePacket::handleOnClient, null)
        );
        reg.playToClient(
                CalendarSyncPacket.TYPE,
                CalendarSyncPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(CalendarSyncPacket::handleOnClient, null)
        );
        reg.playToClient(
                ClientSettingsPacket.TYPE,
                ClientSettingsPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(ClientSettingsPacket::handleOnClient, null)
        );
    }
}