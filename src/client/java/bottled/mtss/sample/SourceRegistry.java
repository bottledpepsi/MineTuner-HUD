package bottled.mtss.sample;

import bottled.mtss.sample.sources.*;

import java.util.ArrayList;
import java.util.List;

/**
 * To add a new client-side value:
 *   1. Write a class in bottled.mtss.sample.sources implementing StatSource.
 *   2. Register an instance below.
 * No other file changes — SamplingDriver iterates this list blindly.
 * <p>
 * Registration order matters for one case: {@link FastMetricsSource} reads
 * fps/ping/cpuPercent/speedBps to push into the graph history buffers, so it
 * must run after the sources that set those fields. It's registered last
 * for that reason — keep it there if you add more PER_FRAME sources above it.
 */
public final class SourceRegistry {
    private SourceRegistry() {}
    private static final List<StatSource> ALL = new ArrayList<>();
    private static void register(StatSource src) { ALL.add(src); }

    static {
        register(new SingleplayerMsptSource());
        register(new ClientPerfSource());        // fps
        register(new PingSource());
        register(new WorldCountsSource());        // entities, chunks, dimension
        register(new PlayerPositionSource());     // x/y/z, facing, speed
        register(new PlayerEnvironmentSource());  // light level, biome
        register(new RenderedSectionsSource());
        register(new SlowMetricsSource());        // THROTTLED — cpu, gc
        register(new FastMetricsSource());        // memory + history push; must run last (reads fields above)
        // Server tick rate is EVENT_PUSHED via
        // ClientPacketListenerMixin#mtss$onTickingState — not driven from
        // here, listed for discoverability only.
    }

    public static List<StatSource> all() { return List.copyOf(ALL); }
}
