package bottled.minetuner.sample;

import bottled.minetuner.MineTunerMod;

import java.util.HashSet;
import java.util.Set;

/** Runs every registered StatSource at its declared cadence, once per frame. */
public final class SamplingDriver {
    private static final long TICK_MS = 50, THROTTLE_MS = 500;
    private static final Set<String> WARNED_SOURCE_IDS = new HashSet<>();
    private static long lastTickMs = 0, lastThrottledMs = 0;

    private SamplingDriver() {
    }

    /** Call once per render frame. */
    public static void sampleAll() {
        SamplingContext ctx = SamplingContext.capture();
        long now = System.currentTimeMillis();
        boolean runTick = now - lastTickMs >= TICK_MS;
        boolean runThrottled = now - lastThrottledMs >= THROTTLE_MS;
        if (runTick) lastTickMs = now;
        if (runThrottled) lastThrottledMs = now;

        for (StatSource src : SourceRegistry.all()) {
            boolean shouldRun = switch (src.cadence()) {
                case PER_FRAME -> true;
                case PER_TICK -> runTick;
                case THROTTLED -> runThrottled;
                case EVENT_PUSHED -> false;
            };
            if (shouldRun && src.isAvailable(ctx)) {
                try {
                    src.sample(ctx);
                } catch (Exception e) {
                    // One bad source shouldn't take the whole overlay down. Sampling continues
                    // every frame (so it recovers on its own if the failure was transient), but
                    // only the first failure per source is logged, so a source that's
                    // consistently broken doesn't spam the log forever.
                    if (WARNED_SOURCE_IDS.add(src.id())) {
                        MineTunerMod.LOGGER.error("StatSource \"{}\" threw during sample() — its stat(s) "
                                + "will show stale/default values until it recovers.", src.id(), e);
                    }
                }
            }
        }
    }
}
