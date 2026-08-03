package bottled.mtss.stat;

import bottled.mtss.config.MtssConfig;

/**
 * Everything MTSS needs to know about one stat, in one place. This is the
 * seam that makes adding a stat "write one class, register it" instead of
 * touching a switch statement in every file that cares about stats.
 * <p>
 * Implementations live in {@code bottled.mtss.stat.stats} and are wired up
 * once in {@link StatRegistry}. Nothing outside this package should need to
 * {@code switch (stat) { ... }} on {@link MtssConfig.Stat} again — go through
 * {@link StatRegistry#get(MtssConfig.Stat)} instead.
 * <p>
 * Every method has a sensible default so a minimal stat (no decimals, no
 * graph, no threshold coloring) only needs to implement {@link #key()},
 * {@link #token()}, and {@link #format(int)}.
 */
public interface StatDefinition {

    /** The persisted identity — must match a {@link MtssConfig.Stat} constant. Never changes once shipped, since it's saved in every user's config. */
    MtssConfig.Stat key();

    /** Lowercase token name for Template Mode, e.g. {@code "tps"} for {tps}. Keep in sync with the README's token table. */
    String token();

    /** The rendered line for classic mode / template mode, e.g. "TPS: 19.8". Empty string means "skip this line" (e.g. MSPT on a remote server). */
    String format(int decimals);

    /** Default decimal count used when no per-stat/per-token override is given. Ignored unless {@link #supportsDecimals()} is true. */
    default int defaultDecimals() { return 1; }

    /** Whether {@link #format(int)}'s decimals argument does anything for this stat. */
    default boolean supportsDecimals() { return false; }

    /** Whether this stat can render as a rolling history graph instead of text. */
    default boolean supportsGraph() { return false; }

    /** Snapshot of this stat's recorded history, oldest-to-newest. Only called when {@link #supportsGraph()} is true. */
    default float[] history() { return new float[0]; }

    /** Whether this stat has a user-configurable good/warn color threshold. */
    default boolean supportsThreshold() { return false; }

    /** True if a higher value is "good" (TPS, FPS); false if a lower value is "good" (Ping, Memory, CPU). Only meaningful when {@link #supportsThreshold()} is true. */
    default boolean higherIsBetter() { return true; }

    /** Increment step for the threshold editor's -/+ steppers, e.g. 0.5 for TPS, 1.0 for whole-unit stats. */
    default float thresholdStep() { return 1.0f; }

    /** Built-in "good" cutoff used when the list has no custom threshold enabled. */
    default float defaultGoodMin() { return 0f; }

    /** Built-in "warn" cutoff used when the list has no custom threshold enabled. */
    default float defaultWarnMin() { return 0f; }

    /** ARGB color for the current live value, honoring an optional per-list threshold override. Pass null for the built-in default thresholds. */
    default int color(MtssConfig.ThresholdSettings custom) { return 0xFFFFFFFF; }

    /** ARGB color for a specific historical value (used by per-segment graph coloring), honoring the same threshold rules as {@link #color}. */
    default int colorFor(float value, MtssConfig.ThresholdSettings custom) { return color(custom); }

    /** How the axis/min-max labels on a graph should format a raw value, e.g. "%" suffix for Memory. */
    default String formatAxisValue(float value) { return Integer.toString(Math.round(value)); }
}
