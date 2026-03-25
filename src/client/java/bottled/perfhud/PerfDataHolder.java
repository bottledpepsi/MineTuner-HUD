package bottled.perfhud;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;

public final class PerfDataHolder {

    // ── Server ────────────────────────────────────────────────────────────────
    public static float tickRate = 20.0f;
    /** -1 when not on a singleplayer/LAN server (MSPT unavailable). */
    public static float mspt = -1f;

    // ── Client ────────────────────────────────────────────────────────────────
    public static int fps              = 0;
    public static int ping             = -1;
    public static int entityCount      = 0;
    public static int loadedChunks     = 0;
    public static int renderedSections = 0;

    // ── Player ────────────────────────────────────────────────────────────────
    public static double playerX    = 0;
    public static double playerY    = 0;
    public static double playerZ    = 0;
    public static String facingName = "S";
    public static float  speedBps   = 0;

    // ── System (throttled) ────────────────────────────────────────────────────
    public static long   memUsedMb  = 0;
    public static long   memMaxMb   = 0;
    public static double cpuPercent = -1.0;
    public static long   gcTimeMs   = 0;

    // ── Internals ─────────────────────────────────────────────────────────────
    private static final MemoryMXBean          MEM_BEAN = ManagementFactory.getMemoryMXBean();
    private static final OperatingSystemMXBean OS_BEAN  = ManagementFactory.getOperatingSystemMXBean();
    private static final List<GarbageCollectorMXBean> GC_BEANS =
            ManagementFactory.getGarbageCollectorMXBeans();

    private static long lastSlowUpdateMs = 0;
    private static final long SLOW_MS = 500;

    private PerfDataHolder() {}

    // ── Updates ───────────────────────────────────────────────────────────────

    public static void updateFastMetrics() {
        long used = MEM_BEAN.getHeapMemoryUsage().getUsed();
        long max  = MEM_BEAN.getHeapMemoryUsage().getMax();
        memUsedMb = used / (1024 * 1024);
        memMaxMb  = max  / (1024 * 1024);
    }

    public static void updateSlowMetrics() {
        long now = System.currentTimeMillis();
        if (now - lastSlowUpdateMs < SLOW_MS) return;
        lastSlowUpdateMs = now;

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

    // ── Color helpers ─────────────────────────────────────────────────────────

    public static float getTps() {
        if (mspt > 0f) return Math.min(tickRate, 1000f / mspt);
        return tickRate;
    }
    public static int getTpsColor() {
        float t = getTps();
        if (t >= 18f) return 0xFF55FF55;
        if (t >= 14f) return 0xFFFFFF55;
        return 0xFFFF5555;
    }
    public static int getFpsColor() {
        if (fps >= 60) return 0xFF55FF55;
        if (fps >= 30) return 0xFFFFFF55;
        return 0xFFFF5555;
    }
    public static int getPingColor() {
        if (ping < 0)    return 0xFFFFFFFF;
        if (ping <= 80)  return 0xFF55FF55;
        if (ping <= 150) return 0xFFFFFF55;
        return 0xFFFF5555;
    }
    public static int getMemColor() {
        if (memMaxMb <= 0) return 0xFFFFFFFF;
        double pct = (double) memUsedMb / memMaxMb;
        if (pct < 0.6)  return 0xFF55FF55;
        if (pct < 0.85) return 0xFFFFFF55;
        return 0xFFFF5555;
    }
    public static int getCpuColor() {
        if (cpuPercent < 0)  return 0xFFFFFFFF;
        if (cpuPercent < 50) return 0xFF55FF55;
        if (cpuPercent < 80) return 0xFFFFFF55;
        return 0xFFFF5555;
    }
    public static int getSpeedColor() {
        if (speedBps < 0.01f) return 0xFFAAAAAA;
        if (speedBps > 20f)   return 0xFFFFFF55;
        return 0xFFFFFFFF;
    }

    // ── Formatted strings (via I18n so text is translatable) ──────────────────

    private static String t(String key, Object... args) {
        return net.minecraft.client.resources.language.I18n.get(key, args);
    }

    public static String getFormattedTps()  { return t("perfhud.stat.tps",  String.format("%.1f", getTps())); }

    /** Returns empty string on remote servers — caller skips the line. */
    public static String getFormattedMspt() {
        return mspt >= 0f ? t("perfhud.stat.mspt", String.format("%.1f", mspt)) : "";
    }

    public static String getFormattedFps()      { return t("perfhud.stat.fps",      fps); }
    public static String getFormattedPing()     { return ping >= 0 ? t("perfhud.stat.ping", ping) : t("perfhud.stat.ping.na"); }
    public static String getFormattedMem()      { return t("perfhud.stat.memory",   memUsedMb, memMaxMb); }
    public static String getFormattedCpu()      { return cpuPercent >= 0 ? t("perfhud.stat.cpu", String.format("%.1f", cpuPercent)) : t("perfhud.stat.cpu.na"); }
    public static String getFormattedEntities() { return t("perfhud.stat.entities", entityCount); }
    public static String getFormattedChunks()   { return t("perfhud.stat.chunks",   loadedChunks); }
    public static String getFormattedRendered() { return t("perfhud.stat.rendered", renderedSections); }
    public static String getFormattedCoords()   { return t("perfhud.stat.coords",   (int) Math.floor(playerX), (int) Math.floor(playerY), (int) Math.floor(playerZ)); }
    public static String getFormattedFacing()   { return t("perfhud.stat.facing",   facingName); }
    public static String getFormattedSpeed()    { return t("perfhud.stat.speed",    String.format("%.2f", speedBps)); }
    public static String getFormattedGcTime()   { return t("perfhud.stat.gc",       gcTimeMs); }
}
