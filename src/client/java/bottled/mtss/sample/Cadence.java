package bottled.mtss.sample;

/** How often a { StatSource} runs. */
public enum Cadence {
    /** Every render frame. */
    PER_FRAME,
    /** At most once per game tick (~50ms). */
    PER_TICK,
    /** At most once per 500ms. */
    THROTTLED,
    /** Never polled by the driver. */
    EVENT_PUSHED
}
