package bottled.mtss.stat;

import bottled.mtss.config.MtssConfig;

/** Everything MTSS needs to know about one stat, in one place. */
public interface StatDefinition {

    /** The persisted identity. */
    MtssConfig.Stat key();

    /** Lowercase token name for Template Mode, e.g. */
    String token();

    /** The rendered line for classic mode / template mode, e.g. */
    String format(int decimals);

    /** The bare value with no label and no unit suffix, e.g. */
    default String rawValue(int decimals) {
        return format(decimals);
    }

    /** Default decimal count used when no per-stat/per-token override is given. */
    default int defaultDecimals() {
        return 1;
    }

    /** Whether { #format(int)}'s decimals argument does anything for this stat. */
    default boolean supportsDecimals() {
        return false;
    }

    /** Whether this stat can render as a rolling history graph instead of text. */
    default boolean supportsGraph() {
        return false;
    }

    /** Snapshot of this stat's recorded history, oldest-to-newest. */
    default float[] history() {
        return new float[0];
    }

    /** Whether this stat has a user-configurable good/warn color threshold. */
    default boolean supportsThreshold() {
        return false;
    }

    /** True if a higher value is "good" (TPS, FPS). */
    default boolean higherIsBetter() {
        return true;
    }

    /** Increment step for the threshold editor's -/+ steppers, e.g. */
    default float thresholdStep() {
        return 1.0f;
    }

    /** Built-in "good" cutoff used when the list has no custom threshold enabled. */
    default float defaultGoodMin() {
        return 0f;
    }

    /** Built-in "warn" cutoff used when the list has no custom threshold enabled. */
    default float defaultWarnMin() {
        return 0f;
    }

    /** ARGB color for the current live value, honoring an optional per-list. */
    default int color(MtssConfig.ThresholdSettings custom) {
        return 0xFFFFFFFF;
    }

    /** ARGB color for a specific historical value (used by per-segment graph. */
    default int colorFor(float value, MtssConfig.ThresholdSettings custom) {
        return color(custom);
    }

    /** How the axis/min-max labels on a graph should format a raw value, e.g. */
    default String formatAxisValue(float value) {
        return Integer.toString(Math.round(value));
    }
}
