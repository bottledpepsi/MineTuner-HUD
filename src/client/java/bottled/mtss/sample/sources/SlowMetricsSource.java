package bottled.mtss.sample.sources;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.sample.Cadence;
import bottled.mtss.sample.SamplingContext;
import bottled.mtss.sample.StatSource;

/**
 * Throttled CPU load and cumulative GC time. Wraps
 * {@link MtssDataHolder#updateSlowMetrics()} as-is: that method already
 * self-throttles to 500ms internally (via {@code lastSlowUpdateMs}), so
 * this source is safe to register at {@link Cadence#THROTTLED} even though
 * the timing check is currently duplicated in two places (the driver's own
 * 500ms gate, and this method's internal one). Left untouched for this
 * pass — see design doc §3.7/§3.8 step 4.
 */
public final class SlowMetricsSource implements StatSource {
    @Override public String id() { return "slow_metrics"; }
    @Override public Cadence cadence() { return Cadence.THROTTLED; }

    @Override public void sample(SamplingContext ctx) {
        MtssDataHolder.updateSlowMetrics();
    }
}
