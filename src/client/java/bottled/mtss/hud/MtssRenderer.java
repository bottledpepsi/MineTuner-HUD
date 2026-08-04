package bottled.mtss.hud;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.sample.SamplingDriver;
import bottled.mtss.stat.StatDefinition;
import bottled.mtss.stat.StatRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public class MtssRenderer {

    // ── Per-frame line cache ──────────────────────────────────────────────────
    // buildLines runs from both the renderer and the GUI (drawList + getListBounds).
    // The cache avoids rebuilding the same strings twice in one frame.

    /**
     * One row that renders as a rolling graph instead of text.
     * <p>
     * {@code displayHistory} is the smoothed series actually drawn;
     * {@code rawHistory} is the raw buffer, kept alongside so coloring and
     * peak markers stay consistent with what's on screen. min/max are the
     * scale bounds (0%/100% of plot height), not necessarily the data's own
     * min/max — see peakMinIdx/peakMaxIdx for the actual extremes.
     */
    public record GraphEntry(MtssConfig.Stat stat, float[] rawHistory, float[] displayHistory,
                             int color, String label, String minValueLabel, String maxValueLabel,
                             float scaleMin, float scaleMax, int peakMinIdx, int peakMaxIdx,
                             MtssConfig.GraphStyle style, MtssConfig.ThresholdSettings threshold) {}

    /** Which underlying list a given display row pulls from. */
    public enum RowKind { TEXT, GRAPH }

    /** Fallback size for the empty-list placeholder; real graphs size themselves from GraphStyle.width/height. */
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
    /** Bumped by {@link #tickCache()} each render call. The GUI calls it too so both share the same frame budget. */
    private static long cacheGeneration = 0;
    private static long lastCacheGeneration = -1;

    /** Advances the cache generation — call once per frame. */
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
        // Template mode is a separate path, opt-in per list. Classic mode's
        // loop below is untouched either way.
        if (cfg.useTemplate) {
            buildTemplateLines(cfg, lines, colors, rowKinds);
            return;
        }
        buildClassicLines(cfg, lines, colors, graphEntries, rowKinds);
    }

    /**
     * Template mode: renders {@code cfg.templateLines} through
     * {@link TemplateEngine} instead of the classic per-Stat switch. Always
     * TEXT rows — a template line can mix multiple stats, so it doesn't map
     * to a single Stat the way a graph row does.
     * <p>
     * Every line renders in one flat color (the list's override color, or
     * white). Per-token inline coloring isn't built yet.
     */
    private static void buildTemplateLines(MtssConfig.StatListConfig cfg,
                                           List<String> lines, List<Integer> colors,
                                           List<RowKind> rowKinds) {
        int color = cfg.useCustomColor ? cfg.overrideColor : 0xFFFFFFFF;
        List<List<TemplateEngine.Token>> parsedLines = TemplateEngine.getParsedLines(cfg);
        for (List<TemplateEngine.Token> tokens : parsedLines) {
            lines.add(TemplateEngine.render(tokens));
            colors.add(color);
            rowKinds.add(RowKind.TEXT);
        }
    }

    /** Classic mode: unchanged per-Stat line building. */
    private static void buildClassicLines(MtssConfig.StatListConfig cfg,
                                  List<String> lines, List<Integer> colors,
                                  List<GraphEntry> graphEntries, List<RowKind> rowKinds) {
        for (MtssConfig.Stat stat : cfg.getVisibleStats()) {
            // Every stat's behavior comes from its StatDefinition now — no
            // per-stat switch here, so a new stat needs no changes in this file.
            StatDefinition def = StatRegistry.get(stat);

            // getStatSettings() lazily creates settings for any stat, so this
            // is safe even for stats with no settings entry written to disk.
            boolean asGraph = def.supportsGraph() && cfg.getStatSettings(stat).renderAsGraph;

            if (asGraph) {
                MtssConfig.StatSettings statSettings = cfg.getStatSettings(stat);
                int decimals = statSettings.decimals;
                MtssConfig.GraphStyle style = statSettings.graphStyle;

                float[] rawHistory = def.history();
                // Nothing to draw yet (unsupported CPU, remote-server MSPT, etc.) — same as text mode skipping unavailable stats.
                if (rawHistory.length == 0) continue;

                // Smoothed into a separate array each frame; the ring buffer itself is never mutated.
                float[] displayHistory = applySmoothing(rawHistory, style.smoothing);

                // Current-value color, also used for CURRENT_THRESHOLD mode and the label color.
                // Stats with no threshold of their own (MSPT/Speed) ignore the
                // custom argument or resolve it to a related stat's threshold —
                // see their StatDefinition for specifics.
                int currentColor = def.color(cfg.getThreshold(stat));
                // Per-list color override beats everything: override > per-stat threshold > default.
                if (cfg.useCustomColor) currentColor = cfg.overrideColor;

                // Same formatted string text mode would show, so switching a stat
                // between text and graph doesn't change how the number reads.
                String label = def.format(decimals);
                if (!statSettings.showPrefix) {
                    int sep = label.indexOf(": ");
                    if (sep >= 0) label = label.substring(sep + 2);
                }
                // MSPT can go unavailable mid-session (e.g. leaving singleplayer) while
                // its history still has old samples — fall back to the last raw value
                // instead of showing a blank label. Routed through formatAxisValue (the
                // same helper used for minValueLabel/maxValueLabel below) instead of
                // printing the raw float, so the fallback still respects the stat's
                // normal decimal formatting.
                if (label.isEmpty()) label = def.formatAxisValue(rawHistory[rawHistory.length - 1]);

                // Scale bounds computed once per frame here, not per pixel-column in drawGraph.
                float scaleMin, scaleMax;
                if (style.autoScale) {
                    float rawMin = Float.MAX_VALUE, rawMax = -Float.MAX_VALUE;
                    for (float v : displayHistory) { if (v < rawMin) rawMin = v; if (v > rawMax) rawMax = v; }
                    float range = rawMax - rawMin;
                    // 10% headroom so the line doesn't touch the plot edges; fixed +/-1 pad for a flat history.
                    float pad = range > 1e-4f ? range * 0.10f : 1f;
                    scaleMin = rawMin - pad;
                    scaleMax = rawMax + pad;
                } else {
                    // Fixed mode: just the user's configured numbers, no rescale math per frame.
                    scaleMin = style.fixedMin;
                    scaleMax = style.fixedMax;
                }
                if (scaleMax - scaleMin < 1e-4f) scaleMax = scaleMin + 1f; // guard divide-by-zero below

                // Highest/lowest visible sample, computed on the smoothed series so
                // the marker matches what's drawn.
                int peakMinIdx = 0, peakMaxIdx = 0;
                for (int i = 1; i < displayHistory.length; i++) {
                    if (displayHistory[i] < displayHistory[peakMinIdx]) peakMinIdx = i;
                    if (displayHistory[i] > displayHistory[peakMaxIdx]) peakMaxIdx = i;
                }

                String minValueLabel = rawHistory.length >= 2 ? def.formatAxisValue(displayHistory[peakMinIdx]) : "";
                String maxValueLabel = rawHistory.length >= 2 ? def.formatAxisValue(displayHistory[peakMaxIdx]) : "";

                // Same threshold as currentColor above, carried onto the entry so
                // PER_SEGMENT_THRESHOLD coloring uses the same cutoffs for every sample.
                MtssConfig.ThresholdSettings entryThreshold = def.supportsThreshold()
                        ? cfg.getThreshold(stat) : null;

                graphEntries.add(new GraphEntry(stat, rawHistory, displayHistory, currentColor, label,
                        minValueLabel, maxValueLabel, scaleMin, scaleMax, peakMinIdx, peakMaxIdx, style, entryThreshold));
                rowKinds.add(RowKind.GRAPH);
                continue;
            }

            // Classic text row: format + color both come straight from the
            // stat's own definition, so this block never needs a new case.
            int decimals = cfg.getStatSettings(stat).decimals;
            String text  = def.format(decimals);
            int    color = def.color(cfg.getThreshold(stat));
            if (text == null || text.isEmpty()) continue;
            // Strip the "Label: " prefix when showPrefix is off
            if (!cfg.getStatSettings(stat).showPrefix) {
                int sep = text.indexOf(": ");
                if (sep >= 0) text = text.substring(sep + 2);
            }
            // Per-list color override replaces threshold coloring
            if (cfg.useCustomColor) color = cfg.overrideColor;
            lines.add(text);
            colors.add(color);
            rowKinds.add(RowKind.TEXT);
        }
    }

    /**
     * Moving average over the last {@code window} samples, into a new array
     * (input is never mutated). window &lt;= 1 returns the input unchanged.
     * Runs once per graph per frame, not per pixel-column.
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

        // Advance the frame cache so getCachedLines() is fresh this frame
        tickCache();

        // ── Data collection ──────────────────────────────────────────────────
        // Each raw value is pulled in by its own StatSource (bottled.mtss.sample),
        // registered in SourceRegistry and run here at its declared cadence.
        // See the design doc for the full acquisition-side pipeline.
        SamplingDriver.sampleAll();

        // ── Render each stat list (uses frame cache) ─────────────────────────
        MtssConfig root = MtssConfig.getInstance();
        var font = mc.font;

        for (MtssConfig.StatListConfig listCfg : root.lists) {
            LineCache cache = getCachedLines(listCfg);
            if (cache.rowKinds().isEmpty()) continue;

            float scale = listCfg.textScale <= 0f ? 1f : listCfg.textScale;
            int unscaledW = cache.boxW(font);
            int unscaledH = cache.boxH(font);

            // Position math is in screen-pixel space, so use the scaled box size —
            // otherwise a scaled-up list could overlap the screen edge or other lists.
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
                // Translate to (x, y), scale, then draw at unscaled local offsets.
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
     * Draws every row (text or graph) in a list's cache, in statOrder,
     * starting at local offset (baseX, baseY) — either the final screen
     * position, or (0,0) inside an already-scaled matrix.
     * <p>
     * Public so {@code MtssGuiScreen.drawList} can reuse the same
     * row-drawing logic for the editor preview.
     */
    public static void drawRows(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font,
                                 LineCache cache, int baseX, int baseY, boolean shadow) {
        int lineH = font.lineHeight + 1;
        // Graphs stretch to the box's actual content width — which may be wider
        // than a graph's configured width if a label needed extra room.
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
     * Renders a rolling history graph — background panel, a gradient-faded
     * area fill with a stroke along the trend line, peak/min markers, and an
     * optional value readout — using only fill()/outline()/text(). Every
     * feature is toggleable via {@code entry.style()}; the default GraphStyle
     * reproduces a bordered panel, flat threshold-colored fill, and a
     * current-value label.
     */
    private static void drawGraph(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font,
                                  GraphEntry entry, int x, int y, int w, int h) {
        MtssConfig.GraphStyle style = entry.style();
        float[] display = entry.displayHistory();
        int n = display.length;

        // ── 1. Background panel ──────────────────────────────────────────
        // Separate from the list's own showBackground, so the graph still
        // reads as its own widget even when the list background is off.
        if (style.showPanelBackground) {
            graphics.fill(x, y, x + w, y + h, 0x30FFFFFF);
            graphics.outline(x, y, w, h, 0x60FFFFFF);
        } else {
            // Still frame the plot area, or the gridlines/plot would float with no border.
            graphics.outline(x, y, w, h, 0x40FFFFFF);
        }

        int plotW = w - 2;   // inset 1px on each side to stay inside the border
        int plotH = h - 2;
        int plotX = x + 1;
        int plotY = y + 1;
        if (plotW <= 0 || plotH <= 0 || n == 0) return;

        // ── 2. Gridlines (drawn behind the data) ──────────────────────────
        // Re-enabled: was left commented out with everything else in this
        // file already using graphics.fill() for pixel-level drawing, so
        // uncommenting (rather than removing) keeps this consistent with the
        // surrounding style instead of dropping the feature.
        // Skipped below ~12px tall — three gridlines would just be noise at small sizes.
        if (style.showGridlines && plotH >= 12) {
            for (float frac : new float[]{0.25f, 0.5f, 0.75f}) {
                int gy = plotY + Math.round(plotH * (1f - frac));
                for (int dx = 0; dx < plotW; dx += 3) {
                    graphics.fill(plotX + dx, gy, plotX + Math.min(dx + 1, plotW), gy + 1, 0x25FFFFFF);
                }
            }
        }

        // ── 3 & 4. Filled area (gradient-faded) + interpolated stroke line ──
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

        // Precompute each column's interpolated top-Y and nearest sample index
        // once, shared by the fill and stroke passes below.
        int[] colTopY = new int[plotW];
        int[] colSampleIdx = new int[plotW];
        for (int col = 0; col < plotW; col++) {
            // Fractional position along the sample series this column represents,
            // interpolated between the two nearest samples.
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

        // Precompute each column's fill color once, so the run-merging loops
        // below don't re-invoke the threshold/gradient lookup per column.
        int[] colColor = new int[plotW];
        for (int col = 0; col < plotW; col++) {
            colColor[col] = colorForColumn(entry, style, colSampleIdx[col]);
        }

        // ── Filled area (2-band gradient-faded) ──────────────────────────
        // Adjacent columns sharing the same (topY, color) — common, since
        // norm rounds to integer pixel heights and color only changes at
        // threshold boundaries — are merged into one wide fill() instead of
        // many single-pixel ones. This is the main cost of graph rendering:
        // with several graphs on screen, per-column fills add up fast.
        int runStart = 0;
        while (runStart < plotW) {
            int topY = colTopY[runStart];
            int fillColor = colColor[runStart];
            int runEnd = runStart + 1;
            while (runEnd < plotW && colTopY[runEnd] == topY && colColor[runEnd] == fillColor) {
                runEnd++;
            }
            int runX0 = plotX + runStart;
            int runX1 = plotX + runEnd;

            int bandH = baseY - topY;
            if (bandH > 0) {
                int split = topY + Math.max(1, bandH / 2);
                graphics.fill(runX0, topY, runX1, Math.min(split, baseY), withAlpha(fillColor, 0xC8));
                if (split < baseY) graphics.fill(runX0, split, runX1, baseY, withAlpha(fillColor, 0x60));
            }
            runStart = runEnd;
        }

        // ── Stroke: a brighter 1px cap along the top edge ─────────────────
        // Second pass so it draws on top of neighboring fills. Same run-merging.
        int[] strokeColor = new int[plotW];
        for (int col = 0; col < plotW; col++) {
            strokeColor[col] = withAlpha(brighten(colColor[col]), 0xFF);
        }
        runStart = 0;
        while (runStart < plotW) {
            int topY = colTopY[runStart];
            int color = strokeColor[runStart];
            int runEnd = runStart + 1;
            while (runEnd < plotW && colTopY[runEnd] == topY && strokeColor[runEnd] == color) {
                runEnd++;
            }
            graphics.fill(plotX + runStart, topY, plotX + runEnd, topY + 1, color);
            runStart = runEnd;
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
        // Map the sample index to its horizontal screen position, same spread as above.
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
            case PER_SEGMENT_THRESHOLD -> StatRegistry.get(entry.stat())
                    .colorFor(entry.displayHistory()[sampleIdx], entry.threshold());
            case GRADIENT -> gradientColorForValue(entry.displayHistory()[sampleIdx], entry.scaleMin(), entry.scaleMax());
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
}
