package net.rp.rpimmersion.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.network.CalendarSyncPacket;

@EventBusSubscriber(modid = RpImmersion.MODID, value = Dist.CLIENT)
public class CalendarHudCache {

    private static String  displayString = "";
    private static String  displayMode   = "NONE";
    private static String  hudPosition   = "TOP_LEFT";
    private static float   hudScale      = 1.0f;
    private static boolean hudBackground = true;
    private static boolean received      = false;
    private static boolean sending       = false;
    private static int     actionBarCooldown = 0;
    private static long    foreignOverlayUntil = 0;

    public static void update(CalendarSyncPacket packet) {
        displayString = packet.displayString();
        displayMode   = packet.displayMode();
        hudPosition   = packet.hudPosition();
        hudScale      = packet.hudScale();
        hudBackground = packet.hudBackground();
        received      = true;
        if (actionBarEnabled()) showActionBar();
    }

    public static void reset() {
        displayString = "";
        displayMode   = "NONE";
        received      = false;
        actionBarCooldown = 0;
        foreignOverlayUntil = 0;
    }

    private static boolean actionBarEnabled() {
        return received && !displayString.isEmpty()
                && (displayMode.equals("ACTION_BAR") || displayMode.equals("BOTH"));
    }

    private static boolean hudEnabled() {
        return received && !displayString.isEmpty()
                && (displayMode.equals("HUD") || displayMode.equals("BOTH"));
    }

    private static void showActionBar() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.level.getGameTime() < foreignOverlayUntil) return;
        sending = true;
        try {
            mc.player.displayClientMessage(Component.literal(displayString), true);
        } finally {
            sending = false;
        }
    }

    @SubscribeEvent
    public static void onSystemChat(ClientChatReceivedEvent.System event) {
        if (!event.isOverlay() || sending) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) foreignOverlayUntil = mc.level.getGameTime() + 60;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!hudEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        GuiGraphics g = event.getGuiGraphics();
        int textW = mc.font.width(displayString);
        int textH = mc.font.lineHeight;
        int margin = 4;
        int boxW = Math.round((textW + 4) * hudScale);
        int boxH = Math.round((textH + 4) * hudScale);

        int x = switch (hudPosition) {
            case "TOP_RIGHT", "BOTTOM_RIGHT" -> g.guiWidth() - boxW - margin;
            case "TOP_CENTER" -> (g.guiWidth() - boxW) / 2;
            default -> margin;
        };
        int y = hudPosition.startsWith("BOTTOM") ? g.guiHeight() - boxH - margin : margin;

        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(hudScale, hudScale, 1f);
        if (hudBackground) g.fill(0, 0, textW + 4, textH + 4, 0x88000000);
        g.drawString(mc.font, displayString, 2, 2, 0xFFFFFF, true);
        g.pose().popPose();
    }

    public static void clientTick() {
        if (!actionBarEnabled()) return;
        if (--actionBarCooldown > 0) return;
        actionBarCooldown = 20;
        showActionBar();
    }

    public static String getDisplayString() { return displayString; }
}