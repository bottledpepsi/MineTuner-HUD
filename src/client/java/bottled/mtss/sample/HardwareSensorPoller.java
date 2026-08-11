package bottled.mtss.sample;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.MtssMod;
import bottled.mtss.config.MtssConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/** Optional, opt-in bridge to LibreHardwareMonitor's (LHM) built-in "Remote Web
 *  Server" plugin (default {@code http://localhost:8085/data.json}), which exposes
 *  live GPU/VRAM sensor readings as JSON. Polls on its own daemon thread so a slow
 *  or hung request never blocks the render thread. */
public final class HardwareSensorPoller {

    /** Conservative default cadence for hardware sensors. */
    private static final long POLL_INTERVAL_MS_DEFAULT = 1500;
    /** Conservative default per-request timeout. */
    private static final Duration REQUEST_TIMEOUT_DEFAULT = Duration.ofMillis(300);

    private static volatile HardwareSensorPoller RUNNING;

    private final Thread thread;
    private final String baseUrl;
    /** Snapshot of the request timeout in effect when this poller (and its
     *  underlying HttpClient) was constructed — see {@link #reconcileWithConfig}
     *  for why a config change to the timeout requires a fresh poller instance
     *  rather than being read live like the poll interval is. */
    private final HttpClient client;
    private volatile boolean stopRequested = false;
    private boolean warnedThisOutage = false;

