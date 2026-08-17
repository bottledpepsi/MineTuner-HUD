package bottled.minetuner.benchmark;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.MineTunerMod;
import bottled.minetuner.stat.math.PercentileLowFps;

import java.nio.file.Path;
import java.util.Arrays;

public final class BenchmarkSession {

    /** Idle: no benchmark has ever run, or {@link #reset()} was called with nothing
     *  frozen to show. Recording: actively capturing frametimes. Stopped: capture frozen,
     *  final results available via the getters below. */
    public enum State {
        IDLE, RECORDING, STOPPED
    }

    /** Ceiling on captured samples, see the class doc's "Cap" section above. Public so the
     *  GUI can explain an auto-stop if it ever observes one (state flips STOPPED with no
     *  user Stop keypress). */
    public static final int MAX_SAMPLES = 240 /* fps */ * 60 /* sec */ * 30 /* min */;

    private static State state = State.IDLE;

    // --- Active capture ---
    // Manually-doubling float[] rather than List<Float> to avoid autoboxing on a buffer
    // that can grow into the hundreds of thousands of entries — this is appended to once
    // per rendered frame while recording, so avoiding a Float allocation per sample matters
    // here in a way it wouldn't for a UI-scale list.
    //
    // Three parallel arrays (frametime/cpu/gpu), all indexed by the same captureSize and
    // grown/reset together, rather than one array-of-records — keeps each series a plain
    // float[] for cheap graphing (MineTunerDataHolder.RingBuffer#snapshot()-style copy-out)
    // and for the CSV writer, with no per-sample object/boxing overhead either way.
    private static float[] capture = new float[4096];
    /** CPU% at each sample's frame, or -1f where CPU% was unavailable that frame — same
     *  sentinel convention as MineTunerDataHolder#cpuPercent itself. Parallel to capture. */
    private static float[] cpuCapture = new float[4096];
    /** GPU usage% at each sample's frame, or -1f where unavailable (hardware sensors off,
     *  or not yet polled) — same sentinel convention as MineTunerDataHolder#gpuUsagePercent.
     *  Parallel to capture. */
    private static float[] gpuCapture = new float[4096];
    private static int captureSize = 0;

    private static long startNanos = 0L;
    private static long stopNanos = 0L;
    /** CPU% sampled once at Start, for a "what was CPU doing during this run" reference
     *  point — see the class doc; this mirrors the same cpuPercent field/sentinel
     *  MineTunerDataHolder#getFormattedCpu already uses, not a separate CPU reading path. */
    private static double startCpuPercent = -1.0;

    // --- Frozen final results, valid only once state == STOPPED ---
    private static float finalAvgFps = 0f;
    private static float final1LowFps = PercentileLowFps.UNAVAILABLE;
    private static float final01LowFps = PercentileLowFps.UNAVAILABLE;
    private static double finalAvgCpuPercent = -1.0;
    private static long finalDurationNanos = 0L;
    private static int finalSampleCount = 0;

    // Frozen copies of the full per-sample series, sized exactly to finalSampleCount, kept
    // around after stop() (unlike the older behavior of only keeping the derived summary
    // numbers) so the GUI's post-stop graphs and saveToFolder() both have real per-sample
    // history to draw/write from, not just the three headline numbers. Frozen separately
    // from the live `capture`/`cpuCapture`/`gpuCapture` buffers (rather than just trusting
    // captureSize to still be valid) so a subsequent start() — which resets captureSize back
    // to 0 to begin reusing the same backing arrays — can never retroactively invalidate or
    // truncate a previous run's already-stopped results out from under the GUI/writer.
    private static float[] finalFrametimes = new float[0];
    private static float[] finalCpuSeries = new float[0];
    private static float[] finalGpuSeries = new float[0];

    private BenchmarkSession() {
    }

    // --- Lifecycle ---

