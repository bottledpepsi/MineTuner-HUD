package bottled.mtss;

import bottled.mtss.config.MtssConfig.ThresholdSettings;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;

public final class MtssDataHolder {

    // --- Graph history ring buffers ---
    private static final int HISTORY_SIZE = 2000;
    private static final RingBuffer tpsHistory = new RingBuffer(HISTORY_SIZE);
    private static final RingBuffer msptHistory = new RingBuffer(HISTORY_SIZE);
    private static final RingBuffer fpsHistory = new RingBuffer(HISTORY_SIZE);
    private static final RingBuffer cpuHistory = new RingBuffer(HISTORY_SIZE);
    private static final RingBuffer pingHistory = new RingBuffer(HISTORY_SIZE);
    private static final RingBuffer memHistory = new RingBuffer(HISTORY_SIZE);
    private static final RingBuffer speedHistory = new RingBuffer(HISTORY_SIZE);
    private static final RingBuffer healthHistory = new RingBuffer(HISTORY_SIZE);
    private static final RingBuffer hungerHistory = new RingBuffer(HISTORY_SIZE);
    private static final RingBuffer armorHistory = new RingBuffer(HISTORY_SIZE);
    private static final RingBuffer gpuTempHistory = new RingBuffer(HISTORY_SIZE);
    private static final RingBuffer gpuClockHistory = new RingBuffer(HISTORY_SIZE);
    private static final RingBuffer gpuUsageHistory = new RingBuffer(HISTORY_SIZE);
    private static final RingBuffer vramUsedHistory = new RingBuffer(HISTORY_SIZE);

    // --- JVM/OS metric sources ---
    private static final MemoryMXBean MEM_BEAN = ManagementFactory.getMemoryMXBean();
    private static final OperatingSystemMXBean OS_BEAN = ManagementFactory.getOperatingSystemMXBean();
    private static final List<GarbageCollectorMXBean> GC_BEANS =
            ManagementFactory.getGarbageCollectorMXBeans();
    private static final long SLOW_MS = 500;

    // --- Performance ---
    public static float tickRate = 20.0f;
    /** -1 when not on a singleplayer/LAN server (MSPT unavailable). */
    public static float mspt = -1f;
    public static int fps = 0;
    public static int ping = -1;
    public static int entityCount = 0;
    public static int loadedChunks = 0;
    public static int renderedSections = 0;

    // --- Player position/facing ---
    public static double playerX = 0;
    public static double playerY = 0;
    public static double playerZ = 0;
    public static String facingName = "S";
    public static float playerYaw = 0; // normalized to [0, 360).
    public static float playerPitch = 0; // [-90 (straight up), 90 (straight down)].
    public static float speedBps = 0;
    public static int lightLevel = 0;

    // --- World state ---
    public static String biomeName = "";
    public static String dimensionName = "";
    public static boolean isRaining = false;
    public static boolean isThundering = false;
    public static String difficultyName = "";
    public static int skyLight = 0;
    public static int blockLight = 0;
    public static boolean canSeeSky = false;
    public static int chunkX = 0;
    public static int chunkZ = 0;
    public static double distanceFromSpawn = 0;

    // --- Player vitals ---
    public static float health = 0f;
    public static float maxHealth = 20f;
    public static int hunger = 0;
    public static float saturation = 0f;
    public static int armor = 0;
    public static int air = 0;
    public static int maxAir = 300;
    public static int xpLevel = 0;
    public static float xpProgress = 0f;
    public static String gameMode = "";
    public static int selectedSlot = 0;
    public static String heldItemName = "";
    public static float verticalSpeedBps = 0f;

    // --- Misc world/player ---
    public static int playersOnline = 0;
    /** Empty when nothing is targeted (crosshair over air/sky at max reach). */
    public static String lookingAtName = "";
    /** "block", "entity", or "" (nothing targeted). */
    public static String lookingAtKind = "";
    /** True while the player has non-negligible horizontal movement (above a
     *  small epsilon, to avoid flickering true/false from floating-point noise
     *  while genuinely standing still — see TargetingSource.MOVING_EPSILON). */
    public static boolean isMoving = false;

