package bottled.minetuner.sample;

import bottled.minetuner.sample.sources.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Central registry of every {@link StatSource} that {@link SamplingDriver} drives each frame.
 * To add a new client-side value, implement {@link StatSource} and register an instance below.
 */
public final class SourceRegistry {
    private static final List<StatSource> ALL = new ArrayList<>();

    static {
        register(new SingleplayerMsptSource());
        register(new ClientPerfSource());         // fps.
        register(new PingSource());
        register(new WorldCountsSource());         // entities, chunks, dimension.
        register(new PlayerPositionSource());      // x/y/z, facing, speed.
        register(new PlayerEnvironmentSource());   // light level, biome.
        register(new TargetingSource());           // crosshair target block/entity, moving state.
        register(new PlayerVitalsSource());        // health, hunger, saturation, armor, air, xp, game mode, held item.
        register(new WorldStateSource());          // weather, difficulty, chunk pos, distance from spawn, etc.
        register(new RenderedSectionsSource());
        register(new SlowMetricsSource());         // THROTTLED.
        register(new FastMetricsSource());         // memory + history push.
        // Server tick rate is EVENT_PUSHED by ClientPacketListenerMixin#minetuner$onTickingState,
        // not driven from here — not registered as an instance, since its sample() would
        // never be called under an EVENT_PUSHED cadence. Listed here only for discoverability.
        //
        // Hardware sensors (GPU temp/clock/usage, VRAM) are likewise EVENT_PUSHED, but by
        // HardwareSensorPoller's own dedicated background thread rather than a mixin; see
        // that class's doc for the full design. Also intentionally not registered here.
    }

    /**
     * Immutable snapshot of every registered source, built lazily on first use.
     * Registration only ever happens once, in the static initializer above, so a single
     * cached copy is safe: {@link #all()} can be called every render frame without a
     * fresh {@code List} allocation each time. This must be computed lazily (not as a
     * field initializer declared alongside {@link #ALL}) since field initializers run
     * in declaration order — capturing a copy of {@code ALL} above the static block
     * that populates it would freeze an empty list, which is exactly the bug this
     * lazy form fixes.
     */
    private static List<StatSource> snapshot;

    private SourceRegistry() {
    }

    private static void register(StatSource src) {
        ALL.add(src);
    }

    public static List<StatSource> all() {
        if (snapshot == null) {
            snapshot = List.copyOf(ALL);
        }
        return snapshot;
    }
}
