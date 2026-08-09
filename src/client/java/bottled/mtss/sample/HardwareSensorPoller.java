package bottled.mtss.sample;

import bottled.mtss.MtssDataHolder;
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

/**
 * Optional, opt-in bridge to LibreHardwareMonitor's (LHM) built-in "Remote
 * Web Server" — a plain loopback HTTP JSON endpoint LHM can serve once
 * enabled in its own UI (Options -> Remote Web Server -> Run), at
 * {@code http://localhost:8085/data.json} by default. This is MTSS's only
 * source of hardware sensor telemetry (GPU temp/clock/usage, VRAM) — every
 * other stat this mod shows is readable straight from the JVM/game state.
 * <p>
 * <b>Why this needs its own thread instead of a {@link StatSource}:</b>
 * {@link SamplingDriver#sampleAll()} runs synchronously on the render
 * thread every frame, including for {@link Cadence#THROTTLED} sources — it
 * just skips most frames, it doesn't hop threads. An HTTP round-trip is far
 * slower and less predictable than the in-process MXBean reads the existing
 * THROTTLED sources do, so running it there would risk a frame hitch (or
 * worse, a hitch that compounds if LHM is slow/hung and requests start
 * backing up) every time the 500ms gate opens. This class instead runs a
 * single dedicated daemon thread with its own sleep loop, and the render
 * thread only ever reads the {@code volatile} fields
 * {@link MtssDataHolder} publishes them into — never blocks, never waits on
 * the network. Noted comment-only in {@code SourceRegistry}'s static block
 * for discoverability, the same way server TPS (also event-pushed, from a
 * Mixin) is — there's no dedicated {@code StatSource} class for either,
 * since {@code sample()} would never actually be invoked for either one.
 * <p>
 * <b>Degradation, not failure:</b> LHM not installed, not running, remote
 * web server not enabled, wrong port, a firewall block, or a JSON shape
 * that doesn't parse the way this class expects — every one of these is
 * treated identically: the affected/all sensor value(s) fall back to -1
 * ("unavailable"), {@link MtssDataHolder}'s formatted accessors return ""
 * (or history stops accumulating), and the poller quietly keeps trying on
 * its normal cadence so it recovers on its own the moment LHM becomes
 * reachable. Nothing here ever throws out of {@link #pollOnce()} — every
 * failure path is caught and swallowed (with a one-time log line so a
 * persistently broken setup is still debuggable without spamming the log
 * every 1-2 seconds).
 */
public final class HardwareSensorPoller {

    /** Conservative cadence for hardware sensors: they don't need frame-accurate freshness like FPS/TPS do, and this keeps the extra background HTTP traffic light. */
    private static final long POLL_INTERVAL_MS = 1500;
    /** Loopback request to an already-running local process — a couple hundred ms is generous, and keeps a hung/slow LHM instance from ever building a backlog of in-flight requests. */
    private static final Duration REQUEST_TIMEOUT = Duration.ofMillis(300);

    private static volatile HardwareSensorPoller RUNNING;

    private final Thread thread;
    private volatile boolean stopRequested = false;
    private volatile String baseUrl;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();

    private boolean warnedThisOutage = false;

    private HardwareSensorPoller(String baseUrl) {
        this.baseUrl = baseUrl;
        this.thread = new Thread(this::runLoop, "mtss-hardware-sensor-poller");
        this.thread.setDaemon(true); // never keep the JVM alive on its own
    }

    /**
     * Starts the poller if hardware sensors are enabled in config and it
     * isn't already running. Safe to call repeatedly (e.g. every client
     * init) — a no-op if already started. Must not be called at all when
     * the feature is disabled; callers (see {@code MtssClient}) check
     * {@link MtssConfig#hardwareSensorsEnabled} first so the thread is
     * never even created for a user who hasn't opted in.
     */
    public static synchronized void startIfEnabled() {
        MtssConfig cfg = MtssConfig.getInstance();
        if (!cfg.hardwareSensorsEnabled) return;
        if (RUNNING != null) return;
        HardwareSensorPoller poller = new HardwareSensorPoller(normalizeBaseUrl(cfg.hardwareSensorBaseUrl));
        RUNNING = poller;
        poller.thread.start();
    }

    /** Stops the background thread, if running. Safe to call even if it was never started. */
    public static synchronized void stop() {
        if (RUNNING == null) return;
        RUNNING.stopRequested = true;
        RUNNING.thread.interrupt();
        RUNNING = null;
    }

