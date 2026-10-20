package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;

import java.util.Set;

/** Throttled CPU load and cumulative GC time. */
public final class SlowMetricsSource implements StatSource {
    @Override
    public String id() {
        return "slow_metrics";
    }

    @Override
    public Set<MineTunerConfig.Stat> feeds() {
        return Set.of(MineTunerConfig.Stat.CPU, MineTunerConfig.Stat.GC_TIME);
    }

    @Override
    public Cadence cadence() {
        return Cadence.THROTTLED;
    }

    @Override
    public void sample(SamplingContext ctx) {
        MineTunerDataHolder.updateSlowMetrics();
    }
}
