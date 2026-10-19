package net.rp.rpimmersion.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.client.ClientHudState;
import net.rp.rpimmersion.client.ClientPermissions;

import java.util.List;

/**
 * Packet SERVEUR vers CLIENT.
 * Remplace WeatherSyncPacket : permissions d'override, HUD masque et verrou camera.
 * Envoye a la connexion, au reload et a chaque changement via commande.
 */
public record ClientSettingsPacket(
        boolean allowShakeOverride,
        boolean allowFogOverride,
        boolean forceFirstPerson,
        List<String> hiddenHud
) implements CustomPacketPayload {

    public static final Type<ClientSettingsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpImmersion.MODID, "client_settings"));

    public static final StreamCodec<ByteBuf, ClientSettingsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ClientSettingsPacket::allowShakeOverride,
            ByteBufCodecs.BOOL, ClientSettingsPacket::allowFogOverride,
            ByteBufCodecs.BOOL, ClientSettingsPacket::forceFirstPerson,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(64)), ClientSettingsPacket::hiddenHud,
            ClientSettingsPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handleOnClient(ClientSettingsPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientPermissions.setAllowShakeOverride(packet.allowShakeOverride());
            ClientPermissions.setAllowFogOverride(packet.allowFogOverride());
            ClientHudState.update(packet.forceFirstPerson(), packet.hiddenHud());
        });
    }
}
