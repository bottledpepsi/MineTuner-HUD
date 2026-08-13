package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;

/** Number of level-renderer sections currently rendered. */
public final class RenderedSectionsSource implements StatSource {
    @Override
    public String id() {
        return "rendered_sections";
    }

    @Override
    public Cadence cadence() {
        return Cadence.PER_FRAME;
    }

    @Override
    public boolean isAvailable(SamplingContext ctx) {
        return ctx.mc().levelRenderer != null;
    }

    @Override
    public void sample(SamplingContext ctx) {
        MineTunerDataHolder.renderedSections = ctx.mc().levelExtractor.countRenderedSections();
    }
}
