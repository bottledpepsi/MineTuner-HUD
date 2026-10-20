package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;

import java.util.Set;

/** Loaded-level counters. */
public final class WorldCountsSource implements StatSource {
    @Override
    public String id() {
        return "world_counts";
    }

    @Override
    public Set<MineTunerConfig.Stat> feeds() {
        return Set.of(MineTunerConfig.Stat.ENTITIES, MineTunerConfig.Stat.CHUNKS, MineTunerConfig.Stat.DIMENSION);
    }

    @Override
    public Cadence cadence() {
        return Cadence.PER_TICK;
    }

    @Override
    public boolean isAvailable(SamplingContext ctx) {
        return ctx.hasLevel();
    }

    @Override
    public void sample(SamplingContext ctx) {
        MineTunerDataHolder.entityCount = ctx.level().getEntityCount();
        MineTunerDataHolder.loadedChunks = ctx.level().getChunkSource().getLoadedChunksCount();
        MineTunerDataHolder.dimensionName = ctx.level().dimension().identifier().getPath();
    }
}
