package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.Set;

/** Network round-trip latency to the server, as reported in the player list. */
public final class PingSource implements StatSource {
    @Override
    public String id() {
        return "ping";
    }

    @Override
    public Set<MineTunerConfig.Stat> feeds() {
        return Set.of(MineTunerConfig.Stat.PING);
    }

    @Override
    public Cadence cadence() {
        return Cadence.THROTTLED;
    }

    @Override
    public boolean isAvailable(SamplingContext ctx) {
        return ctx.hasPlayer() && ctx.conn() != null;
    }

    @Override
    public void sample(SamplingContext ctx) {
        PlayerInfo info = ctx.conn().getPlayerInfo(ctx.player().getUUID());
        MineTunerDataHolder.ping = info != null ? info.getLatency() : -1;
    }
}
