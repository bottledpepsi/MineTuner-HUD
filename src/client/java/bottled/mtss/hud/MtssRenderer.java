package bottled.mtss.hud;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayList;
import java.util.List;

public class MtssRenderer {

    // ── Per-frame line cache ──────────────────────────────────────────────────
    // buildLines is called both here (render) and in the GUI (drawList + getListBounds).
    // The cache avoids redundant string building within the same frame.

    /** One row that renders as a rolling graph instead of text. */
    public record GraphEntry(MtssConfig.Stat stat, float[] history, int color,
                             String label, float min, float max) {}

    /** Which underlying list a given display row pulls from. */
    public enum RowKind { TEXT, GRAPH }

    public static final int GRAPH_W = 80;
    public static final int GRAPH_H = 28;

    public record LineCache(List<String> lines, List<Integer> colors,
                            List<GraphEntry> graphEntries, List<RowKind> rowKinds) {

        public int boxW(net.minecraft.client.gui.Font font) {
            int textW = lines.stream().mapToInt(font::width).max().orElse(0);
            int graphW = graphEntries.isEmpty() ? 0 : graphEntries.stream()
                    .mapToInt(e -> Math.max(GRAPH_W, font.width(e.label()) + 4))
                    .max().orElse(GRAPH_W);
            return Math.max(textW, graphW) + 4;
        }

        public int boxH(net.minecraft.client.gui.Font font) {
            int lineH = font.lineHeight + 1;
            int h = 3;
            for (RowKind k : rowKinds) {
                h += (k == RowKind.GRAPH) ? (GRAPH_H + 1) : lineH;
            }
            return h;
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
    public static LineCache getCachedLines(MtssConfig.StatListConfig cfg) {
        return FRAME_CACHE.computeIfAbsent(cfg.id, id -> {
            List<String>     lines        = new ArrayList<>();
            List<Integer>    colors       = new ArrayList<>();
            List<GraphEntry> graphEntries = new ArrayList<>();
            List<RowKind>    rowKinds     = new ArrayList<>();
            buildLines(cfg, lines, colors, graphEntries, rowKinds);
            return new LineCache(lines, colors, graphEntries, rowKinds);
        });
    }

    // ── Position ──────────────────────────────────────────────────────────────

    public static int[] getPosition(MtssConfig.StatListConfig cfg,
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

    public static void buildLines(MtssConfig.StatListConfig cfg,
                                  List<String> lines, List<Integer> colors,
                                  List<GraphEntry> graphEntries, List<RowKind> rowKinds) {
        for (MtssConfig.Stat stat : cfg.getVisibleStats()) {
            // Graph mode only applies to the graphable stats; getStatSettings()
            // lazily creates settings for any stat, so this is safe even for
            // stats that have never had a settings entry written to disk.
            boolean asGraph = MtssConfig.GRAPHABLE_STATS.contains(stat)
                    && cfg.getStatSettings(stat).renderAsGraph;

            if (asGraph) {
                int decimals = cfg.getStatSettings(stat).decimals;
                float[] history = switch (stat) {
                    case TPS   -> MtssDataHolder.getTpsHistory();
                    case MSPT  -> MtssDataHolder.getMsptHistory();
                    case FPS   -> MtssDataHolder.getFpsHistory();
                    case CPU   -> MtssDataHolder.getCpuHistory();
                    case PING  -> MtssDataHolder.getPingHistory();
                    case MEMORY-> MtssDataHolder.getMemHistory();
                    case SPEED -> MtssDataHolder.getSpeedHistory();
                    default    -> new float[0]; // unreachable — guarded by GRAPHABLE_STATS above
                };
                // Skip entirely if there's nothing to draw yet (e.g. CPU unsupported,
                // or MSPT/Ping/Memory never sampled because remote-server/disconnected/
                // heap-not-yet-read) — matches the text-mode behavior of skipping
                // empty/unavailable stats.
                if (history.length == 0) continue;

                int color = switch (stat) {
                    case TPS   -> MtssDataHolder.getTpsColor();
                    case MSPT  -> MtssDataHolder.getTpsColor(); // MSPT has no dedicated color helper; TPS's threshold covers the same underlying tick-time signal
                    case FPS   -> MtssDataHolder.getFpsColor();
                    case CPU   -> MtssDataHolder.getCpuColor();
                    case PING  -> MtssDataHolder.getPingColor();
                    case MEMORY-> MtssDataHolder.getMemColor();
                    case SPEED -> MtssDataHolder.getSpeedColor();
                    default    -> 0xFFFFFFFF;
                };
                if (cfg.useCustomColor) color = cfg.overrideColor;

                // Current-value label uses the same formatted string (and the
                // same decimals/showPrefix settings) as text mode would, so
                // switching a stat between text and graph doesn't change how
                // its number reads — just how it's presented. Memory graphs
                // the used/max percentage (see MtssDataHolder), but the label
                // still shows the familiar "Mem: used/maxMB" text so the
                // actual megabyte figures aren't lost.
                String label = switch (stat) {
                    case TPS   -> MtssDataHolder.getFormattedTps(decimals);
                    case MSPT  -> MtssDataHolder.getFormattedMspt(decimals);
                    case FPS   -> MtssDataHolder.getFormattedFps();
                    case PING  -> MtssDataHolder.getFormattedPing();
                    case CPU   -> MtssDataHolder.getFormattedCpu(decimals);
                    case MEMORY-> MtssDataHolder.getFormattedMem();
                    case SPEED -> MtssDataHolder.getFormattedSpeed(decimals);
                    default    -> "";
                };
                if (!cfg.getStatSettings(stat).showPrefix) {
                    int sep = label.indexOf(": ");
                    if (sep >= 0) label = label.substring(sep + 2);
                }
                // MSPT (and, transitively here, nothing else) can go back to
                // unavailable mid-session — e.g. leaving a singleplayer world
                // — while its history buffer still holds old samples. Rather
                // than overlay a blank label, fall back to the current
                // (possibly stale) numeric value so something is always shown.
                if (label.isEmpty()) label = history[history.length - 1] + "";

                float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
                for (float v : history) { if (v < min) min = v; if (v > max) max = v; }

                graphEntries.add(new GraphEntry(stat, history, color, label, min, max));
                rowKinds.add(RowKind.GRAPH);
                continue;
            }

            String text  = null;
            int    color = 0xFFFFFFFF;
            int    decimals = cfg.getStatSettings(stat).decimals;
            switch (stat) {
                case TPS   -> { text = MtssDataHolder.getFormattedTps(decimals);   color = MtssDataHolder.getTpsColor(); }
                case MSPT  -> { text = MtssDataHolder.getFormattedMspt(decimals); /* empty on remote servers */ }
                case FPS   -> { text = MtssDataHolder.getFormattedFps();      color = MtssDataHolder.getFpsColor(); }
                case PING  -> { text = MtssDataHolder.getFormattedPing();     color = MtssDataHolder.getPingColor(); }
                case MEMORY-> { text = MtssDataHolder.getFormattedMem();      color = MtssDataHolder.getMemColor(); }
                case CPU   -> { text = MtssDataHolder.getFormattedCpu(decimals);   color = MtssDataHolder.getCpuColor(); }
                case ENTITIES         -> { text = MtssDataHolder.getFormattedEntities(); }
                case CHUNKS           -> { text = MtssDataHolder.getFormattedChunks(); }
                case RENDERED_SECTIONS-> { text = MtssDataHolder.getFormattedRendered(); }
                case COORDS           -> { text = MtssDataHolder.getFormattedCoords(); }
                case FACING           -> { text = MtssDataHolder.getFormattedFacing(); }
                case SPEED            -> { text = MtssDataHolder.getFormattedSpeed(decimals); color = MtssDataHolder.getSpeedColor(); }
                case GC_TIME          -> { text = MtssDataHolder.getFormattedGcTime(); }
                case BIOME            -> { text = MtssDataHolder.getFormattedBiome(); }
                case LIGHT_LEVEL      -> { text = MtssDataHolder.getFormattedLight(); }
                case DIMENSION        -> { text = MtssDataHolder.getFormattedDimension(); }
            }
            if (text == null || text.isEmpty()) continue;
            // Strip prefix ("Label: ") when showPrefix is disabled for this stat
            if (!cfg.getStatSettings(stat).showPrefix) {
                int sep = text.indexOf(": ");
                if (sep >= 0) text = text.substring(sep + 2);
            }
            // Per-list color override replaces threshold coloring entirely
            if (cfg.useCustomColor) color = cfg.overrideColor;
            lines.add(text);
            colors.add(color);
            rowKinds.add(RowKind.TEXT);
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();

        if (!MtssConfig.getInstance().overlayEnabled) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;
        if (mc.getConnection() == null) return;
        if (mc.gui.screen() instanceof bottled.mtss.gui.MtssGuiScreen) return;

        // Advance frame cache so getCachedLines() is fresh this frame
        tickCache();

        // ── Data collection ──────────────────────────────────────────────────
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            MtssDataHolder.mspt =
                    mc.getSingleplayerServer().getAverageTickTimeNanos() / 1_000_000.0f;
        } else {
            MtssDataHolder.mspt = -1f; // not available on remote servers
        }
        MtssDataHolder.fps = mc.getFps();

        ClientPacketListener conn = mc.getConnection();
        if (mc.player != null && conn != null) {
            PlayerInfo info = conn.getPlayerInfo(mc.player.getUUID());
            MtssDataHolder.ping = info != null ? info.getLatency() : -1;
        }

        if (mc.level != null) {
            MtssDataHolder.entityCount  = mc.level.getEntityCount();
            MtssDataHolder.loadedChunks = mc.level.getChunkSource().getLoadedChunksCount();
            MtssDataHolder.dimensionName = mc.level.dimension().identifier().getPath();
        }
        if (mc.player != null) {
            MtssDataHolder.playerX = mc.player.getX();
            MtssDataHolder.playerY = mc.player.getY();
            MtssDataHolder.playerZ = mc.player.getZ();
            if (mc.level != null) {
                net.minecraft.core.BlockPos pos = mc.player.blockPosition();
                MtssDataHolder.lightLevel = mc.level.getMaxLocalRawBrightness(pos);
                var biomeHolder = mc.level.getBiome(pos);
                MtssDataHolder.biomeName = biomeHolder.unwrapKey()
                        .map(key -> key.identifier().getPath())
                        .orElse("?");
            }
            // Direction enum: NORTH/SOUTH/EAST/WEST + intercardinals from yaw
            float yaw = ((mc.player.getYRot() % 360) + 360) % 360;
            if      (yaw <  22.5f)  MtssDataHolder.facingName = "S";
            else if (yaw <  67.5f)  MtssDataHolder.facingName = "SW";
            else if (yaw < 112.5f)  MtssDataHolder.facingName = "W";
            else if (yaw < 157.5f)  MtssDataHolder.facingName = "NW";
            else if (yaw < 202.5f)  MtssDataHolder.facingName = "N";
            else if (yaw < 247.5f)  MtssDataHolder.facingName = "NE";
            else if (yaw < 292.5f)  MtssDataHolder.facingName = "E";
            else if (yaw < 337.5f)  MtssDataHolder.facingName = "SE";
            else                     MtssDataHolder.facingName = "S";
            // Horizontal speed: delta movement is per-tick, × 20 = blocks/sec
            double dx = mc.player.getDeltaMovement().x;
            double dz = mc.player.getDeltaMovement().z;
            MtssDataHolder.speedBps = (float)(Math.sqrt(dx * dx + dz * dz) * 20.0);
        }
        if (mc.levelRenderer != null) {
            MtssDataHolder.renderedSections = mc.levelExtractor.countRenderedSections();
        }

        MtssDataHolder.updateFastMetrics();
        MtssDataHolder.updateSlowMetrics();

        // ── Render each stat list (uses frame cache) ─────────────────────────
        MtssConfig root = MtssConfig.getInstance();
        var font = mc.font;

        for (MtssConfig.StatListConfig listCfg : root.lists) {
            LineCache cache = getCachedLines(listCfg);
            if (cache.rowKinds().isEmpty()) continue;

            float scale = listCfg.textScale <= 0f ? 1f : listCfg.textScale;
            int unscaledW = cache.boxW(font);
            int unscaledH = cache.boxH(font);

            // Position math happens in screen-pixel space, so use the scaled box size
            // for layout — otherwise a scaled-up list could overlap the screen edge or
            // other lists at its anchor point.
            int boxW = Math.round(unscaledW * scale);
            int boxH = Math.round(unscaledH * scale);

            int[] pos = getPosition(listCfg, graphics.guiWidth(), graphics.guiHeight(), boxW, boxH);
            int x = pos[0], y = pos[1];

            if (listCfg.showBackground) {
                graphics.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0x90000000);
            }
            boolean shadow = listCfg.textShadow;

            if (scale == 1f) {
                drawRows(graphics, font, cache, x + 2, y + 2, shadow);
            } else {
                // Scale around the box's top-left corner: translate to (x, y) in screen space,
                // scale, then draw at the unscaled local offsets.
                var matrices = graphics.pose();
                matrices.pushMatrix();
                matrices.translate(x, y);
                matrices.scale(scale, scale);
                drawRows(graphics, font, cache, 0, 0, shadow);
                matrices.popMatrix();
            }
        }
    }

    /**
     * Draws every row (text line or graph) in a list's cache, in original
     * statOrder order, starting at local offset (baseX, baseY) — which is
     * either the final screen position (unscaled path) or (0,0) inside an
     * already-translated+scaled matrix (scaled path).
     * <p>
     * Public so {@code MtssGuiScreen.drawList} can reuse the exact same
     * row-drawing logic (text + graph interleaving) for the editor preview,
     * instead of re-implementing it against a hand-rolled lines-only loop.
     */
    public static void drawRows(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font,
                                 LineCache cache, int baseX, int baseY, boolean shadow) {
        int lineH = font.lineHeight + 1;
        // Graphs stretch to the box's actual content width (which may be wider
        // than GRAPH_W if a label like "Mem: 8192/16384MB" needed the extra
        // room) rather than staying fixed-width and leaving dead space.
        int contentW = cache.boxW(font) - 4;
        int textIdx = 0, graphIdx = 0;
        int cursorY = baseY;
        for (RowKind kind : cache.rowKinds()) {
            if (kind == RowKind.TEXT) {
                graphics.text(font, cache.lines().get(textIdx), baseX, cursorY,
                        cache.colors().get(textIdx), shadow);
                textIdx++;
                cursorY += lineH;
            } else {
                drawGraph(graphics, font, cache.graphEntries().get(graphIdx), baseX, cursorY, contentW);
                graphIdx++;
                cursorY += GRAPH_H + 1;
            }
        }
    }

    /**
     * Renders a single rolling history graph as a filled area chart, using only
     * GuiGraphicsExtractor.fill(...)/text(...) — no new rendering dependency.
     * Values are normalized against the min/max of the visible history so the
     * graph is always legible regardless of the stat's absolute scale (e.g.
     * TPS 0-20 vs Ping 0-300ms). A flat/near-flat history still renders a thin
     * baseline strip rather than collapsing to nothing.
     * <p>
     * Design: a 1px border frames the plot area so it doesn't blend into the
     * list's background; a horizontal dotted midline marks the 50% level as a
     * visual reference; the current formatted value is overlaid top-left, and
     * the window's min/max are overlaid bottom-left/bottom-right in a dim
     * gray so the numbers that gave the shape meaning aren't left implicit.
     */
    private static void drawGraph(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font,
                                  GraphEntry entry, int x, int y, int w) {
        float[] history = entry.history();
        int color = entry.color();

        // Background + border so the plot area reads as a distinct widget
        // rather than a loose cluster of bars floating on the list background.
        graphics.fill(x, y, x + w, y + GRAPH_H, 0x30FFFFFF);
        graphics.outline(x, y, w, GRAPH_H, 0x60FFFFFF);

        // Dotted midline at the 50% mark — a fixed visual reference so two
        // glances at the graph can tell "trending up" from "trending down"
        // without having to read the numbers first.
        int midY = y + GRAPH_H / 2;
        for (int dx = 1; dx < w - 1; dx += 3) {
            graphics.fill(x + dx, midY, x + dx + 1, midY + 1, 0x30FFFFFF);
        }

        if (history.length >= 2) {
            float min = entry.min(), max = entry.max();
            float range = max - min;
            boolean flat = range < 1e-4f;
            int n = history.length;
            int plotW = w - 2; // inset 1px on each side to stay inside the border
            for (int col = 0; col < plotW; col++) {
                // Sample index this column represents, spread evenly across history.
                int sampleIdx = (int) ((long) col * (n - 1) / Math.max(1, plotW - 1));
                float v = history[sampleIdx];
                float norm = flat ? 0.5f : (v - min) / range; // 0..1
                int barH = Math.max(1, Math.round(norm * (GRAPH_H - 3)));
                int colX = x + 1 + col;
                int barTop = y + GRAPH_H - 1 - barH;
                graphics.fill(colX, barTop, colX + 1, y + GRAPH_H - 1, color);
            }
        }

        // Current-value label, top-left, with a translucent backing strip so
        // it stays legible over bars of the same brightness.
        String label = entry.label();
        if (!label.isEmpty()) {
            int labelW = font.width(label);
            graphics.fill(x + 1, y + 1, x + 1 + labelW + 2, y + 1 + font.lineHeight, 0x80000000);
            graphics.text(font, label, x + 2, y + 1, color, false);
        }

        // Min/max of the visible window, bottom corners, dim so they read as
        // axis labels rather than competing with the current-value label.
        if (history.length >= 2) {
            String minLabel = formatAxisValue(entry.stat(), entry.min());
            String maxLabel = formatAxisValue(entry.stat(), entry.max());
            int axisY = y + GRAPH_H - font.lineHeight;
            graphics.text(font, minLabel, x + 2, axisY, 0xB0CCCCCC, false);
            int maxW = font.width(maxLabel);
            graphics.text(font, maxLabel, x + w - maxW - 2, axisY, 0xB0CCCCCC, false);
        }
    }

    /**
     * Compact numeric-only formatting for the graph's min/max axis labels —
     * deliberately not the full "Label: value unit" string (that's what the
     * current-value overlay is for); just enough precision to read the
     * window's range at a glance.
     */
    private static String formatAxisValue(MtssConfig.Stat stat, float value) {
        return switch (stat) {
            case TPS, MSPT, CPU, SPEED -> String.format("%.1f", value);
            case MEMORY -> Math.round(value) + "%";
            default -> Integer.toString(Math.round(value)); // FPS, Ping — whole units
        };
    }
}
