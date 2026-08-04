package bottled.mtss.sample;

/** Runs every registered StatSource at its declared cadence, once per frame. */
public final class SamplingDriver {
    private SamplingDriver() {}

    private static long lastTickMs = 0, lastThrottledMs = 0;
    private static final long TICK_MS = 50, THROTTLE_MS = 500;

    /** Call once per render frame — same spot the old inline block lived. */
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
                    // one bad source shouldn't take the whole overlay down
                    // — log once, consider marking it dead until reload.
                }
            }
        }
    }
}
