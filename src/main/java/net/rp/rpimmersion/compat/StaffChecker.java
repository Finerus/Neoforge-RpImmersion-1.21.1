package net.rp.rpimmersion.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Wrapper autour de RpEssentialsPermissions.isStaff().
 *
 * En DEV : RpEssentials absent → tous les joueurs sont staff (false en prod).
 * En PROD : le jar RpEssentials est dans mods/ → la vraie vérification s'exécute.
 *
 * Ceci évite de devoir ajouter le jar en compileOnly dans le build.gradle.
 */
public class StaffChecker {

    private static final boolean RPE_AVAILABLE;

    static {
        boolean found = false;
        try {
            Class.forName("net.rp.rpessentials.RpEssentialsPermissions");
            found = true;
        } catch (ClassNotFoundException ignored) {}
        RPE_AVAILABLE = found;
    }

    public static boolean isStaff(ServerPlayer player) {
        if (player == null) return false;
        if (RPE_AVAILABLE) {
            try {
                return net.rp.rpessentials.RpEssentialsPermissions.isStaff(player);
            } catch (Exception | LinkageError e) {
                return player.hasPermissions(2);
            }
        }
        return player.hasPermissions(2);
    }

    public static boolean isStaffSource(CommandSourceStack src) {
        if (src.getEntity() instanceof ServerPlayer player) return isStaff(player);
        return src.hasPermission(2);
    }
}
