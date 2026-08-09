package bottled.mtss.sample.sources;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.sample.Cadence;
import bottled.mtss.sample.SamplingContext;
import bottled.mtss.sample.StatSource;

/** Heap memory (used/max) plus the once-per-frame ring-buffer history push for. */
public final class FastMetricsSource implements StatSource {
    @Override
    public String id() {
        return "fast_metrics";
    }

    @Override
    public Cadence cadence() {
        return Cadence.PER_FRAME;
    }

    @Override
    public void sample(SamplingContext ctx) {
        MtssDataHolder.updateFastMetrics();
    }
}