    /**
     * Call after saving a config change to {@code hardwareSensorsEnabled}
     * or {@code hardwareSensorBaseUrl} so the poller starts/stops/re-points
     * without requiring a game restart. Cheap to call unconditionally.
     */
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
        } else if (!RUNNING.baseUrl.equals(normalized)) {
            RUNNING.baseUrl = normalized;
        }
    }

    private static String normalizeBaseUrl(String raw) {
        String url = (raw == null || raw.isBlank()) ? "http://localhost:8085" : raw.trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Resets every published sensor field back to "unavailable" — used when the feature is turned off so a stale reading can't linger and be mistaken for live data. */
    private static void resetPublishedValues() {
        MtssDataHolder.gpuTempC = -1.0;
        MtssDataHolder.gpuClockMhz = -1.0;
        MtssDataHolder.gpuUsagePercent = -1.0;
        MtssDataHolder.vramUsedMb = -1.0;
        MtssDataHolder.vramMaxMb = -1.0;
        MtssDataHolder.hardwareSensorsReachable = false;
    }

    private void runLoop() {
        while (!stopRequested) {
            try {
                pollOnce();
            } catch (Throwable t) {
                // Belt-and-suspenders: pollOnce() already catches everything
                // it knows how to, but this loop must survive literally
                // anything so one bad response never silently kills the
                // thread and leaves stats stuck at their last value forever.
                logOutageOnce("unexpected error: " + t);
                markUnavailable();
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return; // stop() interrupts us to exit promptly
            }
        }
    }

    private void pollOnce() {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/data.json"))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
        } catch (IllegalArgumentException badUrl) {
            // Malformed base URL from the config file — treat exactly like
            // any other unreachable case rather than crashing the thread.
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
            // Malformed/unexpected JSON shape — e.g. an LHM version that
            // changed the tree format. Degrade, don't crash.
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
        warnedThisOutage = false; // recovered — next failure logs again
    }

    private void markUnavailable() {
        MtssDataHolder.gpuTempC = -1.0;
        MtssDataHolder.gpuClockMhz = -1.0;
        MtssDataHolder.gpuUsagePercent = -1.0;
        MtssDataHolder.vramUsedMb = -1.0;
        MtssDataHolder.vramMaxMb = -1.0;
        MtssDataHolder.hardwareSensorsReachable = false;
    }

    /** Logs only the first failure of a new outage, not every 1.5s — a persistently unreachable LHM shouldn't spam the log, same rationale as SamplingDriver's per-source warn-once set. */
    private void logOutageOnce(String message) {
        if (warnedThisOutage) return;
        warnedThisOutage = true;
        System.err.println("[MTSS] Hardware sensor poller: " + message
                + " — GPU/VRAM stats will be unavailable until this recovers.");
    }

    // ── data.json tree walk ──────────────────────────────────────────────────

    private record SensorReadings(double tempC, double clockMhz, double usagePercent, double vramUsedMb, double vramMaxMb) {}

    /**
     * LHM's tree is a node with a "Children" array, recursively, down to
     * leaf sensor nodes carrying "Text" (label), "Value"/"Min"/"Max"
     * (formatted strings, e.g. "62.3 °C"), and "SensorId" (a path like
     * {@code /gpu-nvidia/0/temperature/0} that also encodes the
     * {@code SensorType}, e.g. "Temperature"). Sensor identity is
     * system-dependent (multi-GPU rigs, vendor-specific labels), so this
     * matches by {@code SensorType} (parsed out of SensorId, since that's
     * the one consistently-present structural field — the JSON has no
     * separate "SensorType" key of its own) plus a name substring, rather
     * than a single fixed path. First match per category wins;
     * multi-GPU selection is out of scope for this pass (see the design
     * doc's Constraints section).
     */
    private SensorReadings extractReadings(JsonObject root) {
        LeafSensor temp   = findFirstSensor(root, "temperature", "gpu core");
        LeafSensor clock  = findFirstSensor(root, "clock", "gpu core");
        LeafSensor usage  = findFirstSensor(root, "load", "gpu core");
        LeafSensor vramUsed = findFirstSensor(root, "smalldata", "gpu memory used");
        LeafSensor vramMax  = findFirstSensor(root, "smalldata", "gpu memory total");

        return new SensorReadings(
                temp != null ? temp.value : -1,
                clock != null ? clock.value : -1,
                usage != null ? usage.value : -1,
                vramUsed != null ? vramUsed.value : -1,
                vramMax != null ? vramMax.value : -1
        );
    }

    private record LeafSensor(double value) {}

    /**
     * Breadth-first walk of the tree (iterative, not recursive — the tree
     * is shallow in practice, but an explicit queue avoids any risk of deep
     * recursion on a malformed/cyclical-looking payload) looking for the
     * first leaf sensor whose parsed SensorId type matches
     * {@code sensorTypeLower} (case-insensitive) and whose "Text" label
     * contains {@code nameContainsLower} (case-insensitive substring).
     * Returns null if nothing matches — the caller treats that exactly like
     * an unreachable server (value stays -1).
     */
    private LeafSensor findFirstSensor(JsonObject root, String sensorTypeLower, String nameContainsLower) {
        Deque<JsonObject> queue = new ArrayDeque<>();
        queue.add(root);
        int visited = 0;
        // Hard cap so a pathological/huge tree can't turn a single poll into
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

    private static String getString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && el.isJsonPrimitive()) ? el.getAsString() : null;
    }

    /**
     * SensorId looks like {@code /gpu-nvidia/0/temperature/0} — the second
     * path segment from the end (or more simply, the segment right before
     * the trailing index) is the sensor type LHM assigned it. Returns null
     * if the id doesn't look like that shape at all, so a format change in
     * a future LHM version degrades to "no match" rather than an exception.
     */
    private static String sensorTypeFromId(String sensorId) {
        if (sensorId == null || sensorId.isEmpty()) return null;
        String[] parts = sensorId.split("/");
        // Expect at least .../<type>/<index> — need 2 trailing segments.
        if (parts.length < 2) return null;
        return parts[parts.length - 2];
    }

    /**
     * LHM formats Value as e.g. "62.3 °C", "1875 MHz", "45.0 %", "8192 MB" —
     * this pulls the leading numeric portion and ignores the unit suffix
     * entirely (MTSS supplies its own unit via its own lang strings), so a
     * locale-dependent decimal separator or an unexpected unit string can't
     * break parsing. Returns null (not 0) when nothing numeric is found, so
     * callers can tell "no value" apart from a genuine zero reading.
     */
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
            // Normalize a comma decimal separator to '.' — skip thousands-style
            // commas isn't a concern here since LHM's values are small.
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
}
