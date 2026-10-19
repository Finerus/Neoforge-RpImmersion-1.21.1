package net.rp.rpimmersion.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.client.fog.ClientFogCache;
import net.rp.rpimmersion.config.FogConfig;

public record FogSyncPacket(
        boolean active,
        String  presetName,
        float   red, float green, float blue,
        float   start, float end,
        float   transitionSeconds,
        int     overrideFlags
) implements CustomPacketPayload {

    public static final Type<FogSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpImmersion.MODID, "fog_sync"));

    public static final StreamCodec<FriendlyByteBuf, FogSyncPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public FogSyncPacket decode(FriendlyByteBuf buf) {
                    return new FogSyncPacket(
                            buf.readBoolean(), buf.readUtf(),
                            buf.readFloat(), buf.readFloat(), buf.readFloat(),
                            buf.readFloat(), buf.readFloat(), buf.readFloat(),
                            buf.readInt()
                    );
                }

                @Override
                public void encode(FriendlyByteBuf buf, FogSyncPacket p) {
                    buf.writeBoolean(p.active()); buf.writeUtf(p.presetName());
                    buf.writeFloat(p.red()); buf.writeFloat(p.green()); buf.writeFloat(p.blue());
                    buf.writeFloat(p.start()); buf.writeFloat(p.end());
                    buf.writeFloat(p.transitionSeconds());
                    buf.writeInt(p.overrideFlags());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static FogSyncPacket inactive() {
        return new FogSyncPacket(false, "", 0, 0, 0, 0, 0, 0, 0);
    }

    public FogSyncPacket withTransition(float seconds) {
        return new FogSyncPacket(active, presetName, red, green, blue, start, end, seconds, overrideFlags);
    }

    public static FogSyncPacket from(String name, FogConfig.FogPreset p) {
        return new FogSyncPacket(true, name,
                p.red(), p.green(), p.blue(),
                p.start(), p.end(), p.transitionSeconds(),
                p.overrideFlags());
    }

    public static void handleOnClient(FogSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (packet.active()) {
                ClientFogCache.setTargetFog(
                        packet.red(), packet.green(), packet.blue(),
                        packet.start(), packet.end(),
                        packet.transitionSeconds(),
                        packet.overrideFlags()
                );
            } else {
                ClientFogCache.clearFog();
            }
            RpImmersion.LOGGER.debug("[FogSync] CLIENT received — active={} preset='{}'",
                    packet.active(), packet.presetName());
        });
    }
}