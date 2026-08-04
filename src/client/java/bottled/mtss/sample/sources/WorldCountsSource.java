package bottled.mtss.sample.sources;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.sample.Cadence;
import bottled.mtss.sample.SamplingContext;
import bottled.mtss.sample.StatSource;

/** Loaded-level counters: entity count, loaded chunk count, current dimension. */
public final class WorldCountsSource implements StatSource {
    @Override public String id() { return "world_counts"; }
    @Override public Cadence cadence() { return Cadence.PER_FRAME; }
    @Override public boolean isAvailable(SamplingContext ctx) { return ctx.hasLevel(); }

    @Override public void sample(SamplingContext ctx) {
        MtssDataHolder.entityCount   = ctx.level().getEntityCount();
        MtssDataHolder.loadedChunks  = ctx.level().getChunkSource().getLoadedChunksCount();
        MtssDataHolder.dimensionName = ctx.level().dimension().identifier().getPath();
    }
}
