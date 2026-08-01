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

    /**
     * One row that renders as a rolling graph instead of text.
     * <p>
     * {@code displayHistory} is the (possibly smoothed) series actually drawn;
     * {@code rawHistory} is the untouched ring-buffer snapshot, kept alongside
     * so per-segment coloring and peak markers can be computed consistently
     * with what's on screen without re-deriving smoothing twice. min/max are
     * the *scale* bounds (after auto-scale headroom or fixed min/max is
     * applied) — i.e. what 0% and 100% of the plot height represent, not
     * necessarily the raw data's own min/max (see peakMinIdx/peakMaxIdx for
     * the actual data extremes).
     */
    public record GraphEntry(MtssConfig.Stat stat, float[] rawHistory, float[] displayHistory,
                             int color, String label, String minValueLabel, String maxValueLabel,
                             float scaleMin, float scaleMax, int peakMinIdx, int peakMaxIdx,
                             MtssConfig.GraphStyle style) {}

    /** Which underlying list a given display row pulls from. */
    public enum RowKind { TEXT, GRAPH }

    /** Fallback box size used only for the empty-list placeholder measurements elsewhere; individual graphs size themselves from their own GraphStyle.width/height. */
    public static final int GRAPH_W = 80;
    public static final int GRAPH_H = 28;

    public record LineCache(List<String> lines, List<Integer> colors,
                            List<GraphEntry> graphEntries, List<RowKind> rowKinds) {

        public int boxW(net.minecraft.client.gui.Font font) {
            int textW = lines.stream().mapToInt(font::width).max().orElse(0);
            int graphW = graphEntries.isEmpty() ? 0 : graphEntries.stream()
                    .mapToInt(e -> Math.max(e.style().width, font.width(e.label()) + 4))
                    .max().orElse(GRAPH_W);
            return Math.max(textW, graphW) + 4;
        }

        public int boxH(net.minecraft.client.gui.Font font) {
            int lineH = font.lineHeight + 1;
            int h = 3;
            for (int i = 0, g = 0; i < rowKinds.size(); i++) {
                if (rowKinds.get(i) == RowKind.GRAPH) {
                    h += graphEntries.get(g).style().height + 1;
                    g++;
                } else {
                    h += lineH;
                }
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
                MtssConfig.StatSettings statSettings = cfg.getStatSettings(stat);
                int decimals = statSettings.decimals;
                MtssConfig.GraphStyle style = statSettings.graphStyle;

                float[] rawHistory = switch (stat) {
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
                if (rawHistory.length == 0) continue;

                // Smoothing is computed here, once per frame, from the raw buffer
                // into a separate display array — the ring buffer itself is never
                // mutated, so other consumers (and a future toggle back to raw)
                // always see untouched samples.
                float[] displayHistory = applySmoothing(rawHistory, style.smoothing);

                // Current-value color (used for CURRENT_THRESHOLD and as the
                // current-value label's color regardless of colorMode).
                int currentColor = switch (stat) {
                    case TPS   -> MtssDataHolder.getTpsColor();
                    case MSPT  -> MtssDataHolder.getTpsColor(); // MSPT has no dedicated color helper; TPS's threshold covers the same underlying tick-time signal
                    case FPS   -> MtssDataHolder.getFpsColor();
                    case CPU   -> MtssDataHolder.getCpuColor();
                    case PING  -> MtssDataHolder.getPingColor();
                    case MEMORY-> MtssDataHolder.getMemColor();
                    case SPEED -> MtssDataHolder.getSpeedColor();
                    default    -> 0xFFFFFFFF;
                };
                if (cfg.useCustomColor) currentColor = cfg.overrideColor;

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
                if (!statSettings.showPrefix) {
                    int sep = label.indexOf(": ");
                    if (sep >= 0) label = label.substring(sep + 2);
                }
                // MSPT (and, transitively here, nothing else) can go back to
                // unavailable mid-session — e.g. leaving a singleplayer world
                // — while its history buffer still holds old samples. Rather
                // than overlay a blank label, fall back to the current
                // (possibly stale) numeric value so something is always shown.
                if (label.isEmpty()) label = rawHistory[rawHistory.length - 1] + "";

                // ── Scale bounds: computed once here (not per-fill-call in
                // drawGraph) so auto-scale's O(buffer size) min/max scan and
                // fixed-mode's simple lookup both happen at most once per
                // frame per graph, regardless of how many pixels the plot is
                // wide. See the class-level note on frame-cache discipline.
                float scaleMin, scaleMax;
                if (style.autoScale) {
                    float rawMin = Float.MAX_VALUE, rawMax = -Float.MAX_VALUE;
                    for (float v : displayHistory) { if (v < rawMin) rawMin = v; if (v > rawMax) rawMax = v; }
                    float range = rawMax - rawMin;
                    // 10% headroom padding so the line doesn't touch the very
                    // top/bottom edge of the plot — with a flat/near-flat
                    // history (range ~0) fall back to a fixed +/-1 unit pad so
                    // headroom is never zero-width.
                    float pad = range > 1e-4f ? range * 0.10f : 1f;
                    scaleMin = rawMin - pad;
                    scaleMax = rawMax + pad;
                } else {
                    // Fixed mode: no recompute needed per frame — bounds are
                    // just the user's configured numbers, which is the whole
                    // perf/stability point of offering it (no rescale math,
                    // and the scale never jumps around while watching it).
                    scaleMin = style.fixedMin;
                    scaleMax = style.fixedMax;
                }
                if (scaleMax - scaleMin < 1e-4f) scaleMax = scaleMin + 1f; // guard divide-by-zero below

                // Peak/min marker indices — position of the highest and lowest
                // sample currently visible, computed on the display (smoothed)
                // series so the marker lines up with what's drawn on screen.
                int peakMinIdx = 0, peakMaxIdx = 0;
                for (int i = 1; i < displayHistory.length; i++) {
                    if (displayHistory[i] < displayHistory[peakMinIdx]) peakMinIdx = i;
                    if (displayHistory[i] > displayHistory[peakMaxIdx]) peakMaxIdx = i;
                }

                String minValueLabel = rawHistory.length >= 2 ? formatAxisValue(stat, displayHistory[peakMinIdx]) : "";
                String maxValueLabel = rawHistory.length >= 2 ? formatAxisValue(stat, displayHistory[peakMaxIdx]) : "";

                graphEntries.add(new GraphEntry(stat, rawHistory, displayHistory, currentColor, label,
                        minValueLabel, maxValueLabel, scaleMin, scaleMax, peakMinIdx, peakMaxIdx, style));
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

    /**
     * Simple moving average over the last {@code window} samples (inclusive of
     * the sample itself), computed into a new array — never mutates the input.
     * window &lt;= 1 returns the input array unchanged (no allocation) since
     * that's the "off/raw" case and the common default.
     * <p>
     * This runs once per graph per frame (called from buildLines, which itself
     * only runs once per frame per list courtesy of the frame cache), so an
     * O(n * window) smoothing pass here is negligible — it is NOT re-run per
     * pixel-column in drawGraph.
     */
    private static float[] applySmoothing(float[] raw, int window) {
        if (window <= 1 || raw.length < 2) return raw;
        float[] out = new float[raw.length];
        float sum = 0f;
        for (int i = 0; i < raw.length; i++) {
            sum += raw[i];
            if (i >= window) sum -= raw[i - window];
            int count = Math.min(i + 1, window);
            out[i] = sum / count;
        }
        return out;
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
        // than a graph's own configured width if a label like "Mem:
        // 8192/16384MB" needed the extra room) rather than staying fixed-width
        // and leaving dead space.
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
                GraphEntry entry = cache.graphEntries().get(graphIdx);
                drawGraph(graphics, font, entry, baseX, cursorY, contentW, entry.style().height);
                graphIdx++;
                cursorY += entry.style().height + 1;
            }
        }
    }

    /**
     * Renders a single rolling history graph as a layered mini perf-monitor
     * widget — panel background, gridlines, a 2-band gradient-faded area
     * fill with a brighter interpolated stroke along the trend line,
     * peak/min markers, and an optional value readout — using only
     * GuiGraphicsExtractor.fill(...)/outline(...)/text(...). No new
     * rendering dependency, and no per-frame matrix pushes for the plot
     * itself (see the perf note on drawPlotLine).
     * <p>
     * Every visual feature here is individually toggleable via {@code
     * entry.style()}; with an untouched (default) GraphStyle this reproduces
     * step 1's exact look: a bordered panel, a faint 50%-mark reference
     * line, a flat single-tone fill colored by the current value's
     * threshold, and a current-value-only label.
     */
    private static void drawGraph(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font,
                                  GraphEntry entry, int x, int y, int w, int h) {
        MtssConfig.GraphStyle style = entry.style();
        float[] display = entry.displayHistory();
        int n = display.length;

        // ── 1. Background panel ──────────────────────────────────────────
        // A subtly distinct fill/border behind the plot area itself (separate
        // from the outer list's own showBackground), so the graph still reads
        // as a discrete widget even when the list background is off.
        if (style.showPanelBackground) {
            graphics.fill(x, y, x + w, y + h, 0x30FFFFFF);
            graphics.outline(x, y, w, h, 0x60FFFFFF);
        } else {
            // Still frame the plot area even with no background fill, or the
            // gridlines/plot would float with no boundary at all.
            graphics.outline(x, y, w, h, 0x40FFFFFF);
        }

        int plotW = w - 2;   // inset 1px on each side to stay inside the border
        int plotH = h - 2;
        int plotX = x + 1;
        int plotY = y + 1;
        if (plotW <= 0 || plotH <= 0 || n == 0) return;

        // ── 2. Gridlines (drawn behind the data) ─────────────────────────
        // Low-contrast horizontal references at 25/50/75% of the current
        // scale so the plot recedes rather than competing with the line.
        // Below ~12px tall three gridlines would just be visual noise on top
        // of each other, so they're skipped at very small sizes regardless of
        // the toggle — see the height guard.
//        if (style.showGridlines && plotH >= 12) {
//            for (float frac : new float[]{0.25f, 0.5f, 0.75f}) {
//                int gy = plotY + Math.round(plotH * (1f - frac));
//                for (int dx = 0; dx < plotW; dx += 3) {
//                    graphics.fill(plotX + dx, gy, plotX + Math.min(dx + 1, plotW), gy + 1, 0x25FFFFFF);
//                }
//            }
//        }

        // ── 3 & 4. Filled area (gradient-faded) + interpolated stroke line
        // ─────────────────────────────────────────────────────────────────
        drawPlotLine(graphics, entry, style, display, n, plotX, plotY, plotW, plotH);

        // ── Peak/min markers ──────────────────────────────────────────────
        if (style.showPeakMarkers && n >= 2 && plotW >= 10) {
            float peakMaxTopY = lerpTopY(entry, n, plotH, entry.peakMaxIdx());
            float peakMinTopY = lerpTopY(entry, n, plotH, entry.peakMinIdx());
            drawPeakMarker(graphics, entry.peakMaxIdx(), n, plotX, plotY, plotW, peakMaxTopY, 0xFFFFFFFF);
            drawPeakMarker(graphics, entry.peakMinIdx(), n, plotX, plotY, plotW, peakMinTopY, 0xFFAAAAAA);
        }

        // ── Value readout ─────────────────────────────────────────────────
        String label = entry.label();
        if (style.valueDisplay != MtssConfig.GraphValueDisplay.NONE && !label.isEmpty()) {
            int labelW = font.width(label);
            graphics.fill(x + 1, y + 1, x + 1 + labelW + 2, y + 1 + font.lineHeight, 0x80000000);
            graphics.text(font, label, x + 2, y + 1, entry.color(), false);
        }
        if (style.valueDisplay == MtssConfig.GraphValueDisplay.MIN_CURRENT_MAX && n >= 2) {
            String minLabel = entry.minValueLabel();
            String maxLabel = entry.maxValueLabel();
            int axisY = y + h - font.lineHeight;
            graphics.text(font, minLabel, x + 2, axisY, 0xB0CCCCCC, false);
            int maxW = font.width(maxLabel);
            graphics.text(font, maxLabel, x + w - maxW - 2, axisY, 0xB0CCCCCC, false);
        }
    }

    private static void drawPlotLine(GuiGraphicsExtractor graphics, GraphEntry entry,
                                     MtssConfig.GraphStyle style, float[] display, int n,
                                     int plotX, int plotY, int plotW, int plotH) {
        if (plotW <= 0 || plotH <= 0) return;

        float scaleMin = entry.scaleMin(), scaleMax = entry.scaleMax();
        float range = Math.max(1e-4f, scaleMax - scaleMin);

        // Precompute each column's interpolated top-Y and nearest real
        // sample index once — shared by the fill and stroke passes below so
        // neither the interpolation nor the color lookup happens twice.
        int[] colTopY = new int[plotW];
        int[] colSampleIdx = new int[plotW];
        for (int col = 0; col < plotW; col++) {
            // Fractional position along the sample series this column
            // represents, spread evenly across [0, n-1] — interpolated
            // between the two nearest samples rather than snapped to
            // whichever single sample a plain integer division would hit.
            float t = (n <= 1) ? 0f : (col / (float) Math.max(1, plotW - 1)) * (n - 1);
            int i0 = (int) Math.floor(t);
            int i1 = Math.min(n - 1, i0 + 1);
            float frac = t - i0;
            float v = display[i0] + (display[i1] - display[i0]) * frac;
            float norm = Math.max(0f, Math.min(1f, (v - scaleMin) / range)); // 0..1, clamped
            int barH = Math.max(1, Math.round(norm * (plotH - 1)));
            colTopY[col] = plotY + plotH - barH;
            colSampleIdx[col] = Math.round(t);
        }

        int baseY = plotY + plotH; // exclusive bottom, shared by every column

        // ── Filled area (2-band gradient-faded) ──────────────────────────
        // Collapsed from 3 bands to 2 versus the original design: still
        // reads as a fade from more-opaque-near-the-line to more-
        // transparent-near-the-baseline, at 2/3 the fill() calls for this
        // pass. One fill() call per column when the bar is too short to
        // usefully split into two bands.
        for (int col = 0; col < plotW; col++) {
            int topY = colTopY[col];
            int colX = plotX + col;
            int fillColor = colorForColumn(entry, style, colSampleIdx[col]);

            int bandH = baseY - topY;
            if (bandH > 0) {
                int split = topY + Math.max(1, bandH / 2);
                graphics.fill(colX, topY, colX + 1, Math.min(split, baseY), withAlpha(fillColor, 0xC8));
                if (split < baseY) graphics.fill(colX, split, colX + 1, baseY, withAlpha(fillColor, 0x60));
            }
        }

        // ── Stroke: a brighter 1px cap tracing the top edge ──────────────
        // Second pass so it sits on top of neighboring columns' fills.
        for (int col = 0; col < plotW; col++) {
            int topY = colTopY[col];
            int colX = plotX + col;
            int fillColor = colorForColumn(entry, style, colSampleIdx[col]);
            graphics.fill(colX, topY, colX + 1, topY + 1, withAlpha(brighten(fillColor), 0xFF));
        }
    }

    private static float lerpTopY(GraphEntry entry, int n, int plotH, int sampleIdx) {
        float[] display = entry.displayHistory();
        float scaleMin = entry.scaleMin(), scaleMax = entry.scaleMax();
        float range = Math.max(1e-4f, scaleMax - scaleMin);
        float v = display[Math.max(0, Math.min(n - 1, sampleIdx))];
        float norm = Math.max(0f, Math.min(1f, (v - scaleMin) / range));
        float barH = Math.max(1f, norm * (plotH - 1));
        return plotH - barH;
    }

    private static void drawPeakMarker(GuiGraphicsExtractor graphics, int sampleIdx, int n,
                                       int plotX, int plotY, int plotW, float topYLocal, int color) {
        // Map the sample index to its horizontal screen position across the
        // plot width, matching the same even spread used elsewhere.
        float colF = (n <= 1) ? 0f : sampleIdx * (plotW - 1) / (float) Math.max(1, n - 1);
        int screenX = plotX + Math.max(0, Math.min(plotW - 1, Math.round(colF)));
        int topY = plotY + Math.round(topYLocal);
        int tickH = Math.min(4, Math.max(2, topY > plotY ? 3 : 2));
        graphics.fill(screenX, Math.max(0, topY - tickH), screenX + 1, topY, color);
    }

    private static int colorForColumn(GraphEntry entry, MtssConfig.GraphStyle style, int sampleIdx) {
        return switch (style.colorMode) {
            case CURRENT_THRESHOLD -> entry.color();
            case FIXED_ACCENT -> style.accentColor;
            case PER_SEGMENT_THRESHOLD -> thresholdColorForSample(entry.stat(), entry.displayHistory()[sampleIdx]);
            case GRADIENT -> gradientColorForValue(entry.displayHistory()[sampleIdx], entry.scaleMin(), entry.scaleMax());
        };
    }

    private static int thresholdColorForSample(MtssConfig.Stat stat, float value) {
        return switch (stat) {
            case TPS, MSPT -> MtssDataHolder.tpsColorFor(value);
            case FPS       -> MtssDataHolder.fpsColorFor(value);
            case PING      -> MtssDataHolder.pingColorFor(value);
            case MEMORY    -> MtssDataHolder.memColorForPercent(value);
            case CPU       -> MtssDataHolder.cpuColorFor(value);
            case SPEED     -> MtssDataHolder.speedColorFor(value);
            default        -> 0xFFFFFFFF;
        };
    }

    private static int gradientColorForValue(float value, float scaleMin, float scaleMax) {
        float range = scaleMax - scaleMin;
        float t = range > 1e-4f ? (value - scaleMin) / range : 0.5f;
        t = Math.max(0f, Math.min(1f, t));
        // 4-stop gradient: blue (0) -> green (1/3) -> yellow (2/3) -> red (1)
        int[][] stops = {{0x40, 0x80, 0xFF}, {0x55, 0xFF, 0x55}, {0xFF, 0xFF, 0x55}, {0xFF, 0x55, 0x55}};
        float scaled = t * (stops.length - 1);
        int idx = Math.min(stops.length - 2, (int) scaled);
        float localT = scaled - idx;
        int r = Math.round(stops[idx][0] + (stops[idx + 1][0] - stops[idx][0]) * localT);
        int g = Math.round(stops[idx][1] + (stops[idx + 1][1] - stops[idx][1]) * localT);
        int b = Math.round(stops[idx][2] + (stops[idx + 1][2] - stops[idx][2]) * localT);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Replaces an ARGB color's alpha channel, keeping RGB intact. */
    private static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    /** Lightens an RGB color's channels toward white by ~35%, used for the stroke line's brighter cap. */
    private static int brighten(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        r = Math.min(255, r + (255 - r) * 35 / 100);
        g = Math.min(255, g + (255 - g) * 35 / 100);
        b = Math.min(255, b + (255 - b) * 35 / 100);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static String formatAxisValue(MtssConfig.Stat stat, float value) {
        return switch (stat) {
            case TPS, MSPT, CPU, SPEED -> String.format("%.1f", value);
            case MEMORY -> Math.round(value) + "%";
            default -> Integer.toString(Math.round(value)); // FPS, Ping — whole units
        };
    }
}
