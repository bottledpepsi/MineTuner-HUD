package bottled.minetuner.sample;

/**
 * How often a {@link StatSource} runs. Sampling is driven by client events, never by
 * rendering, so it continues while the overlay is hidden or a screen is open.
 */
public enum Cadence {
    /** Once per client tick (every {@code END_CLIENT_TICK}). */
    PER_TICK,
    /** At most once per {@link SamplingDriver#THROTTLE_NANOS} (500 ms), checked on tick. */
    THROTTLED,
    /** Never polled by the driver. */
    EVENT_PUSHED
}
