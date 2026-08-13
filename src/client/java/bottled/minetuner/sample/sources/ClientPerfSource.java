package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;

/** Client-reported frames per second. */
public final class ClientPerfSource implements StatSource {
    @Override
    public String id() {
        return "client_perf";
    }

    @Override
    public Cadence cadence() {
        return Cadence.PER_FRAME;
    }

    @Override
    public void sample(SamplingContext ctx) {
        MineTunerDataHolder.fps = ctx.mc().getFps();
    }
}
