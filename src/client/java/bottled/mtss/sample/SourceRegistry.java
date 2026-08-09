package bottled.mtss.sample;

import bottled.mtss.sample.sources.*;

import java.util.ArrayList;
import java.util.List;

/** To add a new client-side value. */
public final class SourceRegistry {
    private static final List<StatSource> ALL = new ArrayList<>();
    // Registration only ever happens once, in the static initializer above,.
    // so the defensive copy only needs to be made once too.
    // here instead of on every all() call avoids a fresh List allocation.
    // every single frame (all() is called from SamplingDriver.sampleAll(),.
    // which runs once per render frame).
    private static final List<StatSource> SNAPSHOT = List.copyOf(ALL);

    static {
        register(new SingleplayerMsptSource());
        register(new ClientPerfSource());        // fps.
        register(new PingSource());
        register(new WorldCountsSource());        // entities, chunks, dimension.
        register(new PlayerPositionSource());     // x/y/z, facing, speed.
        register(new PlayerEnvironmentSource());  // light level, biome.
        register(new TargetingSource());          // crosshair target block/entity, moving state.
        register(new PlayerVitalsSource());       // health, hunger, saturation, armor, air, xp, game mode, held item.
        register(new WorldStateSource());         // time, moon phase, weather, difficulty, chunk pos, distance from spawn,.
        register(new RenderedSectionsSource());
        register(new SlowMetricsSource());        // THROTTLED.
        register(new FastMetricsSource());        // memory + history push.
        // Server tick rate is EVENT_PUSHED by.
        // ClientPacketListenerMixin#mtss$onTickingState.
        // here, listed for discoverability only.
        // Hardware sensors (GPU temp/clock/usage, VRAM) are EVENT_PUSHED by.
        // HardwareSensorPoller's own background thread.
        // bottled.mtss.sample.sources.HardwareSensorSource's class doc.
        // Not driven from here either, and not registered as an instance.
        // sample() would never be called (EVENT_PUSHED), so a real.
        // registration here would misleadingly suggest SamplingDriver.
        // drives it.
    }

    private SourceRegistry() {
    }

    private static void register(StatSource src) {
        ALL.add(src);
    }

    public static List<StatSource> all() {
        return SNAPSHOT;
    }
}
