package bottled.mtss.sample.sources;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.sample.Cadence;
import bottled.mtss.sample.SamplingContext;
import bottled.mtss.sample.StatSource;

/**
 * Server-side milliseconds-per-tick, only observable on a hosted
 * singleplayer/LAN server. {@code -1} on remote servers (unavailable).
 */
public final class SingleplayerMsptSource implements StatSource {
    @Override public String id() { return "singleplayer_mspt"; }
    @Override public Cadence cadence() { return Cadence.PER_FRAME; }

    @Override public void sample(SamplingContext ctx) {
        if (ctx.mc().hasSingleplayerServer() && ctx.mc().getSingleplayerServer() != null) {
            MtssDataHolder.mspt =
                    ctx.mc().getSingleplayerServer().getAverageTickTimeNanos() / 1_000_000.0f;
        } else {
            MtssDataHolder.mspt = -1f; // unavailable on remote servers
        }
    }
}
