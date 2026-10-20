package bottled.minetuner.core;

import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BenchmarkReportTest {

    // ---- The original implementation, retained verbatim as the golden reference ----

    private static void oldWriteCsv(BufferedWriter w, float[] frametimes, float[] cpu, float[] gpu, int sampleCount) throws IOException {
        w.write("index,elapsed_ms,fps,cpu_percent,gpu_percent");
        w.newLine();
        double elapsedMs = 0.0;
        for (int i = 0; i < sampleCount; i++) {
            float deltaMs = frametimes[i];
            float fps = deltaMs > 0f ? 1000f / deltaMs : 0f;
            String cpuCell = cpu[i] >= 0f ? String.format(Locale.ROOT, "%.1f", cpu[i]) : "";
            String gpuCell = gpu[i] >= 0f ? String.format(Locale.ROOT, "%.1f", gpu[i]) : "";
            w.write(i + "," + String.format(Locale.ROOT, "%.1f", elapsedMs) + ","
                    + String.format(Locale.ROOT, "%.2f", fps) + "," + cpuCell + "," + gpuCell);
            w.newLine();
            elapsedMs += deltaMs;
        }
    }

    private static void oldWriteSummary(BufferedWriter w, String started, long durationNanos, int sampleCount,
                                        float avgFps, float low1Fps, float low01Fps, double avgCpuPercent,
                                        float[] gpu) throws IOException {
        w.write("MineTuner Benchmark Result");
        w.newLine();
        w.write("Started:   " + started);
        w.newLine();
        w.write("Duration:  " + oldFormatDuration(durationNanos));
        w.newLine();
        w.write("Samples:   " + sampleCount);
        w.newLine();
        w.newLine();
        w.write(String.format(Locale.ROOT, "Avg FPS:     %.2f", avgFps));
        w.newLine();
        w.write("1% Low:      " + oldPct(low1Fps));
        w.newLine();
        w.write("0.1% Low:    " + oldPct(low01Fps));
        w.newLine();
        w.write("Avg CPU:     " + (avgCpuPercent >= 0 ? String.format(Locale.ROOT, "%.1f%%", avgCpuPercent) : "N/A"));
        w.newLine();
        double gpuSum = 0.0;
        int gpuCount = 0;
        for (float v : gpu) {
            if (v >= 0f) {
                gpuSum += v;
                gpuCount++;
            }
        }
        w.write("Avg GPU:     " + (gpuCount > 0 ? String.format(Locale.ROOT, "%.1f%%", gpuSum / gpuCount) : "N/A"));
        w.newLine();
    }

    private static String oldFormatDuration(long nanos) {
        long totalMs = nanos / 1_000_000L;
        long totalSeconds = totalMs / 1000L;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long tenths = (totalMs % 1000L) / 100L;
        return minutes > 0
                ? String.format(Locale.ROOT, "%d:%02d.%d", minutes, seconds, tenths)
                : String.format(Locale.ROOT, "%d.%ds", seconds, tenths);
    }

    private static String oldPct(float value) {
        return Float.isNaN(value) ? "N/A" : String.format(Locale.ROOT, "%.2f", value);
    }

    // ---- Tests ----

    @Test
    void csvIsByteIdenticalToOldImplementation() throws IOException {
        java.util.Random rnd = new java.util.Random(99);
        int n = 20_000;
        float[] ft = new float[n], cpu = new float[n], gpu = new float[n];
        for (int i = 0; i < n; i++) {
            int kind = rnd.nextInt(20);
            ft[i] = kind == 0 ? 16.7f : kind == 1 ? 250f + rnd.nextFloat() * 700f : 1f + rnd.nextFloat() * 30f;
            cpu[i] = rnd.nextInt(5) == 0 ? -1f : rnd.nextFloat() * 100f;
            gpu[i] = rnd.nextInt(2) == 0 ? -1f : Math.round(rnd.nextFloat() * 1000f) / 10f; // lots of x.x values
        }
        ft[5] = 0f; // the defensive zero-frametime branch

        StringWriter expected = new StringWriter();
        try (BufferedWriter bw = new BufferedWriter(expected)) {
            oldWriteCsv(bw, ft, cpu, gpu, n);
        }
        StringWriter actual = new StringWriter();
        BenchmarkReport.writeCsv(actual, ft, cpu, gpu, n);
        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    void csvKeepsBlankCellsForUnavailableReadings() throws IOException {
        StringWriter out = new StringWriter();
        BenchmarkReport.writeCsv(out, new float[]{10f, 20f}, new float[]{-1f, 50f}, new float[]{-1f, -1f}, 2);
        String nl = System.lineSeparator();
        assertEquals("index,elapsed_ms,fps,cpu_percent,gpu_percent" + nl
                + "0,0.0,100.00,," + nl
                + "1,10.0,50.00,50.0," + nl, out.toString());
    }

    @Test
    void summaryMatchesOldImplementationExceptCpuSuffix() throws IOException {
        float[] gpu = {-1f, 40f, 60f};
        for (double cpu : new double[]{-1.0, 23.456}) {
            StringWriter expected = new StringWriter();
            try (BufferedWriter bw = new BufferedWriter(expected)) {
                oldWriteSummary(bw, "2026-01-01_10-00-00", 125_400_000_000L, 321, 143.256f,
                        Float.NaN, 88.5f, cpu, gpu);
            }
            StringWriter actual = new StringWriter();
            BenchmarkReport.writeSummary(actual, "2026-01-01_10-00-00", 125_400_000_000L, 321, 143.256f,
                    Float.NaN, 88.5f, cpu, gpu);
            String exp = expected.toString();
            if (cpu >= 0) exp = exp.replace("23.5%", "23.5% (JVM process load)");
            assertEquals(exp, actual.toString());
        }
    }

    @Test
    void fastFormatMatchesStringFormat() {
        java.util.Random rnd = new java.util.Random(5);
        StringBuilder sb = new StringBuilder();
        for (int dec = 0; dec <= 3; dec++) {
            String fmt = "%." + dec + "f";
            for (int i = 0; i < 400_000; i++) {
                double v;
                switch (i % 5) {
                    case 0 -> v = rnd.nextDouble() * 100;
                    case 1 -> v = rnd.nextFloat() * 1000f;                       // float widened
                    case 2 -> v = (rnd.nextInt(2_000_000) + 0.5) / Math.pow(10, dec); // exact-looking ties
                    case 3 -> v = rnd.nextInt(10_000) / 100.0 + 0.005;           // x.xx5 style ties
                    default -> v = rnd.nextDouble() * 1e6;
                }
                sb.setLength(0);
                FastFormat.appendFixed(sb, v, dec);
                assertEquals(String.format(Locale.ROOT, fmt, v), sb.toString(), "v=" + v + " dec=" + dec);
            }
        }
    }

    @Test
    void fastFormatEdgeValues() {
        double[] vals = {0.0, 0.05, 0.95, 9.995, 99.5, 0.5, 1.5, 2.5, 1e8, 123456789.0, 0.0001};
        StringBuilder sb = new StringBuilder();
        for (double v : vals) {
            for (int dec = 0; dec <= 2; dec++) {
                sb.setLength(0);
                FastFormat.appendFixed(sb, v, dec);
                assertEquals(String.format(Locale.ROOT, "%." + dec + "f", v), sb.toString(), v + "/" + dec);
            }
        }
    }
}
