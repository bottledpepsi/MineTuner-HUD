package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;

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
        MineTunerDataHolder.updateFastMetrics();
    }
}
