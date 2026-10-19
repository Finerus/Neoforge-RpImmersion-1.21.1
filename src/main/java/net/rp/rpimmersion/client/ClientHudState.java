package net.rp.rpimmersion.client;

import java.util.Collection;
import java.util.Set;

/**
 * Etat client recu via ClientSettingsPacket.
 * Aucune classe client Minecraft ici pour rester chargeable cote serveur.
 */
public class ClientHudState {

    private static volatile Set<String> hidden = Set.of();
    private static volatile boolean forceFirstPerson = false;

    public static void update(boolean firstPerson, Collection<String> elements) {
        forceFirstPerson = firstPerson;
        hidden = Set.copyOf(elements);
    }

    public static boolean isHidden(String element) { return hidden.contains(element); }

    public static boolean isForceFirstPerson() { return forceFirstPerson; }

    public static void reset() {
        hidden = Set.of();
        forceFirstPerson = false;
    }
}
