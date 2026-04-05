package net.rp.rpimmersion.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rp.rpimmersion.RpImmersion;

/**
 * Packet SERVEUR → CLIENT.
 * Transmet les permissions client (shake override, fog override) à la connexion.
 * Envoyé une seule fois à la connexion et lors d'un reload config.
 */
public record WeatherSyncPacket(
        boolean allowShakeOverride,
        boolean allowFogOverride
) implements CustomPacketPayload {

    public static final Type<WeatherSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpImmersion.MODID, "weather_sync"));

    public static final StreamCodec<ByteBuf, WeatherSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, WeatherSyncPacket::allowShakeOverride,
            ByteBufCodecs.BOOL, WeatherSyncPacket::allowFogOverride,
            WeatherSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handleOnClient(WeatherSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            net.rp.rpimmersion.client.ClientPermissions.setAllowShakeOverride(packet.allowShakeOverride());
            net.rp.rpimmersion.client.ClientPermissions.setAllowFogOverride(packet.allowFogOverride());
        });
    }
}
