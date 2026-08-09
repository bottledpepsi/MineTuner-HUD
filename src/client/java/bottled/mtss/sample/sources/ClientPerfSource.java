package bottled.mtss.sample.sources;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.sample.Cadence;
import bottled.mtss.sample.SamplingContext;
import bottled.mtss.sample.StatSource;

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
        MtssDataHolder.fps = ctx.mc().getFps();
    }
}
