package net.rp.rpimmersion;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Wildcard pattern matching pour les biomes et autres resource locations.
 * Supporte * comme joker (ex: minecraft:*_forest, *:swamp).
 */
public class PatternUtils {

    private static final Map<String, Pattern> cache = new ConcurrentHashMap<>();

    public static boolean matches(String resourceId, String pattern) {
        pattern = pattern.trim();
        if (resourceId.equals(pattern)) return true;
        if (!pattern.contains("*")) return false;

        Pattern compiled = cache.computeIfAbsent(pattern, p ->
                Pattern.compile(p.replace(".", "\\.").replace("*", ".*"))
        );
        return compiled.matcher(resourceId).matches();
    }

    public static void clearCache() { cache.clear(); }
}
