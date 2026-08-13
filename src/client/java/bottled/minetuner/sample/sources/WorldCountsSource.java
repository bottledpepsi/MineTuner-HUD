package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;

/** Loaded-level counters. */
public final class WorldCountsSource implements StatSource {
    @Override
    public String id() {
        return "world_counts";
    }

    @Override
    public Cadence cadence() {
        return Cadence.PER_FRAME;
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