    // --- JVM/CPU metrics ---
    public static long memUsedMb = 0;
    public static long memMaxMb = 0;
    public static double cpuPercent = -1.0;
    public static long gcTimeMs = 0;

    // --- Hardware sensors (GPU/VRAM), via LibreHardwareMonitor ---
    // Unlike every other field in this class, these are written from
    // HardwareSensorPoller's dedicated background thread (bottled.mtss.sample),
    // not the render thread — see that class's doc for why. volatile gives the
    // render thread a guaranteed-fresh read without needing a lock for what's
    // otherwise a simple "poll thread writes, render thread reads" handoff.
    // -1.0 means "not available right now" (LHM not running/reachable, remote web
    // server off, sensor missing from this system's tree, or hardware
    // sensors not enabled in config at all) — the same sentinel-value
    // convention as cpuPercent above, so callers don't need a separate
    // "is this stat available" flag before a genuine reading can be surfaced.
    public static volatile double gpuTempC = -1.0;
    public static volatile double gpuClockMhz = -1.0;
    public static volatile double gpuUsagePercent = -1.0;
    public static volatile double vramUsedMb = -1.0;
    public static volatile double vramMaxMb = -1.0;
    /** True once the poller has successfully reached LHM at least once this session. */
    public static volatile boolean hardwareSensorsReachable = false;
    private static long lastSlowUpdateMs = 0;
    private MtssDataHolder() {
    }

    public static void updateFastMetrics() {
        long used = MEM_BEAN.getHeapMemoryUsage().getUsed();
        long max = MEM_BEAN.getHeapMemoryUsage().getMax();
        memUsedMb = used / (1024 * 1024);
        memMaxMb = max / (1024 * 1024);

        // Sample each graphable stat once per frame. Guarded pushes (e.g. "if
        // (mspt >= 0f)") skip the push entirely when a stat's underlying value isn't
        // available yet (CPU/Ping at -1, Memory before the first heap read),
        // so the buffer doesn't fill with flat -1 lines that would otherwise
        // show up as a fake reading on the graph.
        tpsHistory.push(getTps());
        if (mspt >= 0f) msptHistory.push(mspt);
        fpsHistory.push(fps);
        if (cpuPercent >= 0) cpuHistory.push((float) cpuPercent);
        if (ping >= 0) pingHistory.push(ping);
        if (memMaxMb > 0) memHistory.push((float) (100.0 * memUsedMb / memMaxMb));
        speedHistory.push(speedBps);
        // Health is stored as a percent of max, same convention as Memory's
        // used/max percentage above, so the graph's 0-100 scale stays
        // valid for historical samples even if max health changes mid-session
        // (e.g. from an absorption/health-boost effect ending, or a
        // max-health-modifying attribute change).
        if (maxHealth > 0) healthHistory.push(100f * health / maxHealth);
        hungerHistory.push(hunger);
        armorHistory.push(armor);

        // Hardware sensors follow the same guarded-push convention described
        // above, so a flat run of -1 (LHM not running, or feature disabled)
        // never enters the buffer as if it were a real reading.
        if (gpuTempC >= 0) gpuTempHistory.push((float) gpuTempC);
        if (gpuClockMhz >= 0) gpuClockHistory.push((float) gpuClockMhz);
        if (gpuUsagePercent >= 0) gpuUsageHistory.push((float) gpuUsagePercent);
        if (vramUsedMb >= 0) vramUsedHistory.push((float) vramUsedMb);
    }

    public static void updateSlowMetrics() {
        long now = System.currentTimeMillis();
        if (now - lastSlowUpdateMs < SLOW_MS) return;
        lastSlowUpdateMs = now;

        // HotSpot-only API.
        if (OS_BEAN instanceof com.sun.management.OperatingSystemMXBean sun) {
            double raw = sun.getProcessCpuLoad();
            cpuPercent = raw >= 0 ? raw * 100.0 : -1.0;
        }

        long total = 0;
        for (GarbageCollectorMXBean gc : GC_BEANS) {
            long t = gc.getCollectionTime();
            if (t >= 0) total += t;
        }
        gcTimeMs = total;
    }

