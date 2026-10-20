package bottled.minetuner.sample;

import bottled.minetuner.MineTunerMod;
import bottled.minetuner.config.MineTunerConfig;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Runs {@link StatSource}s at their declared cadence, driven by client ticks (never by
 * rendering) and only for stats somebody currently needs.
 */
public final class SamplingDriver {

    /**
     * Minimum spacing between runs of a {@link Cadence#THROTTLED} source.
     */
    public static final long THROTTLE_NANOS = 500_000_000L;

    private static SamplingDriver instance;

    private final List<StatSource> sources;
    private final Supplier<SamplingContext> contextSupplier;
    private final LongSupplier nanoClock;
    private final Supplier<Set<MineTunerConfig.Stat>> demand;
    private final Predicate<SamplingContext> active;

    private final long[] lastRunNanos;
    private final boolean[] hasRun;
    private final Set<String> warnedSourceIds = new HashSet<>();

    public SamplingDriver(List<StatSource> sources, Supplier<SamplingContext> contextSupplier,
                          LongSupplier nanoClock, Supplier<Set<MineTunerConfig.Stat>> demand,
                          Predicate<SamplingContext> active) {
        this.sources = List.copyOf(sources);
        this.contextSupplier = contextSupplier;
        this.nanoClock = nanoClock;
        this.demand = demand;
        this.active = active;
        this.lastRunNanos = new long[this.sources.size()];
        this.hasRun = new boolean[this.sources.size()];
    }

    /**
     * The live driver: every registered source, real clock, real demand, active only while
     * connected to a world/server.
     */
    public static synchronized SamplingDriver instance() {
        if (instance == null) {
            instance = new SamplingDriver(SourceRegistry.all(), SamplingContext::capture,
                    System::nanoTime, DemandTracker::currentDemand, ctx -> ctx.conn() != null);
        }
        return instance;
    }

    /**
     * Call once per client tick.
     */
    public void sampleTick() {
        SamplingContext ctx = contextSupplier.get();
        if (!active.test(ctx)) return;
        Set<MineTunerConfig.Stat> needed = demand.get();
        long now = nanoClock.getAsLong();

        for (int i = 0; i < sources.size(); i++) {
            StatSource src = sources.get(i);
            if (src.cadence() == Cadence.EVENT_PUSHED) continue;

            if (Collections.disjoint(src.feeds(), needed)) {
                // Forget the last run so the source runs at the very next tick once it is
                // needed again, instead of waiting out a stale throttle window.
                hasRun[i] = false;
                continue;
            }
            if (src.cadence() == Cadence.THROTTLED && hasRun[i]
                    && now - lastRunNanos[i] < THROTTLE_NANOS) {
                continue;
            }
            hasRun[i] = true;
            lastRunNanos[i] = now;
            runSafely(src, ctx);
        }
    }

    private void runSafely(StatSource src, SamplingContext ctx) {
        try {
            if (src.isAvailable(ctx)) src.sample(ctx);
        } catch (Exception e) {
            // One bad source shouldn't take the whole overlay down. It keeps being polled (so
            // it recovers by itself if the failure was transient), but only the first failure
            // per source is logged so a consistently broken one doesn't spam the log.
            if (warnedSourceIds.add(src.id())) {
                MineTunerMod.LOGGER.error("StatSource \"{}\" threw while sampling; its stat(s) "
                        + "will show stale/default values until it recovers.", src.id(), e);
            }
        }
    }
}
