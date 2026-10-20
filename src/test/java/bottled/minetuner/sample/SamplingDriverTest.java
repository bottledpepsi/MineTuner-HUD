package bottled.minetuner.sample;

import bottled.minetuner.config.MineTunerConfig.Stat;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SamplingDriverTest {

    private static final SamplingContext CTX = new SamplingContext(null, null, null, null);

    private static SamplingDriver driver(List<StatSource> sources, AtomicLong clock, Set<Stat>[] demand,
                                         boolean[] active) {
        return new SamplingDriver(sources, () -> CTX, clock::get, () -> demand[0], c -> active[0]);
    }

    @SuppressWarnings("unchecked")
    private static Set<Stat>[] demand(Set<Stat> initial) {
        return new Set[]{initial};
    }

    @Test
    void perTickSourceRunsEveryTickRegardlessOfAnythingElse() {
        FakeSource s = new FakeSource("a", Cadence.PER_TICK, Stat.HEALTH);
        SamplingDriver d = driver(List.of(s), new AtomicLong(), demand(Set.of(Stat.HEALTH)), new boolean[]{true});
        for (int i = 0; i < 5; i++) d.sampleTick();
        assertEquals(5, s.runs);
    }

    @Test
    void throttledSourceHonoursTheIntervalOnTheNanoClock() {
        FakeSource s = new FakeSource("t", Cadence.THROTTLED, Stat.CPU);
        AtomicLong clock = new AtomicLong(1_000_000_000L);
        SamplingDriver d = driver(List.of(s), clock, demand(Set.of(Stat.CPU)), new boolean[]{true});
        d.sampleTick();                       // first run immediately
        clock.addAndGet(400_000_000L);
        d.sampleTick();                       // too soon
        assertEquals(1, s.runs);
        clock.addAndGet(100_000_000L);
        d.sampleTick();                       // exactly 500 ms later
        assertEquals(2, s.runs);
    }

    @Test
    void aThrowingSourceDoesNotStopOthers() {
        FakeSource bad = new FakeSource("bad", Cadence.PER_TICK, Stat.HEALTH);
        bad.throwInSample = true;
        FakeSource bad2 = new FakeSource("bad2", Cadence.PER_TICK, Stat.HEALTH);
        bad2.throwInAvailable = true;
        FakeSource good = new FakeSource("good", Cadence.PER_TICK, Stat.HEALTH);
        SamplingDriver d = driver(List.of(bad, bad2, good), new AtomicLong(), demand(Set.of(Stat.HEALTH)), new boolean[]{true});
        d.sampleTick();
        d.sampleTick();
        assertEquals(2, good.runs);
        assertEquals(2, bad.runs); // keeps being polled so it can recover
    }

    @Test
    void noDemandMeansNoSourceRuns() {
        FakeSource a = new FakeSource("a", Cadence.PER_TICK, Stat.HEALTH);
        FakeSource b = new FakeSource("b", Cadence.THROTTLED, Stat.CPU);
        SamplingDriver d = driver(List.of(a, b), new AtomicLong(), demand(Set.of()), new boolean[]{true});
        d.sampleTick();
        assertEquals(0, a.runs);
        assertEquals(0, b.runs);
    }

    @Test
    void enablingAStatRunsExactlyItsFeedingSource() {
        FakeSource a = new FakeSource("a", Cadence.PER_TICK, Stat.HEALTH);
        FakeSource b = new FakeSource("b", Cadence.PER_TICK, Stat.PING);
        Set<Stat>[] demand = demand(Set.of());
        SamplingDriver d = driver(List.of(a, b), new AtomicLong(), demand, new boolean[]{true});
        d.sampleTick();
        demand[0] = Set.of(Stat.PING);
        d.sampleTick();
        assertEquals(0, a.runs);
        assertEquals(1, b.runs);
    }

    @Test
    void sourceNeededAgainRunsImmediatelyEvenInsideItsThrottleWindow() {
        FakeSource t = new FakeSource("t", Cadence.THROTTLED, Stat.CPU);
        AtomicLong clock = new AtomicLong(5_000_000_000L);
        Set<Stat>[] demand = demand(Set.of(Stat.CPU));
        SamplingDriver d = driver(List.of(t), clock, demand, new boolean[]{true});
        d.sampleTick();
        demand[0] = Set.of();
        clock.addAndGet(50_000_000L);
        d.sampleTick();               // not needed: skipped
        demand[0] = Set.of(Stat.CPU);
        clock.addAndGet(50_000_000L);
        d.sampleTick();               // needed again, only 100 ms after the last run
        assertEquals(2, t.runs);
    }

    @Test
    void inactiveContextSamplesNothing() {
        FakeSource a = new FakeSource("a", Cadence.PER_TICK, Stat.HEALTH);
        boolean[] active = {false};
        SamplingDriver d = driver(List.of(a), new AtomicLong(), demand(Set.of(Stat.HEALTH)), active);
        d.sampleTick();
        assertEquals(0, a.runs);
        active[0] = true;
        d.sampleTick();
        assertEquals(1, a.runs);
    }

    @Test
    void eventPushedSourcesAreNeverPolled() {
        FakeSource e = new FakeSource("e", Cadence.EVENT_PUSHED, Stat.TPS);
        SamplingDriver d = driver(List.of(e), new AtomicLong(), demand(Set.of(Stat.TPS)), new boolean[]{true});
        d.sampleTick();
        assertEquals(0, e.runs);
    }

    private static final class FakeSource implements StatSource {
        final String id;
        final Cadence cadence;
        final Set<Stat> feeds;
        int runs = 0;
        boolean throwInSample = false;
        boolean throwInAvailable = false;

        FakeSource(String id, Cadence cadence, Stat... feeds) {
            this.id = id;
            this.cadence = cadence;
            this.feeds = Set.of(feeds);
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Cadence cadence() {
            return cadence;
        }

        @Override
        public Set<Stat> feeds() {
            return feeds;
        }

        @Override
        public boolean isAvailable(SamplingContext ctx) {
            if (throwInAvailable) throw new IllegalStateException("availability boom");
            return true;
        }

        @Override
        public void sample(SamplingContext ctx) {
            runs++;
            if (throwInSample) throw new IllegalStateException("boom");
        }
    }
}
