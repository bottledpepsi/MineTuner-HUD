package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/**
 * Remaining breath/air supply. Empty string (skipped line) whenever the
 * player is at full air, same "nothing worth showing" convention MSPT uses
 * on remote servers — the value is meaningless outside water, so hiding it
 * most of the time keeps a list clean instead of permanently showing a
 * static max value. Its own fixed white/yellow/red coloring isn't a
 * ThresholdSettings good/warn band (see {@code MtssDataHolder.airColorFor}),
 * same pattern as Speed's gray/yellow/white.
 */
public final class AirStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.AIR; }
    @Override public String token() { return "air"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedAir(); }
    @Override public String rawValue(int decimals) { return MtssDataHolder.getRawAir(); }

    @Override public int color(MtssConfig.ThresholdSettings custom) { return MtssDataHolder.getAirColor(); }
    @Override public int colorFor(float value, MtssConfig.ThresholdSettings custom) {
        return MtssDataHolder.airColorFor(value, MtssDataHolder.maxAir);
    }
}
