package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.core.PercentileLowFps;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;

import java.util.Set;

public final class PercentileLowSource implements StatSource {

    @Override
    public String id() {
        return "percentile_low_fps";
    }

    @Override
    public Set<MineTunerConfig.Stat> feeds() {
        return Set.of(MineTunerConfig.Stat.FPS_1PCT_LOW, MineTunerConfig.Stat.FPS_01PCT_LOW);
    }

    @Override
    public Cadence cadence() {
        return Cadence.THROTTLED;
    }

    @Override
    public void sample(SamplingContext ctx) {

        float[] rawFrametimes = MineTunerDataHolder.getRawFrametimeHistory();

        PercentileLowFps.Lows lows = PercentileLowFps.computeLows(rawFrametimes,
                MineTunerDataHolder.FPS_1LOW_MIN_SAMPLES, MineTunerDataHolder.FPS_01LOW_MIN_SAMPLES);

        MineTunerDataHolder.updatePercentileLowFps(lows.low1Pct(), lows.low01Pct());
    }
}
