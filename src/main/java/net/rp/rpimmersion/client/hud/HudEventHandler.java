package net.rp.rpimmersion.client.hud;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rp.rpimmersion.RpImmersion;
import net.rp.rpimmersion.client.ClientHudState;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = RpImmersion.MODID, value = Dist.CLIENT)
public class HudEventHandler {

    private static final Map<ResourceLocation, String> LAYERS = new HashMap<>();

    static {
        LAYERS.put(VanillaGuiLayers.CROSSHAIR, "crosshair");
        LAYERS.put(VanillaGuiLayers.HOTBAR, "hotbar");
        LAYERS.put(VanillaGuiLayers.PLAYER_HEALTH, "health");
        LAYERS.put(VanillaGuiLayers.ARMOR_LEVEL, "armor");
        LAYERS.put(VanillaGuiLayers.FOOD_LEVEL, "food");
        LAYERS.put(VanillaGuiLayers.AIR_LEVEL, "air");
        LAYERS.put(VanillaGuiLayers.EXPERIENCE_BAR, "experience");
        LAYERS.put(VanillaGuiLayers.EXPERIENCE_LEVEL, "experience");
        LAYERS.put(VanillaGuiLayers.JUMP_METER, "jump_bar");
        LAYERS.put(VanillaGuiLayers.VEHICLE_HEALTH, "vehicle_health");
        LAYERS.put(VanillaGuiLayers.SELECTED_ITEM_NAME, "item_name");
        LAYERS.put(VanillaGuiLayers.EFFECTS, "effects");
        LAYERS.put(VanillaGuiLayers.BOSS_OVERLAY, "boss_bars");
        LAYERS.put(VanillaGuiLayers.SCOREBOARD_SIDEBAR, "scoreboard");
        LAYERS.put(VanillaGuiLayers.TAB_LIST, "tab_list");
        LAYERS.put(VanillaGuiLayers.DEBUG_OVERLAY, "debug");
        LAYERS.put(VanillaGuiLayers.SUBTITLE_OVERLAY, "subtitles");
    }

    @SubscribeEvent
    public static void onLayer(RenderGuiLayerEvent.Pre event) {
        String element = LAYERS.get(event.getName());
        if (element != null && ClientHudState.isHidden(element)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ClientHudState.isForceFirstPerson()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }
}
