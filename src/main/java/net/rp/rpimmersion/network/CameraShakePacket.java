package net.rp.rpimmersion.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.client.camera.CameraShakeHandler;

/**
 * Packet SERVEUR → CLIENT.
 * Déclenche un shake de caméra avec l'intensité et la durée calculées côté serveur
 * (basées sur la distance à l'éclair).
 */
public record CameraShakePacket(float intensity, float durationSeconds) implements CustomPacketPayload {

    public static final Type<CameraShakePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpImmersion.MODID, "camera_shake"));

    public static final StreamCodec<ByteBuf, CameraShakePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, CameraShakePacket::intensity,
            ByteBufCodecs.FLOAT, CameraShakePacket::durationSeconds,
            CameraShakePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handleOnClient(CameraShakePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CameraShakeHandler.trigger(packet.intensity(), packet.durationSeconds()));
    }
}
