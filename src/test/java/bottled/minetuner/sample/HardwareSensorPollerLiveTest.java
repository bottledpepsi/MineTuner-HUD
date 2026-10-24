package bottled.minetuner.sample;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end coverage of the LibreHardwareMonitor poller against a real embedded
 * HTTP server: publish, outage reset, and prompt stop. Previously only the pure
 * parsers had any coverage; the thread lifecycle (including the old bug where an
 * interrupt during client.send() was misreported as a network outage) had none.
 */
class HardwareSensorPollerLiveTest {

    @TempDir
    Path dir;

    private HttpServer server;

    @AfterEach
    void tearDown() {
        HardwareSensorPoller.stop();
        MineTunerDataHolder.gpuTempC = -1.0;
        MineTunerDataHolder.gpuClockMhz = -1.0;
        MineTunerDataHolder.gpuUsagePercent = -1.0;
        MineTunerDataHolder.vramUsedMb = -1.0;
        MineTunerDataHolder.vramMaxMb = -1.0;
        MineTunerDataHolder.hardwareSensorsReachable = false;
        MineTunerConfig.setConfigPathSupplier(() -> {
            throw new IllegalStateException("config path not set");
        });
    }

    /** Minimal LibreHardwareMonitor-shaped tree: one GPU node with the four
     *  sensor leaves extractReadings looks for. */
    private static String lhmJson(double tempC, double clockMhz, double loadPct,
                                  double vramUsedMb, double vramMaxMb) {
        return """
                {"Children":[
                  {"Text":"GPU","Children":[
                    {"Text":"GPU Core","SensorId":"/gpu-nvidia/0/temperature/0","Value":"%s °C"},
                    {"Text":"GPU Core","SensorId":"/gpu-nvidia/0/clock/0","Value":"%s MHz"},
                    {"Text":"GPU Core","SensorId":"/gpu-nvidia/0/load/0","Value":"%s %%"},
                    {"Text":"GPU Memory Used","SensorId":"/gpu-nvidia/0/smalldata/0","Value":"%s MB"},
                    {"Text":"GPU Memory Total","SensorId":"/gpu-nvidia/0/smalldata/1","Value":"%s MB"}
                  ]}
                ]}
                """.formatted(tempC, clockMhz, loadPct, vramUsedMb, vramMaxMb);
    }

    private void startServer(String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> responseBody = new AtomicReference<>(body);
        server.createContext("/data.json", exchange -> {
            byte[] bytes = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    private void writeConfig(int port) throws IOException {
        Path file = dir.resolve("minetuner.json");
        Files.writeString(file, """
                {
                  "hardwareSensorsEnabled": true,
                  "hardwareSensorBaseUrl": "http://127.0.0.1:%d",
                  "hardwareSensorPollIntervalMs": 50,
                  "hardwareSensorRequestTimeoutMs": 300
                }
                """.formatted(port));
        MineTunerConfig.setConfigPathSupplier(() -> file);
    }

    private static boolean await(Condition condition) throws InterruptedException {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (condition.met()) return true;
            Thread.sleep(25);
        }
        return condition.met();
    }

    private interface Condition {
        boolean met();
    }

    @Test
    void publishesReadingsFromLiveServerAndResetsOnOutage() throws Exception {
        startServer(lhmJson(45.2, 2100, 37.5, 1024, 8192));
        writeConfig(server.getAddress().getPort());

        HardwareSensorPoller.startIfEnabled();
        assertTrue(await(() -> MineTunerDataHolder.hardwareSensorsReachable),
                "poller should reach the server within the await window");
        assertTrue(await(() -> MineTunerDataHolder.gpuTempC == 45.2
                        && MineTunerDataHolder.gpuClockMhz == 2100.0
                        && MineTunerDataHolder.gpuUsagePercent == 37.5
                        && MineTunerDataHolder.vramUsedMb == 1024.0
                        && MineTunerDataHolder.vramMaxMb == 8192.0),
                "all five sensor fields should publish the served values");

        // Kill the server: the next poll is an outage and must reset every field.
        server.stop(0);
        assertTrue(await(() -> !MineTunerDataHolder.hardwareSensorsReachable
                        && MineTunerDataHolder.gpuTempC == -1.0
                        && MineTunerDataHolder.gpuClockMhz == -1.0
                        && MineTunerDataHolder.gpuUsagePercent == -1.0
                        && MineTunerDataHolder.vramUsedMb == -1.0
                        && MineTunerDataHolder.vramMaxMb == -1.0),
                "outage must reset all published fields to the -1 sentinel");
    }

    @Test
    void stopTerminatesPromptlyEvenMidRequest() throws Exception {
        // A handler that stalls longer than the poll interval, so stop() very
        // likely lands while the poller thread is inside client.send().
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/data.json", exchange -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException ignored) {
            }
            exchange.sendResponseHeaders(500, -1);
        });
        server.start();
        writeConfig(server.getAddress().getPort());

        HardwareSensorPoller.startIfEnabled();
        Thread.sleep(100); // let at least one poll begin
        long start = System.nanoTime();
        HardwareSensorPoller.stop();
        long stopMillis = (System.nanoTime() - start) / 1_000_000;
        // stop() joins the thread; it must return promptly (well under the 2s
        // join ceiling) rather than blocking for the stalled request's duration.
        assertTrue(stopMillis < 2000, "stop() took " + stopMillis + "ms; interrupt during send() should exit promptly");
    }

    @Test
    void disabledConfigNeverStartsThePoller() throws IOException {
        Path file = dir.resolve("minetuner.json");
        Files.writeString(file, """
                {"hardwareSensorsEnabled": false}
                """);
        MineTunerConfig.setConfigPathSupplier(() -> file);
        HardwareSensorPoller.startIfEnabled();
        assertFalse(MineTunerDataHolder.hardwareSensorsReachable);
        HardwareSensorPoller.stop(); // no-op when nothing is running
    }

    // --- readCapped ---

    @Test
    void readCappedReturnsFullBodyUnderLimit() throws IOException {
        String body = "{\"ok\":true}";
        String out = HardwareSensorPoller.readCapped(
                new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)), 1024);
        assertEquals(body, out);
    }

    @Test
    void readCappedReturnsNullOverLimit() throws IOException {
        // The cap is inclusive: exactly maxBytes is accepted, one more is not.
        byte[] exact = new byte[1024];
        java.util.Arrays.fill(exact, (byte) 'a');
        assertEquals(new String(exact, StandardCharsets.UTF_8),
                HardwareSensorPoller.readCapped(new ByteArrayInputStream(exact), 1024));
        byte[] over = new byte[1025];
        java.util.Arrays.fill(over, (byte) 'a');
        assertNull(HardwareSensorPoller.readCapped(new ByteArrayInputStream(over), 1024));
    }
}
