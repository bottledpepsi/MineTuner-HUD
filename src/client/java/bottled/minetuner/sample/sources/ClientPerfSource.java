package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;

import java.util.Set;

/** Client-reported frames per second. */
public final class ClientPerfSource implements StatSource {
    @Override
    public String id() {
        return "client_perf";
    }

    @Override
    public Set<MineTunerConfig.Stat> feeds() {
        return Set.of(MineTunerConfig.Stat.FPS);
    }

    @Override
    public Cadence cadence() {
        return Cadence.PER_TICK;
    }

    @Override
    public void sample(SamplingContext ctx) {
        MineTunerDataHolder.fps = ctx.mc().getFps();
    }
}
