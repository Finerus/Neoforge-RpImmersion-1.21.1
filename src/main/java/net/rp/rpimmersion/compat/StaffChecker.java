package net.rp.rpimmersion.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Staff check through the public RpEssentials API ({@code RpEssentialsAPI.isStaff}: roles with isStaff,
 * or the opLevelBypass OP level). The API package does not move between RpEssentials versions.
 * Falls back to OP level 2 if RpEssentials cannot be reached.
 */
public class StaffChecker {

    private static final boolean RPE_AVAILABLE;

    static {
        boolean found = false;
        try {
            Class.forName("net.rp.rpessentials.api.RpEssentialsAPI");
            found = true;
        } catch (ClassNotFoundException ignored) {}
        RPE_AVAILABLE = found;
    }

    public static boolean isStaff(ServerPlayer player) {
        if (player == null) return false;
        if (RPE_AVAILABLE) {
            try {
                return net.rp.rpessentials.api.RpEssentialsAPI.isStaff(player);
            } catch (Exception | LinkageError e) {
                return player.hasPermissions(2);
            }
        }
        return player.hasPermissions(2);
    }

    /** Players: {@link #isStaff}. Console and command blocks: OP level 2. */
    public static boolean isStaffSource(CommandSourceStack src) {
        if (src.getEntity() instanceof ServerPlayer player) return isStaff(player);
        return src.hasPermission(2);
    }
}
