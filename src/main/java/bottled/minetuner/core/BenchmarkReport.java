package bottled.minetuner.core;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

/**
 * Text rendering of a finished benchmark run (summary.txt and samples.csv). Pure and
 * Minecraft-free so the exact output can be unit-tested.
 */
public final class BenchmarkReport {

    private static final int FLUSH_AT = 16 * 1024;

    private BenchmarkReport() {
    }

    /**
     * samples.csv: index, elapsed_ms, fps, cpu_percent, gpu_percent. A cpu/gpu cell is blank
     * (not 0) when that frame's reading was unavailable (negative sentinel).
     */
    public static void writeCsv(Writer w, float[] frametimesMs, float[] cpuPercent,
                                float[] gpuPercent, int sampleCount) throws IOException {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder(FLUSH_AT + 256);
        sb.append("index,elapsed_ms,fps,cpu_percent,gpu_percent").append(nl);

        double elapsedMs = 0.0;
        for (int i = 0; i < sampleCount; i++) {
            float deltaMs = frametimesMs[i];
            float fps = deltaMs > 0f ? 1000f / deltaMs : 0f;

            sb.append(i).append(',');
            FastFormat.appendFixed(sb, elapsedMs, 1);
            sb.append(',');
            FastFormat.appendFixed(sb, fps, 2);
            sb.append(',');
            if (cpuPercent[i] >= 0f) FastFormat.appendFixed(sb, cpuPercent[i], 1);
            sb.append(',');
            if (gpuPercent[i] >= 0f) FastFormat.appendFixed(sb, gpuPercent[i], 1);
            sb.append(nl);

            elapsedMs += deltaMs;
            if (sb.length() >= FLUSH_AT) {
                w.append(sb);
                sb.setLength(0);
            }
        }
        w.append(sb);
    }

    /**
     * summary.txt. {@code started} is the already-formatted wall-clock start time. Avg CPU is
     * the JVM process load (not whole-system CPU), and the line says so.
     */
    public static void writeSummary(Writer w, String started, long durationNanos, int sampleCount,
                                    float avgFps, float low1Fps, float low01Fps,
                                    double avgCpuPercent, float[] gpuPercent) throws IOException {
        String nl = System.lineSeparator();
        w.write("MineTuner Benchmark Result" + nl);
        w.write("Started:   " + started + nl);
        w.write("Duration:  " + formatDuration(durationNanos) + nl);
        w.write("Samples:   " + sampleCount + nl);
        w.write(nl);
        w.write(String.format(Locale.ROOT, "Avg FPS:     %.2f", avgFps) + nl);
        w.write("1% Low:      " + formatPercentile(low1Fps) + nl);
        w.write("0.1% Low:    " + formatPercentile(low01Fps) + nl);
        w.write("Avg CPU:     " + (avgCpuPercent >= 0
                ? String.format(Locale.ROOT, "%.1f%%", avgCpuPercent) + " (JVM process load)" : "N/A") + nl);

        // Avg GPU is derived from the per-sample series, skipping unavailable samples so they
        // cannot drag the average toward 0.
        double gpuSum = 0.0;
        int gpuCount = 0;
        for (float v : gpuPercent) {
            if (v >= 0f) {
                gpuSum += v;
                gpuCount++;
            }
        }
        w.write("Avg GPU:     " + (gpuCount > 0
                ? String.format(Locale.ROOT, "%.1f%%", gpuSum / gpuCount) : "N/A") + nl);
    }

    public static String formatDuration(long nanos) {
        long totalMs = nanos / 1_000_000L;
        long totalSeconds = totalMs / 1000L;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long tenths = (totalMs % 1000L) / 100L;
        return minutes > 0
                ? String.format(Locale.ROOT, "%d:%02d.%d", minutes, seconds, tenths)
                : String.format(Locale.ROOT, "%d.%ds", seconds, tenths);
    }

    private static String formatPercentile(float value) {
        return Float.isNaN(value) ? "N/A" : String.format(Locale.ROOT, "%.2f", value);
    }
}
