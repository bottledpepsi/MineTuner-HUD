package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Cumulative JVM garbage collection time in ms, summed across all GC beans. */
public final class GcTimeStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.GC_TIME;
    }

    @Override
    public String token() {
        return "gc";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedGcTime();
    }

    @Override
    public String rawValue(int decimals) {
        return MineTunerDataHolder.getRawGcTime();
    }
}
