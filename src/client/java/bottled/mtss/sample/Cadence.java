package bottled.mtss.sample;

/** How often a {@link StatSource} runs. Makes the mod's existing implicit cadences explicit. */
public enum Cadence {
    /** Every render frame — cheap reads (position, entity/chunk counts). */
    PER_FRAME,
    /** At most once per game tick (~50ms) — values that only change on tick rate. */
    PER_TICK,
    /** At most once per 500ms — expensive reads (MXBean polls). Matches today's updateSlowMetrics() throttle. */
    THROTTLED,
    /** Never polled by the driver — pushed externally (e.g. a Mixin on a packet handler, like TPS today). Registered here for discoverability even though sample() fires elsewhere. */
    EVENT_PUSHED
}
