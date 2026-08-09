package bottled.mtss.sample;

/** Everything MTSS needs to pull ONE raw value (or a small cohesive group. */
public interface StatSource {

    /** Stable id for logging/debugging. */
    String id();

    /** How often sample() should run. */
    Cadence cadence();

    /** Cheap precondition check. */
    default boolean isAvailable(SamplingContext ctx) {
        return true;
    }

    /** Pull the raw value(s) from ctx and write them into MtssDataHolder. */
    void sample(SamplingContext ctx);
}