    /** Starts a fresh benchmark, discarding any previous run's results — matches RTSS's
     *  F9 "start/reset" semantics: pressing Start while a previous run is STOPPED (still
     *  showing old results) clearly discards those results and begins fresh rather than
     *  blending two runs' data together, and pressing Start again while already RECORDING
     *  restarts cleanly rather than corrupting/appending to the in-progress capture. */
    public static void start() {
        captureSize = 0; // Retains the backing arrays (no reallocation) — see clear()'s
        // reasoning below in MineTunerDataHolder.RingBuffer for the same "just reset the
        // count" approach; stale float values beyond captureSize are never read by
        // anything that inspects capture/cpuCapture/gpuCapture, since every reader is
        // bounded by captureSize.
        startNanos = System.nanoTime();
        stopNanos = 0L;
        startCpuPercent = MineTunerDataHolder.cpuPercent;
        finalAvgFps = 0f;
        final1LowFps = PercentileLowFps.UNAVAILABLE;
        final01LowFps = PercentileLowFps.UNAVAILABLE;
        finalAvgCpuPercent = -1.0;
        finalDurationNanos = 0L;
        finalSampleCount = 0;
        // Deliberately NOT resetting finalFrametimes/finalCpuSeries/finalGpuSeries here,
        // unlike every scalar final* field above. Those scalars are safe to clear early
        // because nothing ever reads them outside state()==STOPPED (see drawLiveSection(),
        // which reads only elapsedNanos()/liveSampleCount() while RECORDING, never the
        // final* getters). The three series arrays are different: they're also the class's
        // documented on-the-record "valid once hasFrozenResult() is true" contract (see
        // their own field doc above), and hasFrozenResult() only flips to false once THIS
        // start() call's state=RECORDING below takes effect — clearing them here, before
        // stop() has produced this run's own data to replace them with, would leave a
        // (currently harmless, but contract-violating) window where a caller could observe
        // an empty series for a run whose headline numbers might still be visible elsewhere.
        // They're correctly overwritten only inside stop(), the one place that should ever
        // change them with real data — see stop()'s own finalFrametimes = window; line below.
        state = State.RECORDING;
    }

    /** Stops recording and computes final avg/1%-low/0.1%-low over exactly the recorded
     *  window, freezing them (and the full per-sample series behind {@link #finalFrametimeSeries()}
     *  / {@link #finalCpuSeries()} / {@link #finalGpuSeries()}) for display until the next
     *  {@link #start()}. A no-op if not currently RECORDING (e.g. a stray Stop keypress
     *  while IDLE/STOPPED). */
    public static void stop() {
        if (state != State.RECORDING) return;

        stopNanos = System.nanoTime();
        finalDurationNanos = stopNanos - startNanos;
        finalSampleCount = captureSize;

        float[] window = Arrays.copyOf(capture, captureSize);
        finalFrametimes = window;
        finalCpuSeries = Arrays.copyOf(cpuCapture, captureSize);
        finalGpuSeries = Arrays.copyOf(gpuCapture, captureSize);

        // Averaging each raw sample's instantaneous FPS (rather than averaging deltaMs
        // once and converting a single time) matches how MineTunerDataHolder's own
        // session Avg FPS is defined — see recordSessionFpsSample()'s fpsSum/fpsValue,
        // which likewise sums per-frame FPS values, not per-frame frametimes.
        double sum = 0.0;
        for (float deltaMs : window) {
            if (deltaMs > 0f) sum += 1000f / deltaMs;
        }
        finalAvgFps = captureSize > 0 ? (float) (sum / captureSize) : 0f;
        final1LowFps = PercentileLowFps.computeLowFps(
                window, 0.01f, MineTunerDataHolder.FPS_1LOW_MIN_SAMPLES);
        final01LowFps = PercentileLowFps.computeLowFps(
                window, 0.001f, MineTunerDataHolder.FPS_01LOW_MIN_SAMPLES);

        double endCpuPercent = MineTunerDataHolder.cpuPercent;
        if (startCpuPercent >= 0 && endCpuPercent >= 0) {
            finalAvgCpuPercent = (startCpuPercent + endCpuPercent) / 2.0;
        } else if (endCpuPercent >= 0) {
            finalAvgCpuPercent = endCpuPercent;
        } else {
            finalAvgCpuPercent = startCpuPercent;
        }

        state = State.STOPPED;
    }

