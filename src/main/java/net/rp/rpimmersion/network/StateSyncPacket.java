package net.rp.rpimmersion.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.client.effects.ClientEffects;

public record StateSyncPacket(float fatigue, float exertion) implements CustomPacketPayload {

    public static final Type<StateSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpImmersion.MODID, "state_sync"));

    public static final StreamCodec<ByteBuf, StateSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, StateSyncPacket::fatigue,
            ByteBufCodecs.FLOAT, StateSyncPacket::exertion,
            StateSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handleOnClient(StateSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientEffects.setState(packet.fatigue(), packet.exertion()));
    }
}