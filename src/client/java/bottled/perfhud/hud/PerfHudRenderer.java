package bottled.perfhud.hud;

import bottled.perfhud.PerfDataHolder;
import bottled.perfhud.config.PerfHudConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayList;
import java.util.List;

public class PerfHudRenderer {

    // ── Per-frame line cache ──────────────────────────────────────────────────
    // buildLines is called both here (render) and in the GUI (drawList + getListBounds).
    // The cache avoids redundant string building within the same frame.

    public record LineCache(List<String> lines, List<Integer> colors) {
        public int boxW(net.minecraft.client.gui.Font font) {
            return lines.stream().mapToInt(font::width).max().orElse(0) + 4;
        }
        public int boxH(net.minecraft.client.gui.Font font) {
            return (font.lineHeight + 1) * lines.size() + 3;
        }
    }

    private static final java.util.Map<Integer, LineCache> FRAME_CACHE = new java.util.HashMap<>();
    /**
     * Incremented by {@link #tickCache()} at the start of each render call.
     * The GUI screen calls tickCache() too so both share the same frame budget.
     */
    private static long cacheGeneration = 0;
    private static long lastCacheGeneration = -1;

    /** Advance the cache generation — call once per render frame. */
    public static void tickCache() {
        if (cacheGeneration != lastCacheGeneration) {
            FRAME_CACHE.clear();
            lastCacheGeneration = cacheGeneration;
        }
        cacheGeneration++;
    }

    /** Returns cached lines for this list, building them if needed this frame. */
    public static LineCache getCachedLines(PerfHudConfig.StatListConfig cfg) {
        return FRAME_CACHE.computeIfAbsent(cfg.id, id -> {
            List<String>  lines  = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            buildLines(cfg, lines, colors);
            return new LineCache(lines, colors);
        });
    }

    // ── Position ──────────────────────────────────────────────────────────────

    public static int[] getPosition(PerfHudConfig.StatListConfig cfg,
                                    int screenW, int screenH, int boxW, int boxH) {
        int x, y;
        switch (cfg.anchorCorner) {
            case TOP_RIGHT    -> { x = screenW - boxW - cfg.anchorDx; y = cfg.anchorDy; }
            case BOTTOM_LEFT  -> { x = cfg.anchorDx;                  y = screenH - boxH - cfg.anchorDy; }
            case BOTTOM_RIGHT -> { x = screenW - boxW - cfg.anchorDx; y = screenH - boxH - cfg.anchorDy; }
            default           -> { x = cfg.anchorDx;                  y = cfg.anchorDy; } // TOP_LEFT
        }
        // Snap overrides beat the corner anchor on the snapped axis
        int cx = screenW / 2;
        int cy = screenH / 2;
        x = switch (cfg.snapX) {
            case LEFT_ON_CENTER   -> cx;
            case CENTER_ON_CENTER -> cx - boxW / 2;
            case RIGHT_ON_CENTER  -> cx - boxW;
            default -> x;
        };
        y = switch (cfg.snapY) {
            case TOP_ON_CENTER    -> cy;
            case CENTER_ON_CENTER -> cy - boxH / 2;
            case BOTTOM_ON_CENTER -> cy - boxH;
            default -> y;
        };
        x = Math.max(0, Math.min(screenW - boxW, x));
        y = Math.max(0, Math.min(screenH - boxH, y));
        return new int[]{x, y};
    }

    // ── Line building ─────────────────────────────────────────────────────────

