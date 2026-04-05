package net.rp.rpimmersion.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.rp.rpimmersion.RpImmersion;

/**
 * Intégration optionnelle avec ImmersiveMessages (toni.immersivemessages).
 * Si le mod n'est pas présent, repli transparent sur ACTION_BAR ou CHAT.
 *
 * COMPATIBILITÉ : ImmersiveMessages est déjà requis par RpEssentials côté client.
 * On l'utilise ici uniquement quand il est disponible — pas de crash sinon.
 */
public class ImmersiveMessagesCompat {

    private static final boolean AVAILABLE;

    static {
        boolean found = false;
        try {
            Class.forName("toni.immersivemessages.api.ImmersiveMessage");
            found = true;
        } catch (ClassNotFoundException ignored) {}
        AVAILABLE = found;
        if (AVAILABLE) RpImmersion.LOGGER.info("[RPImmersion] ImmersiveMessages detected.");
    }

    /**
     * Envoie un message au joueur selon le mode configuré.
     * mode : CHAT | ACTION_BAR | IMMERSIVE
     * Repli : IMMERSIVE → ACTION_BAR si ImmersiveMessages absent.
     */
    public static void sendMessage(ServerPlayer player, String rawMessage, String mode) {
        Component component = net.rp.rpimmersion.util.ColorUtil.parse(rawMessage);
        switch (mode.toUpperCase()) {
            case "IMMERSIVE" -> {
                if (AVAILABLE) {
                    try {
                        toni.immersivemessages.api.ImmersiveMessage
                                .builder(4f, rawMessage)
                                .fadeIn(0.5f).fadeOut(0.5f)
                                .sendServer(player);
                        return;
                    } catch (Exception | NoClassDefFoundError e) {
                        // Fallback
                    }
                }
                player.displayClientMessage(component, true); // ACTION_BAR fallback
            }
            case "ACTION_BAR" -> player.displayClientMessage(component, true);
            default           -> player.sendSystemMessage(component); // CHAT
        }
    }

    public static boolean isAvailable() { return AVAILABLE; }
}
