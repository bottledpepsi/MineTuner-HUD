package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;

import java.util.Set;

/** Server-side milliseconds-per-tick, only observable on an integrated
 *  (singleplayer/LAN-hosted) server. */
public final class SingleplayerMsptSource implements StatSource {
    @Override
    public String id() {
        return "singleplayer_mspt";
    }

    @Override
    public Set<MineTunerConfig.Stat> feeds() {
        return Set.of(MineTunerConfig.Stat.MSPT, MineTunerConfig.Stat.TPS);
    }

    @Override
    public Cadence cadence() {
        return Cadence.PER_TICK;
    }

    @Override
    public void sample(SamplingContext ctx) {
        if (ctx.mc().hasSingleplayerServer() && ctx.mc().getSingleplayerServer() != null) {
            MineTunerDataHolder.mspt =
                    ctx.mc().getSingleplayerServer().getAverageTickTimeNanos() / 1_000_000.0f;
        } else {
            MineTunerDataHolder.mspt = -1f; // unavailable on remote servers.
        }
    }
}