    public static void buildLines(PerfHudConfig.StatListConfig cfg,
                                  List<String> lines, List<Integer> colors) {
        for (PerfHudConfig.Stat stat : cfg.getVisibleStats()) {
            String text  = null;
            int    color = 0xFFFFFFFF;
            switch (stat) {
                case TPS   -> { text = PerfDataHolder.getFormattedTps();      color = PerfDataHolder.getTpsColor(); }
                case MSPT  -> { text = PerfDataHolder.getFormattedMspt(); /* empty on remote servers */ }
                case FPS   -> { text = PerfDataHolder.getFormattedFps();      color = PerfDataHolder.getFpsColor(); }
                case PING  -> { text = PerfDataHolder.getFormattedPing();     color = PerfDataHolder.getPingColor(); }
                case MEMORY-> { text = PerfDataHolder.getFormattedMem();      color = PerfDataHolder.getMemColor(); }
                case CPU   -> { text = PerfDataHolder.getFormattedCpu();      color = PerfDataHolder.getCpuColor(); }
                case ENTITIES         -> { text = PerfDataHolder.getFormattedEntities(); }
                case CHUNKS           -> { text = PerfDataHolder.getFormattedChunks(); }
                case RENDERED_SECTIONS-> { text = PerfDataHolder.getFormattedRendered(); }
                case COORDS           -> { text = PerfDataHolder.getFormattedCoords(); }
                case FACING           -> { text = PerfDataHolder.getFormattedFacing(); }
                case SPEED            -> { text = PerfDataHolder.getFormattedSpeed();   color = PerfDataHolder.getSpeedColor(); }
                case GC_TIME          -> { text = PerfDataHolder.getFormattedGcTime(); }
            }
            if (text == null || text.isEmpty()) continue;
            // Strip prefix ("Label: ") when showPrefix is disabled for this stat
            if (!cfg.getStatSettings(stat).showPrefix) {
                int sep = text.indexOf(": ");
                if (sep >= 0) text = text.substring(sep + 2);
            }
            lines.add(text);
            colors.add(color);
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.getDebugOverlay().showDebugScreen()) return;
        if (mc.getConnection() == null) return;
        if (mc.screen instanceof bottled.perfhud.gui.PerfHudGuiScreen) return;

        // Advance frame cache so getCachedLines() is fresh this frame
        tickCache();

        // ── Data collection ──────────────────────────────────────────────────
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            PerfDataHolder.mspt =
                    mc.getSingleplayerServer().getAverageTickTimeNanos() / 1_000_000.0f;
        } else {
            PerfDataHolder.mspt = -1f; // not available on remote servers
        }
        PerfDataHolder.fps = mc.getFps();

        ClientPacketListener conn = mc.getConnection();
        if (mc.player != null && conn != null) {
            PlayerInfo info = conn.getPlayerInfo(mc.player.getUUID());
            PerfDataHolder.ping = info != null ? info.getLatency() : -1;
        }

        if (mc.level != null) {
            PerfDataHolder.entityCount  = mc.level.getEntityCount();
            PerfDataHolder.loadedChunks = mc.level.getChunkSource().getLoadedChunksCount();
        }
        if (mc.player != null) {
            PerfDataHolder.playerX = mc.player.getX();
            PerfDataHolder.playerY = mc.player.getY();
            PerfDataHolder.playerZ = mc.player.getZ();
            // Direction enum: NORTH/SOUTH/EAST/WEST + intercardinals from yaw
            float yaw = ((mc.player.getYRot() % 360) + 360) % 360;
            if      (yaw <  22.5f)  PerfDataHolder.facingName = "S";
            else if (yaw <  67.5f)  PerfDataHolder.facingName = "SW";
            else if (yaw < 112.5f)  PerfDataHolder.facingName = "W";
            else if (yaw < 157.5f)  PerfDataHolder.facingName = "NW";
            else if (yaw < 202.5f)  PerfDataHolder.facingName = "N";
            else if (yaw < 247.5f)  PerfDataHolder.facingName = "NE";
            else if (yaw < 292.5f)  PerfDataHolder.facingName = "E";
            else if (yaw < 337.5f)  PerfDataHolder.facingName = "SE";
            else                     PerfDataHolder.facingName = "S";
            // Horizontal speed: delta movement is per-tick, × 20 = blocks/sec
            double dx = mc.player.getDeltaMovement().x;
            double dz = mc.player.getDeltaMovement().z;
            PerfDataHolder.speedBps = (float)(Math.sqrt(dx * dx + dz * dz) * 20.0);
        }
        if (mc.levelRenderer != null) {
            PerfDataHolder.renderedSections = mc.levelRenderer.countRenderedSections();
        }

        PerfDataHolder.updateFastMetrics();
        PerfDataHolder.updateSlowMetrics();

        // ── Render each stat list (uses frame cache) ─────────────────────────
        PerfHudConfig root = PerfHudConfig.getInstance();
        var font = mc.font;

        for (PerfHudConfig.StatListConfig listCfg : root.lists) {
            LineCache cache = getCachedLines(listCfg);
            List<String>  lines  = cache.lines();
            List<Integer> colors = cache.colors();
            if (lines.isEmpty()) continue;

            int boxW = cache.boxW(font);
            int boxH = cache.boxH(font);
            int lineH = font.lineHeight + 1;

            int[] pos = getPosition(listCfg, graphics.guiWidth(), graphics.guiHeight(), boxW, boxH);
            int x = pos[0], y = pos[1];

            if (listCfg.showBackground) {
                graphics.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0x90000000);
            }
            boolean shadow = listCfg.textShadow;
            for (int i = 0; i < lines.size(); i++) {
                graphics.text(font, lines.get(i), x + 2, y + 2 + i * lineH, colors.get(i), shadow);
            }
        }
    }
}
