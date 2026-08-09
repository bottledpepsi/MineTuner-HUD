package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Cumulative JVM garbage collection time in ms, summed across all GC beans. */
public final class GcTimeStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.GC_TIME;
    }

    @Override
    public String token() {
        return "gc";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedGcTime();
    }

    @Override
    public String rawValue(int decimals) {
        return MtssDataHolder.getRawGcTime();
    }
}
