package net.rp.rpimmersion;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class PatternUtils {

    private static final Map<String, Pattern> cache = new ConcurrentHashMap<>();

    public static boolean matches(String id, String pattern) {
        pattern = pattern.trim();
        if (id.equals(pattern)) return true;
        if (!pattern.contains("*")) return false;
        Pattern compiled = cache.computeIfAbsent(pattern,
                p -> Pattern.compile(p.replace(".", "\\.").replace("*", ".*")));
        return compiled.matcher(id).matches();
    }

    public static void clearCache() { cache.clear(); }
}
