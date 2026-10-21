package net.rp.rpimmersion.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.client.effects.ClientEffects;
import net.rp.rpimmersion.config.EventsConfig;

public record EventSyncPacket(boolean active, int rgb, float opacity, float darkness,
                              float fadeSeconds, String particle) implements CustomPacketPayload {

    public static final Type<EventSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpImmersion.MODID, "event_sync"));

    public static final StreamCodec<ByteBuf, EventSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, EventSyncPacket::active,
            ByteBufCodecs.INT, EventSyncPacket::rgb,
            ByteBufCodecs.FLOAT, EventSyncPacket::opacity,
            ByteBufCodecs.FLOAT, EventSyncPacket::darkness,
            ByteBufCodecs.FLOAT, EventSyncPacket::fadeSeconds,
            ByteBufCodecs.STRING_UTF8, EventSyncPacket::particle,
            EventSyncPacket::new
    );

    public static EventSyncPacket from(EventsConfig.EventDef d) {
        return new EventSyncPacket(true, d.rgb(), d.opacity(), d.darkness(), d.transitionSeconds(), d.particle());
    }

    public static EventSyncPacket inactive(float fadeSeconds) {
        return new EventSyncPacket(false, 0, 0f, 0f, fadeSeconds, "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handleOnClient(EventSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientEffects.setEvent(packet.active(), packet.rgb(), packet.opacity(),
                packet.darkness(), packet.fadeSeconds(), packet.particle()));
    }
}