package bottled.minetuner.benchmark;

import bottled.minetuner.MineTunerDataHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BenchmarkSession's lifecycle state machine: start/stop/reset transitions, the
 * frozen-series contract (replaced, never mutated), the capture cap's auto-stop,
 * and the per-frame hook's cheap no-op when idle. Previously untested.
 */
class BenchmarkSessionTest {

    @BeforeEach
    void isolateStaticState() {
        BenchmarkSession.reset();
        MineTunerDataHolder.cpuPercent = -1.0;
        MineTunerDataHolder.gpuUsagePercent = -1.0;
    }

    @Test
    void startsIdle() {
        assertEquals(BenchmarkSession.State.IDLE, BenchmarkSession.state());
        assertFalse(BenchmarkSession.isRecording());
        assertFalse(BenchmarkSession.hasFrozenResult());
        assertEquals(0, BenchmarkSession.liveSampleCount());
        assertEquals(0L, BenchmarkSession.elapsedNanos());
    }

    @Test
    void recordWhileIdleIsIgnored() {
        BenchmarkSession.recordFrametimeIfRecording(16.6f);
        BenchmarkSession.recordFrametimeIfRecording(16.6f);
        assertEquals(BenchmarkSession.State.IDLE, BenchmarkSession.state());
        assertEquals(0, BenchmarkSession.liveSampleCount());
    }

    @Test
    void stopWhileIdleIsNoOp() {
        BenchmarkSession.stop();
        assertEquals(BenchmarkSession.State.IDLE, BenchmarkSession.state());
        assertFalse(BenchmarkSession.hasFrozenResult());
    }

    @Test
    void stopWhileStoppedIsNoOp() {
        BenchmarkSession.start();
        BenchmarkSession.recordFrametimeIfRecording(16.6f);
        BenchmarkSession.stop();
        int countAfterFirstStop = BenchmarkSession.finalSampleCount();
        BenchmarkSession.stop();
        assertEquals(BenchmarkSession.State.STOPPED, BenchmarkSession.state());
        assertEquals(countAfterFirstStop, BenchmarkSession.finalSampleCount());
    }

    @Test
    void startTransitionsToRecording() {
        BenchmarkSession.start();
        assertEquals(BenchmarkSession.State.RECORDING, BenchmarkSession.state());
        assertTrue(BenchmarkSession.isRecording());
        assertFalse(BenchmarkSession.hasFrozenResult());
        assertEquals(0, BenchmarkSession.liveSampleCount());
    }

    @Test
    void stopFreezesResults() {
        BenchmarkSession.start();
        for (int i = 0; i < 5; i++) BenchmarkSession.recordFrametimeIfRecording(16.6f);
        assertEquals(5, BenchmarkSession.liveSampleCount());
        BenchmarkSession.stop();

        assertEquals(BenchmarkSession.State.STOPPED, BenchmarkSession.state());
        assertTrue(BenchmarkSession.hasFrozenResult());
        assertEquals(5, BenchmarkSession.finalSampleCount());
        assertArrayEquals(new float[]{16.6f, 16.6f, 16.6f, 16.6f, 16.6f},
                BenchmarkSession.finalFrametimeSeries());
        // True average: frames * 1000 / sum(ms) — 5 frames at 16.6ms each.
        assertEquals(5 * 1000.0 / (5 * 16.6), BenchmarkSession.finalAvgFps(), 1e-3);
    }

    @Test
    void averageIsFramesOverElapsedNotMeanOfPerFrameFps() {
        BenchmarkSession.start();
        // One slow frame and one fast frame: mean of per-frame FPS would give
        // (60 + 120)/2 = 90; the true average is 2*1000/(16.6+8.3) = 80.3.
        BenchmarkSession.recordFrametimeIfRecording(16.6f);
        BenchmarkSession.recordFrametimeIfRecording(8.3f);
        BenchmarkSession.stop();
        assertEquals(2 * 1000.0 / (16.6 + 8.3), BenchmarkSession.finalAvgFps(), 1e-3);
    }

    @Test
    void percentileLowsUnavailableForShortRuns() {
        BenchmarkSession.start();
        BenchmarkSession.recordFrametimeIfRecording(16.6f);
        BenchmarkSession.stop();
        assertTrue(Float.isNaN(BenchmarkSession.final1LowFps()));
        assertTrue(Float.isNaN(BenchmarkSession.final01LowFps()));
    }

