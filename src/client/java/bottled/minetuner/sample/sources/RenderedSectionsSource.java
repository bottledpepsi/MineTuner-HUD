package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;

import java.util.Set;

/** Number of level-renderer sections currently rendered. */
public final class RenderedSectionsSource implements StatSource {
    @Override
    public String id() {
        return "rendered_sections";
    }

    @Override
    public Set<MineTunerConfig.Stat> feeds() {
        return Set.of(MineTunerConfig.Stat.RENDERED_SECTIONS);
    }

    @Override
    public Cadence cadence() {
        return Cadence.PER_TICK;
    }

    @Override
    public boolean isAvailable(SamplingContext ctx) {
        // Guard the object sample() actually dereferences — this used to check
        // levelRenderer while sample() counted sections via levelExtractor, so the
        // guard protected the wrong lifecycle (an NPE here would only be caught by
        // SamplingDriver's catch-all and misattributed to a transient failure).
        return ctx.mc().levelExtractor != null;
    }

    @Override
    public void sample(SamplingContext ctx) {
        MineTunerDataHolder.renderedSections = ctx.mc().levelExtractor.countRenderedSections();
    }
}
