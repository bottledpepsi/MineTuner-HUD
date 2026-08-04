package bottled.mtss.sample;

/**
 * Everything MTSS needs to pull ONE raw value (or a small cohesive group —
 * e.g. playerX/Y/Z + facing + speed) out of the game and into
 * MtssDataHolder. The acquisition-side counterpart to StatDefinition:
 * that interface answers "how do I render this", this answers "how do I
 * get this in the first place". Implementations live in
 * bottled.mtss.sample.sources and are wired up once in SourceRegistry —
 * nothing else needs to change to add a new client-side value.
 */
public interface StatSource {

    /** Stable id for logging/debugging. Doesn't need to map 1:1 to a Stat. */
    String id();

    /** How often sample() should run. */
    Cadence cadence();

    /** Cheap precondition check — skip sample() this cycle if false. Default: always available. */
    default boolean isAvailable(SamplingContext ctx) { return true; }

    /** Pull the raw value(s) from ctx and write them into MtssDataHolder. Only called when isAvailable() is true. */
    void sample(SamplingContext ctx);
}
