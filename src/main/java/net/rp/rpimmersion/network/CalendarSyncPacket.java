package net.rp.rpimmersion.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.client.hud.CalendarHudCache;

/**
 * Packet SERVEUR → CLIENT.
 * Transmet l'état complet du calendrier (date, heure, pause) au client.
 * Envoyé à la connexion, au reload, et à chaque changement de date/pause.
 */
public record CalendarSyncPacket(
        int  year, int month, int day,
        int  hour, int minute,
        int  dayOfWeek,    // -1 si pas de semaine configurée
        boolean paused,
        String  displayString // Chaîne pré-formatée côté serveur
) implements CustomPacketPayload {

    public static final Type<CalendarSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpImmersion.MODID, "calendar_sync"));

    public static final StreamCodec<FriendlyByteBuf, CalendarSyncPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public CalendarSyncPacket decode(FriendlyByteBuf buf) {
                    return new CalendarSyncPacket(
                            buf.readInt(), buf.readInt(), buf.readInt(),
                            buf.readInt(), buf.readInt(), buf.readInt(),
                            buf.readBoolean(), buf.readUtf()
                    );
                }
                @Override
                public void encode(FriendlyByteBuf buf, CalendarSyncPacket p) {
                    buf.writeInt(p.year()); buf.writeInt(p.month()); buf.writeInt(p.day());
                    buf.writeInt(p.hour()); buf.writeInt(p.minute()); buf.writeInt(p.dayOfWeek());
                    buf.writeBoolean(p.paused()); buf.writeUtf(p.displayString());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handleOnClient(CalendarSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CalendarHudCache.update(packet));
    }
}
