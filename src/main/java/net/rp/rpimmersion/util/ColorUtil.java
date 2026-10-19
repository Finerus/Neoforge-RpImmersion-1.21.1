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
        text = text.replaceAll("&(?=[0-9a-fk-orA-FK-OR])", "§");
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
        return switch (Character.toLowerCase(c)) {
            case '0' -> Style.EMPTY.withColor(ChatFormatting.BLACK);
            case '1' -> Style.EMPTY.withColor(ChatFormatting.DARK_BLUE);
            case '2' -> Style.EMPTY.withColor(ChatFormatting.DARK_GREEN);
            case '3' -> Style.EMPTY.withColor(ChatFormatting.DARK_AQUA);
            case '4' -> Style.EMPTY.withColor(ChatFormatting.DARK_RED);
            case '5' -> Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE);
            case '6' -> Style.EMPTY.withColor(ChatFormatting.GOLD);
            case '7' -> Style.EMPTY.withColor(ChatFormatting.GRAY);
            case '8' -> Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
            case '9' -> Style.EMPTY.withColor(ChatFormatting.BLUE);
            case 'a' -> Style.EMPTY.withColor(ChatFormatting.GREEN);
            case 'b' -> Style.EMPTY.withColor(ChatFormatting.AQUA);
            case 'c' -> Style.EMPTY.withColor(ChatFormatting.RED);
            case 'd' -> Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE);
            case 'e' -> Style.EMPTY.withColor(ChatFormatting.YELLOW);
            case 'f' -> Style.EMPTY.withColor(ChatFormatting.WHITE);
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
