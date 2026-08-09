package bottled.mtss.sample.sources;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.sample.Cadence;
import bottled.mtss.sample.SamplingContext;
import bottled.mtss.sample.StatSource;

/** Throttled CPU load and cumulative GC time. */
public final class SlowMetricsSource implements StatSource {
    @Override
    public String id() {
        return "slow_metrics";
    }

    @Override
    public Cadence cadence() {
        return Cadence.THROTTLED;
    }

    @Override
    public void sample(SamplingContext ctx) {
        MtssDataHolder.updateSlowMetrics();
    }
}