    private HardwareSensorPoller(String baseUrl, Duration connectTimeout) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        this.thread = new Thread(this::runLoop, "mtss-hardware-sensor-poller");
        this.thread.setDaemon(true); // never keep the JVM alive on its own.
    }

    /** Starts the poller if hardware sensors are enabled in config and it isn't
     *  already running (a no-op call if either condition isn't met, so callers
     *  don't need to check state themselves before calling this). */
    public static synchronized void startIfEnabled() {
        MtssConfig cfg = MtssConfig.getInstance();
        if (!cfg.hardwareSensorsEnabled) return;
        if (RUNNING != null) return;
        Duration connectTimeout = cfg.hardwareSensorRequestTimeoutMs > 0
                ? Duration.ofMillis(cfg.hardwareSensorRequestTimeoutMs)
                : REQUEST_TIMEOUT_DEFAULT;
        HardwareSensorPoller poller = new HardwareSensorPoller(
                normalizeBaseUrl(cfg.hardwareSensorBaseUrl), connectTimeout);
        RUNNING = poller;
        poller.thread.start();
    }

    /** Stops the background thread, if running. */
    public static synchronized void stop() {
        if (RUNNING == null) return;
        RUNNING.stopRequested = true;
        RUNNING.thread.interrupt();
        RUNNING = null;
    }

    /** Call after saving a config change to {@code hardwareSensorsEnabled},
     *  {@code hardwareSensorBaseUrl}, or {@code hardwareSensorRequestTimeoutMs} —
     *  starts, stops, or restarts the poller as needed to pick up the new settings. */
    public static synchronized void reconcileWithConfig() {
        MtssConfig cfg = MtssConfig.getInstance();
        if (!cfg.hardwareSensorsEnabled) {
            stop();
            resetPublishedValues();
            return;
        }
        String normalized = normalizeBaseUrl(cfg.hardwareSensorBaseUrl);
        if (RUNNING == null) {
            startIfEnabled();
        } else if (!RUNNING.baseUrl.equals(normalized)
                || RUNNING.client.connectTimeout().map(t -> t.toMillis() != cfg.hardwareSensorRequestTimeoutMs).orElse(true)) {
            // Either the base URL moved, or the connect timeout baked into
            // the running poller's HttpClient no longer matches config. The
            // simplest correct fix for the latter is a clean restart rather
            // than trying to swap the HttpClient out from under the thread.
            stop();
            startIfEnabled();
        }
    }

    private static String normalizeBaseUrl(String raw) {
        String url = (raw == null || raw.isBlank()) ? "http://localhost:8085" : raw.trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Resets every published sensor field back to "unavailable". */
    private static void resetPublishedValues() {
        MtssDataHolder.gpuTempC = -1.0;
        MtssDataHolder.gpuClockMhz = -1.0;
        MtssDataHolder.gpuUsagePercent = -1.0;
        MtssDataHolder.vramUsedMb = -1.0;
        MtssDataHolder.vramMaxMb = -1.0;
        MtssDataHolder.hardwareSensorsReachable = false;
    }

    private static String getString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && el.isJsonPrimitive()) ? el.getAsString() : null;
    }

    /** SensorId looks like { /gpu-nvidia/0/temperature/0}. */
    private static String sensorTypeFromId(String sensorId) {
        if (sensorId == null || sensorId.isEmpty()) return null;
        String[] parts = sensorId.split("/");
        // Expect at least ./<type>/<index>.
        if (parts.length < 2) return null;
        return parts[parts.length - 2];
    }

    /** LHM formats Value as e.g. "45.2 °C" or "1234 MHz" — a leading number (optionally
     *  with a comma as the decimal separator, per some locales) followed by a unit
     *  suffix this method ignores, returning just the numeric part. */
    private static Double parseLeadingNumber(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        int i = 0;
        int len = trimmed.length();
        StringBuilder num = new StringBuilder();
        if (i < len && (trimmed.charAt(i) == '-' || trimmed.charAt(i) == '+')) {
            num.append(trimmed.charAt(i));
            i++;
        }
        boolean sawDigit = false;
        while (i < len && (Character.isDigit(trimmed.charAt(i)) || trimmed.charAt(i) == '.' || trimmed.charAt(i) == ',')) {
            char c = trimmed.charAt(i);
            if (Character.isDigit(c)) sawDigit = true;
            // Normalize a comma decimal separator to '.'; a comma as a thousands
            // separator isn't a concern here since LHM's sensor values are always
            // small (temperatures, clock speeds, percentages) and never need grouping.
            num.append(c == ',' ? '.' : c);
            i++;
        }
        if (!sawDigit) return null;
        try {
            return Double.parseDouble(num.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void runLoop() {
        while (!stopRequested) {
            try {
                pollOnce();
            } catch (Throwable t) {
                // Belt-and-suspenders: pollOnce() already catches every failure mode
                // it knows how to (see its own catch blocks above), but this loop must
                // survive literally anything — including an Error subtype, or a bug in
                // code pollOnce() doesn't wrap in its own try — so one bad response
                // never silently kills the thread and leaves stats stuck at their
                // last value forever.
                logOutageOnce("unexpected error: " + t);
                markUnavailable();
            }
            try {
                // Read live every cycle (not cached at thread-start) so a
                // Cloth Config change to hardwareSensorPollIntervalMs takes
                // effect on the very next sleep, no poller restart needed —
                // unlike the request timeout below, which is baked into the
                // HttpClient at construction (see reconcileWithConfig).
                long intervalMs = MtssConfig.getInstance().hardwareSensorPollIntervalMs;
                Thread.sleep(intervalMs > 0 ? intervalMs : POLL_INTERVAL_MS_DEFAULT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return; // stop() interrupts us to exit promptly.
            }
        }
    }

    private void pollOnce() {
        HttpRequest request;
        Duration requestTimeout = Duration.ofMillis(
                Math.max(50, MtssConfig.getInstance().hardwareSensorRequestTimeoutMs));
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/data.json"))
                    .timeout(requestTimeout)
                    .GET()
                    .build();
        } catch (IllegalArgumentException badUrl) {
            // Malformed base URL from the config file. Caught separately (rather
            // than falling through to the generic network-failure catch below) so
            // this specific outage gets a "invalid base URL" message instead of a
            // vague "could not reach" one, even though the end result — logging
            // the outage once and marking sensors unavailable — is the same either way.
            logOutageOnce("invalid base URL \"" + baseUrl + "\": " + badUrl.getMessage());
            markUnavailable();
            return;
        }

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception networkFailure) {
            // Covers connection refused (LHM not running / remote web server
            // off), timeout (hung/slow), unknown host, and anything else
            // java.net.http can throw for a failed request.
            logOutageOnce("could not reach " + baseUrl + ": " + networkFailure);
            markUnavailable();
            return;
        }

        if (response.statusCode() != 200) {
            logOutageOnce("unexpected HTTP " + response.statusCode() + " from " + baseUrl);
            markUnavailable();
            return;
        }

        JsonObject root;
        try {
            root = JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception parseFailure) {
            // Malformed/unexpected JSON shape — e.g. LHM returned an HTML error
            // page instead of JSON, or a future LHM version has since
            // changed the tree format.
            logOutageOnce("could not parse data.json: " + parseFailure);
            markUnavailable();
            return;
        }

        SensorReadings readings = extractReadings(root);
        MtssDataHolder.gpuTempC = readings.tempC;
        MtssDataHolder.gpuClockMhz = readings.clockMhz;
        MtssDataHolder.gpuUsagePercent = readings.usagePercent;
        MtssDataHolder.vramUsedMb = readings.vramUsedMb;
        MtssDataHolder.vramMaxMb = readings.vramMaxMb;
        MtssDataHolder.hardwareSensorsReachable = true;
        warnedThisOutage = false; // recovered.
    }

    private void markUnavailable() {
        MtssDataHolder.gpuTempC = -1.0;
        MtssDataHolder.gpuClockMhz = -1.0;
        MtssDataHolder.gpuUsagePercent = -1.0;
        MtssDataHolder.vramUsedMb = -1.0;
        MtssDataHolder.vramMaxMb = -1.0;
        MtssDataHolder.hardwareSensorsReachable = false;
    }

    /** Logs only the first failure of a new outage, not every 1.5s. */
    private void logOutageOnce(String message) {
        if (warnedThisOutage) return;
        warnedThisOutage = true;
        MtssMod.LOGGER.warn("Hardware sensor poller: {} — GPU/VRAM stats will be unavailable "
                + "until this recovers.", message);
    }

    /** LHM's tree is a node with a "Children" array, recursively, down to leaf. */
    private SensorReadings extractReadings(JsonObject root) {
        LeafSensor temp = findFirstSensor(root, "temperature", "gpu core");
        LeafSensor clock = findFirstSensor(root, "clock", "gpu core");
        LeafSensor usage = findFirstSensor(root, "load", "gpu core");
        LeafSensor vramUsed = findFirstSensor(root, "smalldata", "gpu memory used");
        LeafSensor vramMax = findFirstSensor(root, "smalldata", "gpu memory total");

        return new SensorReadings(
                temp != null ? temp.value : -1,
                clock != null ? clock.value : -1,
                usage != null ? usage.value : -1,
                vramUsed != null ? vramUsed.value : -1,
                vramMax != null ? vramMax.value : -1
        );
    }

    /** Breadth-first walk of the tree (iterative, not recursive — LHM's node tree
     *  depth isn't bounded by anything MTSS controls, so recursion here could risk a
     *  stack overflow on a deep tree; the explicit ArrayDeque queue below has no such
     *  limit, and MAX_NODES caps total work either way). Returns the first leaf sensor
     *  whose SensorId type matches sensorTypeLower and whose Text contains
     *  nameContainsLower (case-insensitive), or null if none match within the cap. */
    private LeafSensor findFirstSensor(JsonObject root, String sensorTypeLower, String nameContainsLower) {
        Deque<JsonObject> queue = new ArrayDeque<>();
        queue.add(root);
        int visited = 0;
        // Hard cap so a pathological/huge tree can't turn a single poll into.
        // an unbounded scan on the background thread.
        final int MAX_NODES = 20_000;

        while (!queue.isEmpty() && visited < MAX_NODES) {
            JsonObject node = queue.poll();
            visited++;

            JsonElement sensorIdEl = node.get("SensorId");
            if (sensorIdEl != null && sensorIdEl.isJsonPrimitive()) {
                String sensorId = sensorIdEl.getAsString();
                String type = sensorTypeFromId(sensorId);
                if (type != null && type.equalsIgnoreCase(sensorTypeLower)) {
                    String text = getString(node, "Text");
                    if (text != null && text.toLowerCase(Locale.ROOT).contains(nameContainsLower)) {
                        Double value = parseLeadingNumber(getString(node, "Value"));
                        if (value != null) {
                            return new LeafSensor(value);
                        }
                    }
                }
            }

            JsonElement childrenEl = node.get("Children");
            if (childrenEl != null && childrenEl.isJsonArray()) {
                JsonArray children = childrenEl.getAsJsonArray();
                for (JsonElement child : children) {
                    if (child.isJsonObject()) queue.add(child.getAsJsonObject());
                }
            }
        }
        return null;
    }

    private record SensorReadings(double tempC, double clockMhz, double usagePercent, double vramUsedMb,
                                  double vramMaxMb) {
    }

    private record LeafSensor(double value) {
    }
}
