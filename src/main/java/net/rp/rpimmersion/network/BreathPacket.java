package net.rp.rpimmersion.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.client.effects.ClientEffects;

import java.util.UUID;

public record BreathPacket(UUID player, boolean heavy) implements CustomPacketPayload {

    public static final Type<BreathPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpImmersion.MODID, "breath"));

    public static final StreamCodec<ByteBuf, BreathPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, BreathPacket::player,
            ByteBufCodecs.BOOL, BreathPacket::heavy,
            BreathPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handleOnClient(BreathPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientEffects.setBreath(packet.player(), packet.heavy()));
    }
}