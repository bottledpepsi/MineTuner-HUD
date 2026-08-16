package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;
import bottled.minetuner.stat.math.PercentileLowFps;

public final class PercentileLowSource implements StatSource {

    @Override
    public String id() {
        return "percentile_low_fps";
    }

    @Override
    public Cadence cadence() {
        return Cadence.THROTTLED;
    }

    @Override
    public void sample(SamplingContext ctx) {

        float[] rawFrametimes = MineTunerDataHolder.getRawFrametimeHistory();

        float fps1Low = PercentileLowFps.computeLowFps(
                rawFrametimes, 0.01f, MineTunerDataHolder.FPS_1LOW_MIN_SAMPLES);
        float fps01Low = PercentileLowFps.computeLowFps(
                rawFrametimes, 0.001f, MineTunerDataHolder.FPS_01LOW_MIN_SAMPLES);

        MineTunerDataHolder.updatePercentileLowFps(fps1Low, fps01Low);
    }
}
