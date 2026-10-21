package net.rp.rpimmersion.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.client.effects.ClientEffects;

public record RespawnFadePacket(int ticks) implements CustomPacketPayload {

    public static final Type<RespawnFadePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpImmersion.MODID, "respawn_fade"));

    public static final StreamCodec<ByteBuf, RespawnFadePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RespawnFadePacket::ticks,
            RespawnFadePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handleOnClient(RespawnFadePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientEffects.startRespawnFade(packet.ticks()));
    }
}