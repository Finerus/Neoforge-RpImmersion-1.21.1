package net.rp.rpimmersion.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.config.CalendarConfig;
import net.rp.rpimmersion.network.CalendarSyncPacket;

/**
 * Cache et rendu HUD du calendrier côté client.
 * Reçoit les données via CalendarSyncPacket.
 * Affiche selon le mode configuré (HUD, ACTION_BAR, BOTH).
 */
@EventBusSubscriber(modid = RpImmersion.MODID, value = Dist.CLIENT)
public class CalendarHudCache {

    private static String  displayString = "";
    private static boolean received      = false;
    private static int     actionBarCooldown = 0;

    // =========================================================================
    // MISE À JOUR depuis packet
    // =========================================================================

    public static void update(CalendarSyncPacket packet) {
        displayString = packet.displayString();
        received      = true;
        // Si mode ACTION_BAR ou BOTH : envoyer immédiatement en action bar
        sendToActionBarIfNeeded();
    }

    public static void reset() {
        displayString = "";
        received      = false;
        actionBarCooldown = 0;
    }

    // =========================================================================
    // ACTION BAR (rendu via tick client)
    // =========================================================================

    private static void sendToActionBarIfNeeded() {
        if (!received || displayString.isEmpty()) return;
        try {
            String mode = CalendarConfig.DISPLAY_MODE.get().toUpperCase();
            if (mode.equals("ACTION_BAR") || mode.equals("BOTH")) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(displayString), true);
                }
            }
        } catch (IllegalStateException ignored) {}
    }

    // =========================================================================
    // HUD RENDERING
    // =========================================================================

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!received || displayString.isEmpty()) return;

        try {
            String mode = CalendarConfig.DISPLAY_MODE.get().toUpperCase();
            if (!mode.equals("HUD") && !mode.equals("BOTH")) return;
        } catch (IllegalStateException e) { return; }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int x = 4;
        int y = 4;

        // Fond semi-transparent
        int textWidth = mc.font.width(net.minecraft.network.chat.Component.literal(displayString));
        graphics.fill(x - 2, y - 2, x + textWidth + 2, y + mc.font.lineHeight + 2, 0x88000000);
        graphics.drawString(mc.font, displayString, x, y, 0xFFFFFF, true);
    }

    // =========================================================================
    // REFRESH périodique de l'action bar (toutes les 20 ticks pour qu'elle reste visible)
    // =========================================================================

    public static void clientTick() {
        if (!received || displayString.isEmpty()) return;
        try {
            String mode = CalendarConfig.DISPLAY_MODE.get().toUpperCase();
            if (!mode.equals("ACTION_BAR") && !mode.equals("BOTH")) return;
        } catch (IllegalStateException e) { return; }

        if (--actionBarCooldown > 0) return;
        actionBarCooldown = 20; // Rafraîchir toutes les 20 ticks

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null)
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(displayString), true);
    }

    public static String getDisplayString() { return displayString; }
}
