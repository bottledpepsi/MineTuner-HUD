package bottled.mtss.sample.sources;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.sample.Cadence;
import bottled.mtss.sample.SamplingContext;
import bottled.mtss.sample.StatSource;

/** Number of level-renderer sections currently rendered. */
public final class RenderedSectionsSource implements StatSource {
    @Override public String id() { return "rendered_sections"; }
    @Override public Cadence cadence() { return Cadence.PER_FRAME; }
    @Override public boolean isAvailable(SamplingContext ctx) { return ctx.mc().levelRenderer != null; }

    @Override public void sample(SamplingContext ctx) {
        MtssDataHolder.renderedSections = ctx.mc().levelExtractor.countRenderedSections();
    }
}
