package net.rp.rpimmersion.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.client.effects.ClientEffects;

import java.util.ArrayList;
import java.util.List;

public record EffectsSyncPacket(float[] values, List<String> stumbleBlocks) implements CustomPacketPayload {

    public static final int ENABLED = 0, ALLOW_OVERRIDE = 1, TEMP = 2, COLD_THR = 3, HOT_THR = 4, COLD_TINT = 5,
            HOT_TINT = 6, COLD_FOV = 7, TREMBLE = 8, COLD_BREATH = 9, LOW_ON = 10, LOW_THR = 11, LOW_VIG = 12,
            LOW_CAM = 13, FAT_VIG = 14, FAT_BLINK = 15, EXERT_ON = 16, BREATH_THR = 17, STUMBLE_ON = 18,
            STUMBLE_CHANCE = 19, STUMBLE_INT = 20, FALL_ON = 21, FALL_MIN = 22, FALL_INT = 23, COUNT = 24;

    public static final Type<EffectsSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpImmersion.MODID, "effects_sync"));

    public static final StreamCodec<FriendlyByteBuf, EffectsSyncPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public EffectsSyncPacket decode(FriendlyByteBuf buf) {
                    float[] v = new float[COUNT];
                    int n = buf.readVarInt();
                    for (int i = 0; i < n; i++) {
                        float x = buf.readFloat();
                        if (i < COUNT) v[i] = x;
                    }
                    int m = Math.min(buf.readVarInt(), 256);
                    List<String> blocks = new ArrayList<>(m);
                    for (int i = 0; i < m; i++) blocks.add(buf.readUtf());
                    return new EffectsSyncPacket(v, blocks);
                }

                @Override
                public void encode(FriendlyByteBuf buf, EffectsSyncPacket p) {
                    buf.writeVarInt(p.values().length);
                    for (float x : p.values()) buf.writeFloat(x);
                    buf.writeVarInt(p.stumbleBlocks().size());
                    for (String s : p.stumbleBlocks()) buf.writeUtf(s);
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handleOnClient(EffectsSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientEffects.applySettings(packet.values(), packet.stumbleBlocks()));
    }
}