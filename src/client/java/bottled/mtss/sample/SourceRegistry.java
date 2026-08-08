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
        register(new TargetingSource());          // crosshair target block/entity, moving state
        register(new PlayerVitalsSource());       // health, hunger, saturation, armor, air, xp, game mode, held item
        register(new WorldStateSource());         // time, moon phase, weather, difficulty, chunk pos, distance from spawn, players online
        register(new RenderedSectionsSource());
        register(new SlowMetricsSource());        // THROTTLED — cpu, gc
        register(new FastMetricsSource());        // memory + history push; must run last (reads fields above)
        // Server tick rate is EVENT_PUSHED via
        // ClientPacketListenerMixin#mtss$onTickingState — not driven from
        // here, listed for discoverability only.
    }

    // Registration only ever happens once, in the static initializer above,
    // so the defensive copy only needs to be made once too — snapshotting it
    // here instead of on every all() call avoids a fresh List allocation
    // every single frame (all() is called from SamplingDriver.sampleAll(),
    // which runs once per render frame).
    private static final List<StatSource> SNAPSHOT = List.copyOf(ALL);

    public static List<StatSource> all() { return SNAPSHOT; }
}
