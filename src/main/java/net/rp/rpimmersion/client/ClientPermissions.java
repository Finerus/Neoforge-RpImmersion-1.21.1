package net.rp.rpimmersion.client;

/**
 * Flags de permission envoyés par le serveur à la connexion.
 * Détermine si le client peut override les réglages serveur.
 */
public class ClientPermissions {
    private static boolean allowShakeOverride = false;
    private static boolean allowFogOverride   = false;

    public static void setAllowShakeOverride(boolean v) { allowShakeOverride = v; }
    public static void setAllowFogOverride(boolean v)   { allowFogOverride   = v; }
    public static boolean canOverrideShake()             { return allowShakeOverride; }
    public static boolean canOverrideFog()               { return allowFogOverride; }

    public static void reset() { allowShakeOverride = false; allowFogOverride = false; }
}
