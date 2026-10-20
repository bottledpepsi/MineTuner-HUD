package bottled.minetuner.core;

import java.util.Locale;

/**
 * Builds translation keys from enum constants. Always uses {@link Locale#ROOT}: under
 * some default locales (e.g. Turkish) {@code "PING".toLowerCase()} is not {@code "ping"}.
 */
public final class LangKeys {

    private LangKeys() {
    }

    /**
     * {@code stat.minetuner.<name>} for a Stat constant.
     */
    public static String stat(Enum<?> stat) {
        return "stat.minetuner." + lower(stat);
    }

    /**
     * {@code gui.minetuner.category.<name>}.
     */
    public static String category(Enum<?> category) {
        return enumValue("gui.minetuner.category.", category);
    }

    /**
     * {@code prefix + <lower-cased constant name>}; the prefix must include its trailing dot.
     */
    public static String enumValue(String prefix, Enum<?> value) {
        return prefix + lower(value);
    }

    private static String lower(Enum<?> e) {
        return e.name().toLowerCase(Locale.ROOT);
    }
}