    /** Returns a copy each time so callers can't mutate the backing buffer. */
    public static float[] getTpsHistory() {
        return tpsHistory.snapshot();
    }

    public static float[] getMsptHistory() {
        return msptHistory.snapshot();
    }

    public static float[] getFpsHistory() {
        return fpsHistory.snapshot();
    }

    public static float[] getCpuHistory() {
        return cpuHistory.snapshot();
    }

    public static float[] getPingHistory() {
        return pingHistory.snapshot();
    }

    public static float[] getMemHistory() {
        return memHistory.snapshot();
    }

    public static float[] getSpeedHistory() {
        return speedHistory.snapshot();
    }

    public static float[] getHealthHistory() {
        return healthHistory.snapshot();
    }

    public static float[] getHungerHistory() {
        return hungerHistory.snapshot();
    }

    public static float[] getArmorHistory() {
        return armorHistory.snapshot();
    }

    public static float[] getGpuTempHistory() {
        return gpuTempHistory.snapshot();
    }

    public static float[] getGpuClockHistory() {
        return gpuClockHistory.snapshot();
    }

    public static float[] getGpuUsageHistory() {
        return gpuUsageHistory.snapshot();
    }

    public static float[] getVramUsedHistory() {
        return vramUsedHistory.snapshot();
    }

    public static float getTps() {
        if (mspt > 0f) return Math.min(tickRate, 1000f / mspt);
        return tickRate;
    }

    // Each getXColor() reads live state and calls the matching xColorFor(value),
    // so graph mode can color historical samples with the same thresholds as
    // classic text mode — getXColor() is just xColorFor(currentValue), and both
    // accept an optional per-list custom ThresholdSettings override (null = use
    // this stat's own built-in default thresholds instead).
    // for the default hardcoded bands.

    public static int getTpsColor() {
        return getTpsColor(null);
    }

    public static int getTpsColor(ThresholdSettings custom) {
        return tpsColorFor(getTps(), custom);
    }

    public static int tpsColorFor(float tps) {
        return tpsColorFor(tps, null);
    }

