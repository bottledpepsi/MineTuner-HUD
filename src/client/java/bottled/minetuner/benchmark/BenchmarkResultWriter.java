package bottled.minetuner.benchmark;

import bottled.minetuner.MineTunerMod;
import bottled.minetuner.core.BenchmarkReport;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Writes a single completed {@link BenchmarkSession} run to its own folder on disk.
 *
 *  <p>On-disk layout, one folder per run:
 *  <pre>
 *  &lt;game dir&gt;/minetuner/benchmarks/&lt;yyyy-MM-dd_HH-mm-ss&gt;/
 *      summary.txt   — human-readable headline numbers (Avg FPS, 1%/0.1% Low, Avg CPU,
 *                      duration, sample count), the same figures the GUI's results
 *                      section shows.
 *      samples.csv   — every captured sample, one row per frame, oldest-first:
 *                      index, elapsed_ms, fps, cpu_percent, gpu_percent
 *                      (cpu_percent/gpu_percent are blank, not 0, for a frame where that
 *                      reading was unavailable — see BenchmarkSession's own -1 sentinel
 *                      convention on cpuCapture/gpuCapture; a blank CSV cell reads
 *                      correctly as "no data" in a spreadsheet, whereas a literal 0 would
 *                      misleadingly plot as "0% usage").
 *  </pre>
 *
 *  <p>Saved under the mod's own subfolder of the instance's game directory (parallel to,
 *  but distinct from, {@code config/} — this is per-run output data, not a setting), the
 *  same {@link FabricLoader}-resolved-path approach {@code MineTunerConfig}'s config path
 *  already uses for its own file, and written with the same tmp-file-then-atomic-move
 *  pattern as {@code MineTunerConfig#save()} so a crash or an out-of-space disk mid-write
 *  can never leave a half-written file behind under the final name. */
final class BenchmarkResultWriter {

    private static final DateTimeFormatter FOLDER_NAME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private BenchmarkResultWriter() {
    }

    private static void writeSummary(BufferedWriter w, Result r) throws IOException {
        String started = LocalDateTime.ofInstant(r.startedAt(), ZoneId.systemDefault()).format(FOLDER_NAME_FORMAT);
        BenchmarkReport.writeSummary(w, started, r.durationNanos(), r.sampleCount(), r.avgFps(),
                r.low1Fps(), r.low01Fps(), r.avgCpuPercent(), r.gpuPercentSeries());
    }

    /** Creates the run's timestamped subfolder and writes summary.txt + samples.csv into
     *  it. Returns the created folder's path on success, or null if anything went wrong
     *  (already logged here, so callers don't need to log a second time). */
    static Path write(Result result) {
        Path benchmarksRoot = FabricLoader.getInstance().getGameDir()
                .resolve("minetuner").resolve("benchmarks");

        String folderName = LocalDateTime.now().format(FOLDER_NAME_FORMAT);
        Path runDir = benchmarksRoot.resolve(folderName);
        // Extremely unlikely (would need two runs saved within the same second), but
        // cheap to guard: rather than silently overwriting an existing folder's files,
        // suffix with -2, -3, ... until a free name is found.
        int suffix = 2;
        while (Files.exists(runDir)) {
            runDir = benchmarksRoot.resolve(folderName + "-" + suffix);
            suffix++;
        }

        try {
            Files.createDirectories(runDir);
        } catch (IOException e) {
            MineTunerMod.LOGGER.error(
                    "Failed to create benchmark results folder {}: {}", runDir, e.getMessage());
            return null;
        }

        boolean ok = writeAtomically(runDir.resolve("summary.txt"), w -> writeSummary(w, result))
                & writeAtomically(runDir.resolve("samples.csv"), w -> writeCsv(w, result));

        if (!ok) return null;

        MineTunerMod.LOGGER.info("Saved benchmark results to {}", runDir);
        return runDir;
    }

    @FunctionalInterface
    private interface WriterAction {
        void write(BufferedWriter w) throws IOException;
    }

    /** Same tmp-file-then-{@link Files#move} pattern as {@code MineTunerConfig#save()}:
     *  write to a sibling {@code .tmp} file first, then atomically (falling back to a
     *  plain replace if the filesystem doesn't support an atomic move) rename it into
     *  place, so a failure partway through a write can never leave a truncated/corrupt
     *  file sitting under the real target name. */
    private static boolean writeAtomically(Path target, WriterAction action) {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            try (BufferedWriter w = Files.newBufferedWriter(tmp)) {
                action.write(w);
            }
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            MineTunerMod.LOGGER.error("Failed to write {}: {}", target, e.getMessage());
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
            }
            return false;
        }
    }

    private static void writeCsv(BufferedWriter w, Result r) throws IOException {
        BenchmarkReport.writeCsv(w, r.frametimesMs(), r.cpuPercentSeries(), r.gpuPercentSeries(), r.sampleCount());
    }

    /** Immutable snapshot of exactly the fields a completed run needs to write out —
     *  passed in by {@link BenchmarkSession#saveToFolderAsync()} rather than this class reading
     *  BenchmarkSession's static state directly, so this writer stays independently
     *  testable/reusable and has no dependency on BenchmarkSession's own lifecycle/locking.
     *  (The monotonic startNanos/stopNanos the record used to carry were never read by
     *  the writer — the summary's "Started" line comes from startedAt — so they're
     *  dropped rather than carried dead.) */
    record Result(long durationNanos, int sampleCount,
                  float avgFps, float low1Fps, float low01Fps, double avgCpuPercent,
                  float[] frametimesMs, float[] cpuPercentSeries, float[] gpuPercentSeries,
                  Instant startedAt, Instant stoppedAt) {
    }
}
