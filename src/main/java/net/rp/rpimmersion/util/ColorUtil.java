package net.rp.rpimmersion.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitaire de parsing des codes couleur Minecraft (§ et &).
 * Même logique que ColorHelper dans RpEssentials, dupliqué ici pour autonomie.
 */
public class ColorUtil {

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        text = text.replace("&", "§");
        if (!text.contains("§")) return Component.literal(text);

        MutableComponent result = Component.empty();
        List<Segment> segments  = parseSegments(text);
        for (Segment seg : segments)
            result.append(Component.literal(seg.text).setStyle(seg.style));
        return result;
    }

    private static List<Segment> parseSegments(String text) {
        List<Segment> segments = new ArrayList<>();
        StringBuilder cur      = new StringBuilder();
        Style         style    = Style.EMPTY;

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '§' && i + 1 < text.length()) {
                if (!cur.isEmpty()) {
                    segments.add(new Segment(cur.toString(), style));
                    cur = new StringBuilder();
                }
                style = applyCode(style, text.charAt(i + 1));
                i++;
            } else {
                cur.append(text.charAt(i));
            }
        }
        if (!cur.isEmpty()) segments.add(new Segment(cur.toString(), style));
        return segments;
    }

    private static Style applyCode(Style s, char c) {
        return switch (c) {
            case '0' -> s.withColor(ChatFormatting.BLACK);
            case '1' -> s.withColor(ChatFormatting.DARK_BLUE);
            case '2' -> s.withColor(ChatFormatting.DARK_GREEN);
            case '3' -> s.withColor(ChatFormatting.DARK_AQUA);
            case '4' -> s.withColor(ChatFormatting.DARK_RED);
            case '5' -> s.withColor(ChatFormatting.DARK_PURPLE);
            case '6' -> s.withColor(ChatFormatting.GOLD);
            case '7' -> s.withColor(ChatFormatting.GRAY);
            case '8' -> s.withColor(ChatFormatting.DARK_GRAY);
            case '9' -> s.withColor(ChatFormatting.BLUE);
            case 'a' -> s.withColor(ChatFormatting.GREEN);
            case 'b' -> s.withColor(ChatFormatting.AQUA);
            case 'c' -> s.withColor(ChatFormatting.RED);
            case 'd' -> s.withColor(ChatFormatting.LIGHT_PURPLE);
            case 'e' -> s.withColor(ChatFormatting.YELLOW);
            case 'f' -> s.withColor(ChatFormatting.WHITE);
            case 'l' -> s.withBold(true);
            case 'o' -> s.withItalic(true);
            case 'n' -> s.withUnderlined(true);
            case 'm' -> s.withStrikethrough(true);
            case 'k' -> s.withObfuscated(true);
            case 'r' -> Style.EMPTY;
            default  -> s;
        };
    }

    private record Segment(String text, Style style) {}
}