    /** Discards any frozen results and returns to IDLE, e.g. if a future settings/reset
     *  action ever needs a "forget everything" path distinct from starting a new run.
     *  Not currently wired to any keybind/button — {@link #start()} already covers the
     *  "discard old results and go fresh" UX this feature calls for — but kept as a
     *  small, clearly-named seam rather than folding "discard" logic only into start(). */
    public static void reset() {
        captureSize = 0;
        startNanos = 0L;
        stopNanos = 0L;
        startCpuPercent = -1.0;
        finalAvgFps = 0f;
        final1LowFps = PercentileLowFps.UNAVAILABLE;
        final01LowFps = PercentileLowFps.UNAVAILABLE;
        finalAvgCpuPercent = -1.0;
        finalDurationNanos = 0L;
        finalSampleCount = 0;
        // Unlike start() above (which deliberately leaves these three alone — see its own
        // comment), reset()'s whole documented purpose is "discard everything and go back
        // to IDLE", so clearing them here is correct, not the same premature-clear bug
        // start() had.
        finalFrametimes = new float[0];
        finalCpuSeries = new float[0];
        finalGpuSeries = new float[0];
        state = State.IDLE;
    }

    // --- Per-frame hook ---

    /** Appends one frametime sample if (and only if) a benchmark is currently RECORDING —
     *  a no-op otherwise, so calling this unconditionally from MineTunerDataHolder's
     *  existing per-frame hook is cheap even when no benchmark is running. Must be called
     *  with the same raw (unsmoothed) per-frame deltaMs value already computed at Feature
     *  1's sampling point in MineTunerDataHolder#recordFrametime — the same value pushed
     *  to rawFrametimeHistory there — not a second, independently-measured timing.
     *
     *  <p>Alongside the frametime, this also records the current
     *  {@link MineTunerDataHolder#cpuPercent} and {@link MineTunerDataHolder#gpuUsagePercent}
     *  for this same frame. Both are read live rather than passed in: by the time a render
     *  frame's recordFrametime() fires, MineTunerDataHolder's own fast/slow metric sampling
     *  (FastMetricsSource/SlowMetricsSource, on SamplingDriver's own cadence — see those
     *  classes) has already kept cpuPercent/gpuUsagePercent continuously fresh, so no second
     *  sampling path is needed here; this simply captures a per-frame snapshot of values
     *  that already exist. GPU usage in particular only truly updates roughly once every
     *  ~1.5s (HardwareSensorPoller's own poll cadence — see its class doc), so several
     *  consecutive captured samples will legitimately repeat the same GPU reading between
     *  polls; that's an accurate reflection of the source data, not a sampling bug. */
    public static void recordFrametimeIfRecording(float deltaMs) {
        if (state != State.RECORDING) return;

        if (captureSize >= MAX_SAMPLES) {
            // Auto-stop cleanly rather than growing the buffers indefinitely — see the
            // class doc's "Cap" section. This still computes a valid final result over
            // the (very long) capped window; it just means "you forgot this was running."
            MineTunerMod.LOGGER.warn(
                    "MineTuner benchmark hit its {} sample cap and was auto-stopped — "
                            + "if this wasn't intentional, remember to press Stop/Freeze "
                            + "when a benchmark run is done.", MAX_SAMPLES);
            stop();
            return;
        }

        if (captureSize == capture.length) {
            int newCap = Math.min(MAX_SAMPLES, capture.length * 2);
            capture = Arrays.copyOf(capture, newCap);
            cpuCapture = Arrays.copyOf(cpuCapture, newCap);
            gpuCapture = Arrays.copyOf(gpuCapture, newCap);
        }
        capture[captureSize] = deltaMs;
        cpuCapture[captureSize] = (float) MineTunerDataHolder.cpuPercent;
        gpuCapture[captureSize] = (float) MineTunerDataHolder.gpuUsagePercent;
        captureSize++;
    }

    // --- Live/state getters, for the GUI ---

    public static State state() {
        return state;
    }

    public static boolean isRecording() {
        return state == State.RECORDING;
    }

    public static boolean hasFrozenResult() {
        return state == State.STOPPED;
    }

    /** Samples captured so far in the active recording, or the frozen run's final count
     *  once stopped. 0 while IDLE. */
    public static int liveSampleCount() {
        return state == State.IDLE ? 0 : captureSize;
    }

    /** Elapsed nanos since Start while RECORDING, frozen duration once STOPPED, or 0
     *  while IDLE. Safe to call every frame the GUI is open — this is a cheap
     *  System.nanoTime() subtraction, not a recomputation of anything derived from the
     *  capture buffer. */
    public static long elapsedNanos() {
        return switch (state) {
            case IDLE -> 0L;
            case RECORDING -> System.nanoTime() - startNanos;
            case STOPPED -> finalDurationNanos;
        };
    }