    @Test
    void cpuSentinelPropagatesWhenUnavailable() {
        MineTunerDataHolder.cpuPercent = -1.0;
        BenchmarkSession.start();
        for (int i = 0; i < 10; i++) BenchmarkSession.recordFrametimeIfRecording(16.6f);
        BenchmarkSession.stop();
        assertFalse(BenchmarkSession.hasCpuData());
        assertEquals(-1.0, BenchmarkSession.finalAvgCpuPercent());
    }

    @Test
    void gpuSentinelMeansNoGpuData() {
        MineTunerDataHolder.gpuUsagePercent = -1.0;
        BenchmarkSession.start();
        BenchmarkSession.recordFrametimeIfRecording(16.6f);
        BenchmarkSession.stop();
        assertFalse(BenchmarkSession.hasGpuData());
    }

    @Test
    void frozenSeriesAreReplacedNotMutatedAcrossStart() {
        BenchmarkSession.start();
        BenchmarkSession.recordFrametimeIfRecording(10f);
        BenchmarkSession.recordFrametimeIfRecording(20f);
        BenchmarkSession.recordFrametimeIfRecording(30f);
        BenchmarkSession.stop();

        float[] snapshot = BenchmarkSession.finalFrametimeSeries();
        float[] view = BenchmarkSession.finalFrametimeSeriesView();

        // Starting a new run must not disturb the previous run's frozen array...
        BenchmarkSession.start();
        assertArrayEquals(new float[]{10, 20, 30}, snapshot, 1e-6f);
        assertArrayEquals(new float[]{10, 20, 30}, view, 1e-6f);

        // ...and stopping the new run replaces (not edits) the published array.
        BenchmarkSession.recordFrametimeIfRecording(50f);
        BenchmarkSession.recordFrametimeIfRecording(60f);
        BenchmarkSession.stop();
        assertArrayEquals(new float[]{50, 60}, BenchmarkSession.finalFrametimeSeries(), 1e-6f);
        assertArrayEquals(new float[]{10, 20, 30}, view, 1e-6f);
    }

    @Test
    void autoStopsAtSampleCap() {
        BenchmarkSession.start();
        MineTunerDataHolder.cpuPercent = 25.0;
        for (int i = 0; i < BenchmarkSession.MAX_SAMPLES + 5; i++) {
            BenchmarkSession.recordFrametimeIfRecording(16.6f);
        }
        assertEquals(BenchmarkSession.State.STOPPED, BenchmarkSession.state());
        assertEquals(BenchmarkSession.MAX_SAMPLES, BenchmarkSession.finalSampleCount());
        assertTrue(BenchmarkSession.hasCpuData());
        assertTrue(BenchmarkSession.wasAutoStopped(),
                "a run that hit the cap must report wasAutoStopped() so the GUI can surface it");
        // Further samples after the auto-stop are ignored, not appended.
        int count = BenchmarkSession.finalSampleCount();
        BenchmarkSession.recordFrametimeIfRecording(16.6f);
        assertEquals(count, BenchmarkSession.finalSampleCount());
    }

    @Test
    void manualStopDoesNotSetAutoStopped() {
        BenchmarkSession.start();
        BenchmarkSession.recordFrametimeIfRecording(16.6f);
        BenchmarkSession.stop();
        assertFalse(BenchmarkSession.wasAutoStopped());
    }

    @Test
    void startingANewRunClearsTheAutoStoppedFlag() {
        BenchmarkSession.start();
        for (int i = 0; i < BenchmarkSession.MAX_SAMPLES + 5; i++) {
            BenchmarkSession.recordFrametimeIfRecording(16.6f);
        }
        assertTrue(BenchmarkSession.wasAutoStopped());
        BenchmarkSession.start();
        assertFalse(BenchmarkSession.wasAutoStopped());
        BenchmarkSession.reset();
        assertFalse(BenchmarkSession.wasAutoStopped());
    }

    @Test
    void resetReturnsToIdleAndClearsEverything() {
        BenchmarkSession.start();
        BenchmarkSession.recordFrametimeIfRecording(16.6f);
        BenchmarkSession.stop();
        assertTrue(BenchmarkSession.hasFrozenResult());

        BenchmarkSession.reset();
        assertEquals(BenchmarkSession.State.IDLE, BenchmarkSession.state());
        assertFalse(BenchmarkSession.hasFrozenResult());
        assertEquals(0, BenchmarkSession.liveSampleCount());
        assertEquals(0, BenchmarkSession.finalSampleCount());
        assertEquals(0, BenchmarkSession.finalFrametimeSeries().length);
        assertEquals(0L, BenchmarkSession.elapsedNanos());
    }
}
