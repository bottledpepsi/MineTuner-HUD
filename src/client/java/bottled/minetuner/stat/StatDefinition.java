package bottled.minetuner.stat;

import bottled.minetuner.config.MineTunerConfig;

/** Everything MineTuner needs to know about one stat, in one place. */
public interface StatDefinition {

    /** The persisted identity, e.g. {@code MineTunerConfig.Stat.TPS}. Used as the config's
     *  storage key and as the switch target wherever a stat still needs special-casing. */
    MineTunerConfig.Stat key();

    /** Lowercase token name for Template Mode, e.g. {@code "tps"} for TPS — the text
     *  a user types inside {@code {tps}} in a template line. */
    String token();

    /** The rendered line for classic mode / template mode, e.g. {@code "TPS: 20.0"}
     *  (label included; see {@link #rawValue(int)} for the label-free form). */
    String format(int decimals);

    /** The bare value with no label and no unit suffix, e.g. {@code "18"} rather than
     *  {@code "Air: 18"}. Defaults to {@link #format(int)} unmodified — note that for
     *  most stats format() itself already includes a label (e.g. TPS's format() is
     *  "TPS: 20.0"), so most stats need a real override here (typically delegating to
     *  a dedicated label-free getter) to produce a genuinely bare value; only stats
     *  whose format() has no label to begin with can safely rely on this default. */
    default String rawValue(int decimals) {
        return format(decimals);
    }

    /** Default decimal count used when no per-stat/per-token override is given. */
    default int defaultDecimals() {
        return 1;
    }

    /** Whether {@link #format(int)}'s decimals argument does anything for this stat.
     *  False for naturally-integer stats (entity count, chunk count, ping in ms). */
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

    /** True if a higher value is "good" (TPS, FPS, health); false if a lower value is
     *  "good" instead (ping, CPU load, GPU temperature). Only meaningful for stats
     *  where {@link #supportsThreshold()} is true. */
    default boolean higherIsBetter() {
        return true;
    }

    /** Increment step for the threshold editor's -/+ steppers, e.g. {@code 0.5f} for
     *  TPS (fine-grained around the 20.0 target) vs. {@code 1.0f} for a whole-number
     *  stat like GPU temperature in °C. */
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

    /** ARGB color for the current live value, honoring an optional per-list custom
     *  threshold ({@code custom}, or the stat's own built-in default when null/disabled). */
    default int color(MineTunerConfig.ThresholdSettings custom) {
        return 0xFFFFFFFF;
    }

    /** ARGB color for a specific historical value (used by per-segment graph coloring,
     *  where each point on the line is colored by its own recorded value rather than
     *  the graph as a whole being tinted by the current value). Defaults to just
     *  calling {@link #color}, ignoring the historical value, for stats that don't
     *  override this. */
    default int colorFor(float value, MineTunerConfig.ThresholdSettings custom) {
        return color(custom);
    }

    /** How the axis/min-max labels on a graph should format a raw value, e.g.
     *  {@code "20.0"} for TPS (one decimal) vs. the default integer rounding
     *  ({@code Math.round}) used by stats that don't override this, like entity count. */
    default String formatAxisValue(float value) {
        return Integer.toString(Math.round(value));
    }
}