    // --- Frozen final-result getters, valid once hasFrozenResult() is true ---
    // Each returns a "not available"-style sentinel before a benchmark has ever
    // completed, following the same convention MineTunerDataHolder's own getters use
    // (NaN for percentile lows, -1 for CPU) rather than a misleading 0.

    public static float finalAvgFps() {
        return finalAvgFps;
    }

    public static float final1LowFps() {
        return final1LowFps;
    }

    public static float final01LowFps() {
        return final01LowFps;
    }

    /** -1.0 if CPU% was unavailable for the whole run (see MineTunerDataHolder#cpuPercent's
     *  own -1 sentinel), matching getFormattedCpu()'s N/A convention. */
    public static double finalAvgCpuPercent() {
        return finalAvgCpuPercent;
    }

    public static long finalDurationNanos() {
        return finalDurationNanos;
    }

    public static int finalSampleCount() {
        return finalSampleCount;
    }

    // --- Frozen full-history getters, valid once hasFrozenResult() is true ---
    // Empty arrays before any run has completed (or after reset()), same "empty rather
    // than null" convention MineTunerDataHolder.RingBuffer#snapshot() uses, so the GUI's
    // graphing code never needs a null check before reading length/iterating.

    /** Every raw per-frame frametime (ms) captured this run, oldest-first, exactly
     *  {@link #finalSampleCount()} long. This is the results screen's Frametime graph's
     *  data source, drawn directly in its native ms unit — frametime, not FPS, is the
     *  quantity actually captured per frame, and the graph shows it as such rather than
     *  converting to FPS the way the live section's graph (drawFrametimeGraph, fed from
     *  MineTunerDataHolder's own rolling window, not this method) still does. The headline
     *  Avg FPS / 1% Low / 0.1% Low stats shown alongside the graph are unaffected by this —
     *  see finalAvgFps()/final1LowFps()/final01LowFps(), each already derived from this same
     *  underlying frametime data inside stop(), not from this getter. Returns a fresh copy
     *  each call so callers can't mutate the frozen backing array. */
    public static float[] finalFrametimeSeries() {
        return Arrays.copyOf(finalFrametimes, finalFrametimes.length);
    }

    /** Every per-frame CPU% reading captured this run, oldest-first, exactly
     *  {@link #finalSampleCount()} long. A given entry is -1f where CPU% was unavailable
     *  on that frame (see MineTunerDataHolder#cpuPercent's own sentinel). Returns a fresh
     *  copy each call so callers can't mutate the frozen backing array. */
    public static float[] finalCpuSeries() {
        return Arrays.copyOf(finalCpuSeries, finalCpuSeries.length);
    }

    /** Every per-frame GPU usage% reading captured this run, oldest-first, exactly
     *  {@link #finalSampleCount()} long. A given entry is -1f where GPU usage was
     *  unavailable on that frame (hardware sensors disabled, or LHM unreachable — see
     *  MineTunerDataHolder#gpuUsagePercent's own sentinel). Returns a fresh copy each call
     *  so callers can't mutate the frozen backing array. */
    public static float[] finalGpuSeries() {
        return Arrays.copyOf(finalGpuSeries, finalGpuSeries.length);
    }

    // --- Saving ---

    /** Saves the currently-frozen run to its own timestamped subfolder on disk — see
     *  {@link BenchmarkResultWriter} for the on-disk format and exact save location. A
     *  no-op (returns null) if there's no frozen result to save, e.g. hasFrozenResult() is
     *  false because nothing has completed yet or {@link #reset()} was since called.
     *
     *  @return the path to the created run folder, or null if there was nothing to save
     *          or the save failed (a failure is also logged by BenchmarkResultWriter itself,
     *          so callers don't need to log a second time on a null return). */
    public static Path saveToFolder() {
        if (state != State.STOPPED) return null;
        return BenchmarkResultWriter.write(new BenchmarkResultWriter.Result(
                startNanos, stopNanos, finalDurationNanos, finalSampleCount,
                finalAvgFps, final1LowFps, final01LowFps, finalAvgCpuPercent,
                finalFrametimes, finalCpuSeries, finalGpuSeries));
    }
}
