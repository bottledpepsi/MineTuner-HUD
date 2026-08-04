package bottled.mtss.sample.sources;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.sample.Cadence;
import bottled.mtss.sample.SamplingContext;
import bottled.mtss.sample.StatSource;

/**
 * Heap memory (used/max) plus the once-per-frame ring-buffer history push
 * for every graphable stat (TPS/MSPT/FPS/CPU/Ping/Memory/Speed).
 * <p>
 * This wraps {@link MtssDataHolder#updateFastMetrics()} as-is rather than
 * splitting it into per-value sources: the history push reads several
 * already-sampled fields (fps, mspt, cpuPercent, ping, speedBps) together
 * in one pass, so it must run exactly once per frame, after those fields
 * are set by the other PER_FRAME sources. Registration order in
 * SourceRegistry keeps this last for that reason. The read/push logic
 * itself is left untouched for this pass — see design doc §3.7/§3.8 step 4;
 * migrating it onto per-source history() hooks is the follow-up in §5.
 */
public final class FastMetricsSource implements StatSource {
    @Override public String id() { return "fast_metrics"; }
    @Override public Cadence cadence() { return Cadence.PER_FRAME; }

    @Override public void sample(SamplingContext ctx) {
        MtssDataHolder.updateFastMetrics();
    }
}
