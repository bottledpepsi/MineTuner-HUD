package bottled.mtss.sample.sources;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.sample.Cadence;
import bottled.mtss.sample.SamplingContext;
import bottled.mtss.sample.StatSource;
import net.minecraft.client.multiplayer.PlayerInfo;

/** Network round-trip latency to the server, as reported in the player list. */
public final class PingSource implements StatSource {
    @Override public String id() { return "ping"; }
    @Override public Cadence cadence() { return Cadence.PER_FRAME; }

    @Override public boolean isAvailable(SamplingContext ctx) {
        return ctx.hasPlayer() && ctx.conn() != null;
    }

    @Override public void sample(SamplingContext ctx) {
        PlayerInfo info = ctx.conn().getPlayerInfo(ctx.player().getUUID());
        MtssDataHolder.ping = info != null ? info.getLatency() : -1;
    }
}
