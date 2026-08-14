package bottled.minetuner.config;

import java.util.LinkedHashMap;
import java.util.Map;


public class ListTheme {

    /** Display name shown in the Theme panel and Cloth Config selector. */
    public String name;
    /** True for the small set of themes shipped by MineTuner itself — built-in
     *  themes cannot be deleted, renamed, or overwritten by "re-save." */
    public boolean builtin = false;

    // --- Appearance fields (see class doc for what is/isn't included). ---
    public boolean showBackground = true;
    public boolean textShadow = false;
    public boolean useCustomColor = false;
    public int overrideColor = 0xFFFFFFFF;
    public int overrideFillColor = 0xB8141820;
    public int overrideOutlineColor = 0x5E9BA9BE;
    public float textScale = 1.0f;
    /** Horizontal and vertical content padding, in GUI pixels at 1x scale. */
    public int paddingX = 6;
    public int paddingY = 5;


    public Map<String, MineTunerConfig.GraphStyle> graphStyles = new LinkedHashMap<>();

    public ListTheme() {
    }

    public ListTheme(String name) {
        this.name = name;
    }

    /** Deep-copies this theme into a brand-new instance, nested mutable maps
     *  included, so editing the copy can never affect the original. Matches
     *  {@link MineTunerConfig.StatListConfig#duplicate(int)}'s copy pattern. */
    public ListTheme duplicate() {
        ListTheme copy = new ListTheme(name);
        copy.builtin = builtin;
        copy.showBackground = showBackground;
        copy.textShadow = textShadow;
        copy.useCustomColor = useCustomColor;
        copy.overrideColor = overrideColor;
        copy.overrideFillColor = overrideFillColor;
        copy.overrideOutlineColor = overrideOutlineColor;
        copy.textScale = textScale;
        copy.paddingX = paddingX;
        copy.paddingY = paddingY;
        copy.graphStyles = new LinkedHashMap<>();
        graphStyles.forEach((key, src) -> copy.graphStyles.put(key, src.copy()));
        return copy;
    }

    /** Builds a new theme named {@code themeName} by capturing {@code lc}'s current
     *  appearance. The returned theme is never marked {@link #builtin}. */
    public static ListTheme captureFrom(String themeName, MineTunerConfig.StatListConfig lc) {
        ListTheme t = new ListTheme(themeName);
        t.builtin = false;
        t.showBackground = lc.showBackground;
        t.textShadow = lc.textShadow;
        t.useCustomColor = lc.useCustomColor;
        t.overrideColor = lc.overrideColor;
        t.overrideFillColor =lc.overrideFillColor;
        t.overrideOutlineColor =lc.overrideOutlineColor;
        t.textScale = lc.textScale;
        t.paddingX = lc.paddingX;
        t.paddingY = lc.paddingY;
        t.graphStyles = new LinkedHashMap<>();
        lc.statSettings.forEach((key, settings) -> {
            MineTunerConfig.GraphStyle style = settings.graphStyle;
            t.graphStyles.put(key, (style != null ? style : new MineTunerConfig.GraphStyle()).copy());
        });
        return t;
    }

    /** Re-captures this EXISTING theme's fields from {@code lc}'s current appearance
     *  in place (used by the Theme panel's "update"/re-save action). No-op guarded
     *  against built-ins by the caller — see {@link MineTunerConfig#saveTheme}. */
    public void recaptureFrom(MineTunerConfig.StatListConfig lc) {
        showBackground = lc.showBackground;
        textShadow = lc.textShadow;
        useCustomColor = lc.useCustomColor;
        overrideColor = lc.overrideColor;
        overrideFillColor = lc.overrideFillColor;
        overrideOutlineColor = lc.overrideOutlineColor;
        textScale = lc.textScale;
        paddingX = lc.paddingX;
        paddingY = lc.paddingY;
        graphStyles = new LinkedHashMap<>();
        lc.statSettings.forEach((key, settings) -> {
            MineTunerConfig.GraphStyle style = settings.graphStyle;
            graphStyles.put(key, (style != null ? style : new MineTunerConfig.GraphStyle()).copy());
        });
    }

    /** Overwrites only {@code lc}'s appearance fields with this theme's, leaving
     *  stats/order/name/position completely untouched. Any stat this theme has no
     *  captured GraphStyle for (e.g. a stat added to MineTuner after this theme was
     *  saved) simply keeps its current GraphStyle. */
    public void applyTo(MineTunerConfig.StatListConfig lc) {
        lc.showBackground = showBackground;
        lc.textShadow = textShadow;
        lc.useCustomColor = useCustomColor;
        lc.overrideColor = overrideColor;
        lc.overrideFillColor = overrideFillColor;
        lc.overrideOutlineColor = overrideOutlineColor;
        lc.textScale = textScale;
        lc.paddingX = paddingX;
        lc.paddingY = paddingY;
        graphStyles.forEach((key, style) -> {
            MineTunerConfig.StatSettings settings = lc.statSettings.computeIfAbsent(
                    key, k -> new MineTunerConfig.StatSettings());
            settings.graphStyle = style.copy();
        });
    }
}
