package net.rp.rpimmersion;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PatternUtils {

    private static final Map<String, Pattern> cache = new ConcurrentHashMap<>();

    public static boolean matches(String id, String pattern) {
        pattern = pattern.trim();
        if (id.equals(pattern)) return true;
        if (!pattern.contains("*")) return false;
        Pattern compiled = cache.computeIfAbsent(pattern, p -> Pattern.compile(
                Arrays.stream(p.split("\\*", -1))
                        .map(Pattern::quote)
                        .collect(Collectors.joining(".*"))));
        return compiled.matcher(id).matches();
    }

    public static void clearCache() { cache.clear(); }
}
