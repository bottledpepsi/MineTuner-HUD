package bottled.minetuner.sample;

import bottled.minetuner.config.MineTunerConfig;

import java.util.Set;

/** Everything MineTuner needs to pull ONE raw value (or a small cohesive group of
 *  related values, e.g. WorldStateSource covers weather/difficulty/chunk-pos/
 *  distance-from-spawn together) from the game and write it into
 *  {@link bottled.minetuner.MineTunerDataHolder}. */
public interface StatSource {

    /** Stable id for logging/debugging. */
    String id();

    /** How often sample() should run. */
    Cadence cadence();

    /**
     * The stats this source produces data for. The driver skips the source entirely while
     * none of them is needed (see {@link DemandTracker}).
     */
    Set<MineTunerConfig.Stat> feeds();

    /** Cheap precondition check. */
    default boolean isAvailable(SamplingContext ctx) {
        return true;
    }

    /** Pull the raw value(s) from ctx and write them into MineTunerDataHolder. */
    void sample(SamplingContext ctx);
}