    /** TPS is higher-is-better. */
    public static int tpsColorFor(float tps, ThresholdSettings custom) {
        if (custom != null && custom.enabled) {
            if (tps >= custom.goodMin) return 0xFF55FF55;
            if (tps >= custom.warnMin) return 0xFFFFFF55;
            return 0xFFFF5555;
        }
        if (tps >= 18f) return 0xFF55FF55;
        if (tps >= 14f) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    public static int getFpsColor() {
        return getFpsColor(null);
    }

    public static int getFpsColor(ThresholdSettings custom) {
        return fpsColorFor(fps, custom);
    }

    public static int fpsColorFor(float fpsValue) {
        return fpsColorFor(fpsValue, null);
    }

    /** FPS is higher-is-better. */
    public static int fpsColorFor(float fpsValue, ThresholdSettings custom) {
        if (custom != null && custom.enabled) {
            if (fpsValue >= custom.goodMin) return 0xFF55FF55;
            if (fpsValue >= custom.warnMin) return 0xFFFFFF55;
            return 0xFFFF5555;
        }
        if (fpsValue >= 60) return 0xFF55FF55;
        if (fpsValue >= 30) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    public static int getPingColor() {
        return getPingColor(null);
    }

    public static int getPingColor(ThresholdSettings custom) {
        return pingColorFor(ping, custom);
    }

    public static int pingColorFor(float pingValue) {
        return pingColorFor(pingValue, null);
    }

    /** Ping is lower-is-better. */
    public static int pingColorFor(float pingValue, ThresholdSettings custom) {
        if (pingValue < 0) return 0xFFFFFFFF;
        if (custom != null && custom.enabled) {
            if (pingValue <= custom.goodMin) return 0xFF55FF55;
            if (pingValue <= custom.warnMin) return 0xFFFFFF55;
            return 0xFFFF5555;
        }
        if (pingValue <= 80) return 0xFF55FF55;
        if (pingValue <= 150) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    public static int getMemColor() {
        return getMemColor(null);
    }

    public static int getMemColor(ThresholdSettings custom) {
        if (memMaxMb <= 0) return 0xFFFFFFFF;
        return memColorForPercent(100.0 * memUsedMb / memMaxMb, custom);
    }

    /** For graph mode, where history is already stored as a used/max percentage
     *  (see MtssDataHolder.updateFastMetrics's memHistory.push call) rather than
     *  raw used/max megabyte values — same "already a percent, no memMaxMb needed"
     *  shortcut healthColorFor/HealthStat.colorFor use for the health graph. */
    public static int memColorForPercent(double pct100) {
        return memColorForPercent(pct100, null);
    }

    /** Memory (used %) is lower-is-better. */
    public static int memColorForPercent(double pct100, ThresholdSettings custom) {
        if (custom != null && custom.enabled) {
            if (pct100 <= custom.goodMin) return 0xFF55FF55;
            if (pct100 <= custom.warnMin) return 0xFFFFFF55;
            return 0xFFFF5555;
        }
        double pct = pct100 / 100.0;
        if (pct < 0.6) return 0xFF55FF55;
        if (pct < 0.85) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    public static int getCpuColor() {
        return getCpuColor(null);
    }

    public static int getCpuColor(ThresholdSettings custom) {
        return cpuColorFor(cpuPercent, custom);
    }

    public static int cpuColorFor(double cpuValue) {
        return cpuColorFor(cpuValue, null);
    }

    /** CPU is lower-is-better. */
    public static int cpuColorFor(double cpuValue, ThresholdSettings custom) {
        if (cpuValue < 0) return 0xFFFFFFFF;
        if (custom != null && custom.enabled) {
            if (cpuValue <= custom.goodMin) return 0xFF55FF55;
            if (cpuValue <= custom.warnMin) return 0xFFFFFF55;
            return 0xFFFF5555;
        }
        if (cpuValue < 50) return 0xFF55FF55;
        if (cpuValue < 80) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    // Speed isn't part of the ThresholdSettings system.
    // logic isn't a good/warn/bad scale, so there's no threshold override here.
    public static int getSpeedColor() {
        return speedColorFor(speedBps);
    }

    public static int speedColorFor(float speedValue) {
        if (speedValue < 0.01f) return 0xFFAAAAAA;
        if (speedValue > 20f) return 0xFFFFFF55;
        return 0xFFFFFFFF;
    }

    public static int getHealthColor() {
        return getHealthColor(null);
    }

    public static int getHealthColor(ThresholdSettings custom) {
        return healthColorFor(health, custom);
    }

    public static int healthColorFor(float healthValue) {
        return healthColorFor(healthValue, null);
    }

    /** Health is higher-is-better, measured as a percent of max so it works with
     *  goodMin/warnMin thresholds on the same fixed 0-100 scale regardless of the
     *  player's actual max health. Converts the raw current-health value to a percent
     *  internally using the *current* maxHealth — contrast with HealthStat.colorFor,
     *  which receives an already-percent value from the history buffer instead,
     *  each historical sample having been converted against maxHealth as it stood
     *  at that sample's own moment (which may differ from the current maxHealth). */
    public static int healthColorFor(float healthValue, ThresholdSettings custom) {
        float pct = maxHealth > 0 ? (100f * healthValue / maxHealth) : 0f;
        if (custom != null && custom.enabled) {
            if (pct >= custom.goodMin) return 0xFF55FF55;
            if (pct >= custom.warnMin) return 0xFFFFFF55;
            return 0xFFFF5555;
        }
        if (pct >= 75f) return 0xFF55FF55;
        if (pct >= 35f) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    public static int getHungerColor() {
        return getHungerColor(null);
    }

    public static int getHungerColor(ThresholdSettings custom) {
        return hungerColorFor(hunger, custom);
    }

    public static int hungerColorFor(float hungerValue) {
        return hungerColorFor(hungerValue, null);
    }

    /** Hunger (food level, 0-20) is higher-is-better. */
    public static int hungerColorFor(float hungerValue, ThresholdSettings custom) {
        if (custom != null && custom.enabled) {
            if (hungerValue >= custom.goodMin) return 0xFF55FF55;
            if (hungerValue >= custom.warnMin) return 0xFFFFFF55;
            return 0xFFFF5555;
        }
        if (hungerValue >= 15) return 0xFF55FF55;
        if (hungerValue >= 6) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    public static int getArmorColor() {
        return getArmorColor(null);
    }

    public static int getArmorColor(ThresholdSettings custom) {
        return armorColorFor(armor, custom);
    }

    public static int armorColorFor(float armorValue) {
        return armorColorFor(armorValue, null);
    }

    /** Armor points (0-20) is higher-is-better. */
    public static int armorColorFor(float armorValue, ThresholdSettings custom) {
        if (custom != null && custom.enabled) {
            if (armorValue >= custom.goodMin) return 0xFF55FF55;
            if (armorValue >= custom.warnMin) return 0xFFFFFF55;
            return 0xFFFF5555;
        }
        if (armorValue >= 15) return 0xFF55FF55;
        if (armorValue >= 5) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    public static int getGpuTempColor() {
        return getGpuTempColor(null);
    }

    public static int getGpuTempColor(ThresholdSettings custom) {
        return gpuTempColorFor(gpuTempC, custom);
    }

    public static int gpuTempColorFor(double tempValue) {
        return gpuTempColorFor(tempValue, null);
    }

    /** GPU temperature is lower-is-better. */
    public static int gpuTempColorFor(double tempValue, ThresholdSettings custom) {
        if (tempValue < 0) return 0xFFFFFFFF;
        if (custom != null && custom.enabled) {
            if (tempValue <= custom.goodMin) return 0xFF55FF55;
            if (tempValue <= custom.warnMin) return 0xFFFFFF55;
            return 0xFFFF5555;
        }
        if (tempValue < 70) return 0xFF55FF55;
        if (tempValue < 85) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    // Air isn't a ThresholdSettings stat.
    // time (not underwater) and only becomes meaningful near zero, so a.
    // fixed "danger below 20%" rule reads better than a tunable good/warn band.
    public static int getAirColor() {
        return airColorFor(air, maxAir);
    }

    public static int airColorFor(float airValue, int maxAirValue) {
        if (maxAirValue <= 0) return 0xFFFFFFFF;
        float pct = 100f * airValue / maxAirValue;
        if (pct >= 100f) return 0xFFFFFFFF; // full breath.
        if (pct >= 20f) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    private static String t(String key, Object... args) {
        return net.minecraft.client.resources.language.I18n.get(key, args);
    }

    private static String fmt(double value, int decimals) {
        return String.format("%." + Math.max(0, Math.min(6, decimals)) + "f", value);
    }

    public static String getFormattedTps() {
        return getFormattedTps(1);
    }

    public static String getFormattedTps(int decimals) {
        return t("mtss.stat.tps", fmt(getTps(), decimals));
    }

    /** Returns empty string on remote servers. */
    public static String getFormattedMspt() {
        return getFormattedMspt(1);
    }

    public static String getFormattedMspt(int decimals) {
        return mspt >= 0f ? t("mtss.stat.mspt", fmt(mspt, decimals)) : "";
    }

    public static String getFormattedFps() {
        return t("mtss.stat.fps", fps);
    }

    public static String getFormattedPing() {
        return ping >= 0 ? t("mtss.stat.ping", ping) : t("mtss.stat.ping.na");
    }

    /** Bare ping value with no "ms" suffix, for Template Mode. */
    public static String getRawPing() {
        return ping >= 0 ? Integer.toString(ping) : "N/A";
    }

    public static String getFormattedMem() {
        return t("mtss.stat.memory", memUsedMb, memMaxMb);
    }

    public static String getFormattedCpu() {
        return getFormattedCpu(1);
    }

    public static String getFormattedCpu(int decimals) {
        return cpuPercent >= 0 ? t("mtss.stat.cpu", fmt(cpuPercent, decimals)) : t("mtss.stat.cpu.na");
    }

    /** Bare CPU value with no "%" suffix, for Template Mode. */
    public static String getRawCpu(int decimals) {
        return cpuPercent >= 0 ? fmt(cpuPercent, decimals) : "N/A";
    }

    public static String getFormattedEntities() {
        return t("mtss.stat.entities", entityCount);
    }

    public static String getFormattedChunks() {
        return t("mtss.stat.chunks", loadedChunks);
    }

    public static String getFormattedRendered() {
        return t("mtss.stat.rendered", renderedSections);
    }

    public static String getFormattedCoords() {
        return t("mtss.stat.coords", (int) Math.floor(playerX), (int) Math.floor(playerY), (int) Math.floor(playerZ));
    }

    public static String getFormattedX() {
        return t("mtss.stat.x", (int) Math.floor(playerX));
    }

    public static String getFormattedY() {
        return t("mtss.stat.y", (int) Math.floor(playerY));
    }

    public static String getFormattedZ() {
        return t("mtss.stat.z", (int) Math.floor(playerZ));
    }

    /** Bare block-rounded X coordinate, no label. */
    public static String getRawX() {
        return Integer.toString((int) Math.floor(playerX));
    }

    /** Bare block-rounded Y coordinate, no label. */
    public static String getRawY() {
        return Integer.toString((int) Math.floor(playerY));
    }

    /** Bare block-rounded Z coordinate, no label. */
    public static String getRawZ() {
        return Integer.toString((int) Math.floor(playerZ));
    }

    public static String getFormattedFacing() {
        return t("mtss.stat.facing", facingName);
    }

    public static String getFormattedYaw() {
        return getFormattedYaw(1);
    }

    public static String getFormattedYaw(int decimals) {
        return t("mtss.stat.yaw", fmt(playerYaw, decimals));
    }

    /** Bare yaw value with no label, for Template Mode. */
    public static String getRawYaw(int decimals) {
        return fmt(playerYaw, decimals);
    }

    public static String getFormattedPitch() {
        return getFormattedPitch(1);
    }

    public static String getFormattedPitch(int decimals) {
        return t("mtss.stat.pitch", fmt(playerPitch, decimals));
    }

    /** Bare pitch value with no label, for Template Mode. */
    public static String getRawPitch(int decimals) {
        return fmt(playerPitch, decimals);
    }

    public static String getFormattedSpeed() {
        return getFormattedSpeed(2);
    }

    public static String getFormattedSpeed(int decimals) {
        return t("mtss.stat.speed", fmt(speedBps, decimals));
    }

    /** Bare speed value with no "b/s" suffix, for Template Mode. */
    public static String getRawSpeed(int decimals) {
        return fmt(speedBps, decimals);
    }

    public static String getFormattedGcTime() {
        return t("mtss.stat.gc", gcTimeMs);
    }

    /** Bare GC time value with no "ms" suffix, for Template Mode. */
    public static String getRawGcTime() {
        return Long.toString(gcTimeMs);
    }

    public static String getFormattedBiome() {
        return t("mtss.stat.biome", biomeName.isEmpty() ? "?" : biomeName);
    }

    public static String getFormattedLight() {
        return t("mtss.stat.light", lightLevel);
    }

    public static String getFormattedDimension() {
        return t("mtss.stat.dimension", dimensionName.isEmpty() ? "?" : dimensionName);
    }

    public static String getFormattedHealth() {
        return t("mtss.stat.health", fmt(health, 0), fmt(maxHealth, 0));
    }

    /** Bare "current/max" with no label, for Template Mode. */
    public static String getRawHealth() {
        return fmt(health, 0) + "/" + fmt(maxHealth, 0);
    }

    public static String getFormattedHunger() {
        return t("mtss.stat.hunger", hunger);
    }

    public static String getFormattedSaturation() {
        return getFormattedSaturation(1);
    }

    public static String getFormattedSaturation(int decimals) {
        return t("mtss.stat.saturation", fmt(saturation, decimals));
    }

    public static String getRawSaturation(int decimals) {
        return fmt(saturation, decimals);
    }

    public static String getFormattedArmor() {
        return t("mtss.stat.armor", armor);
    }

    /** Empty string when at full air (nothing worth showing outside water), same. */
    public static String getFormattedAir() {
        return air < maxAir ? t("mtss.stat.air", air) : "";
    }

    public static String getRawAir() {
        return Integer.toString(air);
    }

    public static String getFormattedXpLevel() {
        return t("mtss.stat.xp_level", xpLevel);
    }

    public static String getFormattedXpProgress() {
        return getFormattedXpProgress(0);
    }

    public static String getFormattedXpProgress(int decimals) {
        return t("mtss.stat.xp_progress", fmt(xpProgress * 100.0, decimals));
    }

    public static String getRawXpProgress(int decimals) {
        return fmt(xpProgress * 100.0, decimals);
    }

    public static String getFormattedGameMode() {
        String raw = gameMode;
        String display = raw.isEmpty() ? "?" : Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
        return t("mtss.stat.game_mode", display);
    }

    public static String getFormattedSelectedSlot() {
        return t("mtss.stat.selected_slot", selectedSlot + 1);
    }

    public static String getFormattedHeldItem() {
        return t("mtss.stat.held_item", heldItemName.isEmpty() ? "-" : heldItemName);
    }

    public static String getFormattedVerticalSpeed() {
        return getFormattedVerticalSpeed(2);
    }

    public static String getFormattedVerticalSpeed(int decimals) {
        return t("mtss.stat.vertical_speed", fmt(verticalSpeedBps, decimals));
    }

    public static String getRawVerticalSpeed(int decimals) {
        return fmt(verticalSpeedBps, decimals);
    }

    public static String getFormattedWeather() {
        String state = isThundering ? t("mtss.stat.weather.thunder")
                : isRaining ? t("mtss.stat.weather.rain")
                : t("mtss.stat.weather.clear");
        return t("mtss.stat.weather", state);
    }

    public static String getFormattedDifficulty() {
        String raw = difficultyName;
        String display = raw.isEmpty() ? "?" : Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
        return t("mtss.stat.difficulty", display);
    }

    public static String getFormattedSkyLight() {
        return t("mtss.stat.sky_light", skyLight);
    }

    public static String getFormattedBlockLight() {
        return t("mtss.stat.block_light", blockLight);
    }

    public static String getFormattedCanSeeSky() {
        return t("mtss.stat.can_see_sky", canSeeSky ? t("gui.mtss.menu.on") : t("gui.mtss.menu.off"));
    }

    public static String getFormattedPlayersOnline() {
        return t("mtss.stat.players_online", playersOnline);
    }

    public static String getFormattedChunkPos() {
        return t("mtss.stat.chunk_pos", chunkX, chunkZ);
    }

    public static String getFormattedDistanceFromSpawn() {
        return getFormattedDistanceFromSpawn(0);
    }

    public static String getFormattedDistanceFromSpawn(int decimals) {
        return t("mtss.stat.distance_from_spawn", fmt(distanceFromSpawn, decimals));
    }

    public static String getRawDistanceFromSpawn(int decimals) {
        return fmt(distanceFromSpawn, decimals);
    }

    /** Empty string when nothing is targeted, same "skip this line" convention as
     *  getFormattedMspt()/getFormattedGpuTemp() use for their own unavailable cases. */
    public static String getFormattedLookingAt() {
        return lookingAtName.isEmpty() ? "" : t("mtss.stat.looking_at", lookingAtName);
    }

    /** Bare targeted-thing name, or a translated placeholder for Template Mode. */
    public static String getRawLookingAt() {
        return lookingAtName.isEmpty() ? t("mtss.stat.looking_at.none") : lookingAtName;
    }

    public static String getFormattedMoving() {
        return t("mtss.stat.moving", isMoving ? t("gui.mtss.menu.on") : t("gui.mtss.menu.off"));
    }

    public static String getFormattedGpuTemp() {
        return getFormattedGpuTemp(0);
    }

    // -1 means "unavailable" (see the field doc above), and getFormattedGpuTemp()
    // follows getFormattedMspt()'s "" (skip this line) convention rather than
    // getFormattedCpu()'s "show a literal N/A line" one. GPU/VRAM stats are
    // *structurally* absent for most users (an opt-in remote-sensor feature,
    // off by default, that most people never set up) rather than a single
    // always-enabled stat that occasionally can't resolve a value, so
    // skipping the line entirely (LineBuilder's existing "" == omit-this-row
    // signal) avoids a permanent "N/A" row on every HUD that hasn't set this up
    // — matching MSPT's own "hidden unless you're on singleplayer/LAN" behavior.
    // getRawGpuTemp/getRawGpuClock/etc. (used by Template Mode, where the
    // user's own template text supplies the label) still return a
    // literal "N/A" like getRawCpu()/getRawPing() do, since a template line
    // mixing several tokens going silently blank for just one would read as
    // a rendering glitch rather than "unavailable".

    public static String getFormattedGpuTemp(int decimals) {
        return gpuTempC >= 0 ? t("mtss.stat.gpu_temp", fmt(gpuTempC, decimals)) : "";
    }

    /** Bare temperature value with no "°C" suffix, for Template Mode. */
    public static String getRawGpuTemp(int decimals) {
        return gpuTempC >= 0 ? fmt(gpuTempC, decimals) : "N/A";
    }

    public static String getFormattedGpuClock() {
        return getFormattedGpuClock(0);
    }

    public static String getFormattedGpuClock(int decimals) {
        return gpuClockMhz >= 0 ? t("mtss.stat.gpu_clock", fmt(gpuClockMhz, decimals)) : "";
    }

    /** Bare clock value with no "MHz" suffix, for Template Mode. */
    public static String getRawGpuClock(int decimals) {
        return gpuClockMhz >= 0 ? fmt(gpuClockMhz, decimals) : "N/A";
    }

    public static String getFormattedGpuUsage() {
        return getFormattedGpuUsage(0);
    }

    public static String getFormattedGpuUsage(int decimals) {
        return gpuUsagePercent >= 0 ? t("mtss.stat.gpu_usage", fmt(gpuUsagePercent, decimals)) : "";
    }

    /** Bare usage value with no "%" suffix, for Template Mode. */
    public static String getRawGpuUsage(int decimals) {
        return gpuUsagePercent >= 0 ? fmt(gpuUsagePercent, decimals) : "N/A";
    }

    public static String getFormattedVramUsed() {
        // Only render once both used and max are known — a used-but-not-max reading
        // (e.g. mid-poll, or a sensor tree that only partially resolved this cycle)
        // would otherwise show a misleading "1024/0MB".
        return (vramUsedMb >= 0 && vramMaxMb > 0)
                ? t("mtss.stat.vram_used", Math.round(vramUsedMb), Math.round(vramMaxMb))
                : "";
    }

    /** Bare "used/maxMB" with no label, for Template Mode. */
    public static String getRawVramUsed() {
        return (vramUsedMb >= 0 && vramMaxMb > 0)
                ? Math.round(vramUsedMb) + "/" + Math.round(vramMaxMb)
                : "N/A";
    }

    /** Minimal fixed-size float ring buffer with copy-out reads. */
    private static final class RingBuffer {
        private final float[] buf;
        private int count = 0; // number of valid samples, caps at buf.length.
        private int head = 0; // index the NEXT sample will be written to.

        RingBuffer(int size) {
            this.buf = new float[size];
        }

        void push(float value) {
            buf[head] = value;
            head = (head + 1) % buf.length;
            if (count < buf.length) count++;
        }

        /** Returns a copy of the valid samples in oldest-to-newest order. */
        float[] snapshot() {
            float[] out = new float[count];
            int start = (head - count + buf.length) % buf.length;
            for (int i = 0; i < count; i++) {
                out[i] = buf[(start + i) % buf.length];
            }
            return out;
        }
    }
}
