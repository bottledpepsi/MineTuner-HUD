package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;

import java.util.Set;

/** Heap memory (used/max) plus the once-per-frame ring-buffer history push for. */
public final class FastMetricsSource implements StatSource {
    @Override
    public String id() {
        return "fast_metrics";
    }

    @Override
    public Set<MineTunerConfig.Stat> feeds() {
        return Set.of(MineTunerConfig.Stat.TPS, MineTunerConfig.Stat.MSPT, MineTunerConfig.Stat.FPS, MineTunerConfig.Stat.CPU, MineTunerConfig.Stat.PING, MineTunerConfig.Stat.MEMORY, MineTunerConfig.Stat.SPEED, MineTunerConfig.Stat.HEALTH, MineTunerConfig.Stat.HUNGER, MineTunerConfig.Stat.ARMOR, MineTunerConfig.Stat.GPU_TEMP, MineTunerConfig.Stat.GPU_CLOCK, MineTunerConfig.Stat.GPU_USAGE, MineTunerConfig.Stat.VRAM_USED);
    }

    @Override
    public Cadence cadence() {
        return Cadence.PER_TICK;
    }

    @Override
    public void sample(SamplingContext ctx) {
        MineTunerDataHolder.updateFastMetrics();
    }
}
