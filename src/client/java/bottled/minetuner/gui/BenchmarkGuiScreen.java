package bottled.minetuner.gui;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.benchmark.BenchmarkSession;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public class BenchmarkGuiScreen extends Screen {

    // --- Layout constants ---
    private static final int MARGIN = 16;
    private static final int HEADER_H = 28;
    private static final int BUTTON_H = 20;
    private static final int BUTTON_GAP = 8;
    private static final int SECTION_GAP = 10;
    private static final int RESULT_ROW_H = 16;

//    private static final int LIVE_GRAPH_H = 46;

    private static final int MIN_RESULT_GRAPH_H = 40;

    private static final int GRAPH_LABEL_H = 12;
    private static final int GRAPH_GAP = 6;

    private static final int COL_BG = 0xEE111111;
    private static final int COL_BORDER = 0xFFFFAA00;
    private static final int COL_BORDER_DIM = 0x60FFAA00;
    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_TEXT_DIM = 0xFFAAAAAA;
    private static final int COL_BTN_BG = 0x30FFFFFF;
    private static final int COL_BTN_BG_HOVER = 0x55FFFFFF;
    private static final int COL_BTN_BG_DISABLED = 0x18FFFFFF;
    private static final int COL_RECORDING = 0xFFFF5555;
    private static final int COL_STOPPED = 0xFF55FF55;
    private static final int COL_IDLE = 0xFFAAAAAA;
    private static final int COL_FRAMETIME_LINE = 0xFF55FFAA;
    private static final int COL_CPU_LINE = 0xFF55AAFF;
    private static final int COL_GPU_LINE = 0xFFFFAA55;

    private int startBtnX, startBtnY, startBtnW, startBtnH;
    private int stopBtnX, stopBtnY, stopBtnW, stopBtnH;
    private int saveBtnX, saveBtnY, saveBtnW, saveBtnH;

    private static final long SAVE_MESSAGE_DURATION_MS = 3000;
    private String saveMessage = null;
    private int saveMessageColor = COL_TEXT_DIM;
    private long saveMessageShownAtMs = 0L;

    public BenchmarkGuiScreen() {
        super(Component.translatable("gui.minetuner.benchmark.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x80000000);

        int contentX = MARGIN;
        int contentW = Math.max(120, width - MARGIN * 2);

        int y = MARGIN;
        y = drawHeader(g, contentX, y, contentW);
        y += SECTION_GAP;
        y = drawStateAndControls(g, mx, my, contentX, y, contentW);
        y += SECTION_GAP;

        BenchmarkSession.State state = BenchmarkSession.state();
        if (state == BenchmarkSession.State.RECORDING) {
            drawLiveSection(g, contentX, y, contentW);
        } else if (state == BenchmarkSession.State.STOPPED) {
            drawResultsSection(g, contentX, y, contentW);
        } else {
            drawEmptyState(g, contentX, y, contentW);
        }

//        g.centeredText(font, "§7" + I18n.get("gui.minetuner.benchmark.hint"),
//                width / 2, height - 14, COL_TEXT_DIM);

        super.extractRenderState(g, mx, my, partial);
    }

    // --- Sections ---

    private int drawHeader(GuiGraphicsExtractor g, int x, int y, int w) {
        g.text(font, "§e§l" + I18n.get("gui.minetuner.benchmark.title"), x, y, COL_TEXT, false);
        return y + HEADER_H;
    }

    private int drawStateAndControls(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w) {
        BenchmarkSession.State state = BenchmarkSession.state();

        String stateLabel = switch (state) {
            case IDLE -> I18n.get("gui.minetuner.benchmark.state.idle");
            case RECORDING -> I18n.get("gui.minetuner.benchmark.state.recording");
            case STOPPED -> I18n.get("gui.minetuner.benchmark.state.stopped");
        };
        int stateColor = switch (state) {
            case IDLE -> COL_IDLE;
            case RECORDING -> COL_RECORDING;
            case STOPPED -> COL_STOPPED;
        };
        // Small state-color dot, then the label — keeps the current state readable at a
        // glance even for a player who doesn't stop to read the word itself.
        g.fill(x, y + 3, x + 6, y + 9, stateColor);
        g.text(font, I18n.get("gui.minetuner.benchmark.state_label"), x + 10, y, COL_TEXT, false);
        int labelW = font.width(I18n.get("gui.minetuner.benchmark.state_label")) + 4;
        g.text(font, stateLabel, x + 10 + labelW, y, stateColor, false);

        y += font.lineHeight + 6;

        // Three side-by-side buttons (Start/Reset, Stop/Freeze, Save), evenly split across
        // the content width. Save is new: it's how "save every benchmark to a folder"
        // actually gets triggered, sitting alongside Start/Stop rather than auto-saving on
        // every Stop press, so a player can Stop, glance at the graphs, and only Save the
        // runs actually worth keeping.
        int btnW = (w - BUTTON_GAP * 2) / 3;
        startBtnX = x;
        startBtnY = y;
        startBtnW = btnW;
        startBtnH = BUTTON_H;
        stopBtnX = x + btnW + BUTTON_GAP;
        stopBtnY = y;
        stopBtnW = btnW;
        stopBtnH = BUTTON_H;
        saveBtnX = x + (btnW + BUTTON_GAP) * 2;
        saveBtnY = y;
        saveBtnW = w - (btnW + BUTTON_GAP) * 2;
        saveBtnH = BUTTON_H;

        // Start/Reset is always actionable (it's how you begin OR discard-and-restart —
        // see BenchmarkSession#start()'s doc), so it's never disabled. Stop/Freeze and Save
        // only do something in their respective states.
        boolean stopEnabled = state == BenchmarkSession.State.RECORDING;
        boolean saveEnabled = state == BenchmarkSession.State.STOPPED;

        String startLabel = state == BenchmarkSession.State.STOPPED
                ? I18n.get("gui.minetuner.benchmark.button.reset")
                : I18n.get("gui.minetuner.benchmark.button.start");
        drawButton(g, mx, my, startBtnX, startBtnY, startBtnW, startBtnH, startLabel, true);
        drawButton(g, mx, my, stopBtnX, stopBtnY, stopBtnW, stopBtnH,
                I18n.get("gui.minetuner.benchmark.button.stop"), stopEnabled);
        drawButton(g, mx, my, saveBtnX, saveBtnY, saveBtnW, saveBtnH,
                I18n.get("gui.minetuner.benchmark.button.save"), saveEnabled);

        y += BUTTON_H;

        // Confirmation message under the button row, auto-hidden after
        // SAVE_MESSAGE_DURATION_MS — see the field doc for why this doesn't stay up forever.
        if (saveMessage != null) {
            if (System.currentTimeMillis() - saveMessageShownAtMs > SAVE_MESSAGE_DURATION_MS) {
                saveMessage = null;
            } else {
                g.centeredText(font, saveMessage, x + w / 2, y + 4, saveMessageColor); // Save text position
            }
        }

        return y;
    }

    private void drawLiveSection(GuiGraphicsExtractor g, int x, int y, int w) {
        int panelH = RESULT_ROW_H * 2; // Recording now panel size
        drawPanelBackground(g, x, y, w, panelH, true);

        int innerX = x + 6;
        int innerY = y + 6;

        g.text(font, "§c" + I18n.get("gui.minetuner.benchmark.recording_now"), innerX, innerY, COL_TEXT, false);

        String elapsed = formatDuration(BenchmarkSession.elapsedNanos());
        String samples = Integer.toString(BenchmarkSession.liveSampleCount());
        String durationLine = I18n.get("gui.minetuner.benchmark.duration", elapsed);
        String sampleLine = I18n.get("gui.minetuner.benchmark.samples", samples);

        g.text(font, "§f§l" + durationLine, innerX, innerY + font.lineHeight + 4, COL_TEXT, false);
        int sampleLineW = font.width(sampleLine);
        g.text(font, "§7" + sampleLine, x + w - sampleLineW - 6, innerY + font.lineHeight + 4, COL_TEXT_DIM, false);

//        int graphY = innerY + font.lineHeight * 2 + 8;
//        drawFrametimeGraph(g, x + 6, graphY, w - 12, LIVE_GRAPH_H,
//                MineTunerDataHolder.getRawFrametimeHistory(), COL_RECORDING,
//                I18n.get("gui.minetuner.benchmark.graph.fps"));
    }

    private void drawResultsSection(GuiGraphicsExtractor g, int x, int y, int w) {
        g.text(font, "§a§l" + I18n.get("gui.minetuner.benchmark.latest_result"), x, y, COL_TEXT, false);
        y += font.lineHeight + 4;

        int graphsH = graphsSectionHeight();
        int panelH = RESULT_ROW_H * 3 + graphsH + 28; // Background panel size
        drawPanelBackground(g, x, y, w, panelH, false);

        int innerX = x + 6;
        int innerY = y + 6;

        String duration = formatDuration(BenchmarkSession.finalDurationNanos());
        String samples = Integer.toString(BenchmarkSession.finalSampleCount());
        g.text(font, I18n.get("gui.minetuner.benchmark.duration", duration), innerX, innerY, COL_TEXT, false);
        String sampleLine = I18n.get("gui.minetuner.benchmark.samples", samples);
        int sampleLineW = font.width(sampleLine);
        g.text(font, "§7" + sampleLine, x + w - sampleLineW - 6, innerY, COL_TEXT_DIM, false);
        innerY += RESULT_ROW_H;

        // Three-up stat readout: Avg FPS / 1% Low / 0.1% Low, each its own column, each
        // colored via MineTunerDataHolder's existing FPS color thresholds (the same
        // 60/30 good/warn bands the live HUD stats use) so a benchmark result reads with
        // the same "green/yellow/red" language as the rest of the mod's overlay.
        int colW = w / 3;
        drawStatColumn(g, innerX, innerY, colW,
                I18n.get("gui.minetuner.benchmark.avg_fps"),
                formatFpsValue(BenchmarkSession.finalAvgFps()),
                MineTunerDataHolder.fpsColorFor(BenchmarkSession.finalAvgFps()));
        drawStatColumn(g, innerX + colW, innerY, colW,
                I18n.get("gui.minetuner.benchmark.one_pct_low"),
                formatPercentileValue(BenchmarkSession.final1LowFps()),
                percentileColor(BenchmarkSession.final1LowFps()));
        drawStatColumn(g, innerX + colW * 2, innerY, w - colW * 2,
                I18n.get("gui.minetuner.benchmark.point_one_pct_low"),
                formatPercentileValue(BenchmarkSession.final01LowFps()),
                percentileColor(BenchmarkSession.final01LowFps()));
        innerY += RESULT_ROW_H * 2;

        // CPU gets its own row below the three-up FPS columns — a different unit/scale
        // (percent, not FPS) reads awkwardly crammed into the same three-column layout,
        // and it's explicitly called out as its own line item ("Final Benchmark CPU").
        double avgCpu = BenchmarkSession.finalAvgCpuPercent();
        String cpuValue = avgCpu >= 0
                ? I18n.get("minetuner.stat.cpu", String.format("%.1f", avgCpu))
                : I18n.get("minetuner.stat.cpu.na");
        String cpuLabel = I18n.get("gui.minetuner.benchmark.avg_cpu") + ": ";
        g.text(font, cpuLabel, innerX, innerY, COL_TEXT, false);
        g.text(font, cpuValue, innerX + font.width(cpuLabel), innerY,
                MineTunerDataHolder.cpuColorFor(avgCpu), false);
        innerY += RESULT_ROW_H;

        int graphX = x + 6;
        int graphW = w - 12;
        int graphH = singleGraphHeight();

        float[] frametimeSeries = BenchmarkSession.finalFrametimeSeries();
        drawFullLineGraph(g, graphX, innerY, graphW, graphH,
                frametimeSeries, COL_FRAMETIME_LINE,
                I18n.get("gui.minetuner.benchmark.graph.frametime"), "ms", Trend.LOWER_IS_BETTER);
        innerY += graphH + GRAPH_GAP;

        float[] cpuSeries = BenchmarkSession.finalCpuSeries();
        drawFullLineGraph(g, graphX, innerY, graphW, graphH,
                cpuSeries, COL_CPU_LINE,
                I18n.get("gui.minetuner.benchmark.graph.cpu"), "%", Trend.NEUTRAL);
        innerY += graphH + GRAPH_GAP;

        float[] gpuSeries = BenchmarkSession.finalGpuSeries();
        drawFullLineGraph(g, graphX, innerY, graphW, graphH,
                gpuSeries, COL_GPU_LINE,
                I18n.get("gui.minetuner.benchmark.graph.gpu"), "%", Trend.NEUTRAL);
    }

    private void drawEmptyState(GuiGraphicsExtractor g, int x, int y, int w) {
        int panelH = 60;
        drawPanelBackground(g, x, y, w, panelH, false);
        g.centeredText(font, "§7" + I18n.get("gui.minetuner.benchmark.no_result"),
                x + w / 2, y + panelH / 2 - font.lineHeight / 2, COL_TEXT_DIM);
    }

    // --- Layout math for the results section's stacked graphs ---

    private int graphsSectionHeight() {
        // Vertical space already spoken for above this point on a STOPPED screen: margins,
        // header, state/controls row (plus its optional save-confirmation line), the
        // "Latest Result" heading, and the results panel's own duration/samples + three-up
        // stat rows — mirrors drawHeader/drawStateAndControls/drawResultsSection's actual
        // fixed contributions so this estimate stays accurate if those ever change together.
        int fixedAbove = MARGIN + HEADER_H + SECTION_GAP
                + font.lineHeight + 6 + BUTTON_H // state label + button row
                + SECTION_GAP
                + font.lineHeight + 4 // "Latest Result" heading
                + RESULT_ROW_H * 3 + 14; // panel's own duration/samples + stat rows + padding
        int fixedBelow = 14 /* bottom hint line */ + MARGIN;

        int available = height - fixedAbove - fixedBelow;
        int minTotal = (MIN_RESULT_GRAPH_H + GRAPH_LABEL_H) * 3 + GRAPH_GAP * 2;
        return Math.max(minTotal, available);
    }

    /** One graph's own height (label row + plot), i.e. graphsSectionHeight() split three
     *  ways with the gaps between them subtracted first. */
    private int singleGraphHeight() {
        int total = graphsSectionHeight();
        int perGraph = (total - GRAPH_GAP * 2) / 3;
        return Math.max(MIN_RESULT_GRAPH_H + GRAPH_LABEL_H, perGraph);
    }

    // --- Small drawing helpers ---

    private void drawPanelBackground(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean active) {
        g.fill(x, y, x + w, y + h, COL_BG);
        g.outline(x, y, w, h, active ? COL_BORDER : COL_BORDER_DIM);
    }

    private void drawButton(GuiGraphicsExtractor g, int mx, int my, int bx, int by, int bw, int bh,
                            String label, boolean enabled) {
        boolean hovered = enabled && mx >= bx && mx < bx + bw && my >= by && my < by + bh;
        int bg = !enabled ? COL_BTN_BG_DISABLED : hovered ? COL_BTN_BG_HOVER : COL_BTN_BG;
        g.fill(bx, by, bx + bw, by + bh, bg);
        g.outline(bx, by, bw, bh, enabled ? COL_BORDER : COL_BORDER_DIM);
        int textColor = enabled ? COL_TEXT : COL_TEXT_DIM;
        int textW = font.width(label);
        g.text(font, label, bx + (bw - textW) / 2, by + (bh - font.lineHeight) / 2, textColor, false);
    }

    private void drawStatColumn(GuiGraphicsExtractor g, int x, int y, int w, String label, String value, int valueColor) {
        g.text(font, label, x, y, COL_TEXT_DIM, false);
        g.text(font, value, x, y + font.lineHeight + 1, valueColor, false);
    }

    /** Small live line graph in this screen's own visual language (line + soft fill,
     *  thin border) — see the class doc for why this doesn't reuse
     *  bottled.minetuner.hud.GraphRenderer directly. */
    private void drawFrametimeGraph(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                    float[] rawFrametimesMs, int lineColor, String label) {
        g.fill(x, y, x + w, y + h, 0x30FFFFFF);
        g.outline(x, y, w, h, 0x60FFFFFF);
        int plotX = x + 1, plotY = y + 1, plotW = w - 2, plotH = h - 2;
        if (plotW <= 0 || plotH <= 0 || rawFrametimesMs.length == 0) return;

        float[] fpsSeries = toFpsSeries(rawFrametimesMs);
        float[] bounds = seriesBounds(fpsSeries);
        drawLineSeries(g, fpsSeries, bounds[0], bounds[1], plotX, plotY, plotW, plotH, lineColor);
        drawGraphLabel(g, x, y, w, label, lineColor);
    }

    /** Converts a raw per-frame frametime series (ms) to FPS. Used by the live section's
     *  graph (drawFrametimeGraph, fed from MineTunerDataHolder's rolling window) — the
     *  results section's own Frametime graph draws BenchmarkSession#finalFrametimeSeries()
     *  directly in ms and does not call this. A 0-or-negative frametime (shouldn't normally
     *  happen — see MineTunerDataHolder#recordFrametime's own deltaMs > 0f guard upstream —
     *  but kept defensive here too) maps to 0 FPS rather than dividing by zero/going
     *  negative. */
    private static float[] toFpsSeries(float[] frametimesMs) {
        float[] out = new float[frametimesMs.length];
        for (int i = 0; i < frametimesMs.length; i++) {
            out[i] = frametimesMs[i] > 0f ? 1000f / frametimesMs[i] : 0f;
        }
        return out;
    }

    /** [min, max] over a series, ignoring -1-sentinel "unavailable" entries (see
     *  BenchmarkSession#finalCpuSeries()/finalGpuSeries()'s own doc on that sentinel) so a
     *  run where GPU usage was off for part of the time doesn't have its whole graph's
     *  vertical scale dragged down toward -1 by samples that were never real. If every
     *  sample is unavailable this returns [0, 1] as a harmless default range — the caller
     *  ends up drawing a flat empty-looking graph, not a divide-by-zero. */
    private static float[] seriesBounds(float[] series) {
        float minV = 0f, maxV = 0f;
        boolean seen = false;
        for (float v : series) {
            if (v < 0f) continue; // -1 sentinel: unavailable this sample, skip.
            if (!seen || v < minV) minV = v;
            if (v > maxV) maxV = v;
            seen = true;
        }
        if (!seen) return new float[]{0f, 1f};
        if (maxV <= minV) maxV = minV + 1f;
        return new float[]{minV, maxV};
    }

    /** Which direction is "good" for a results-screen graph's axis labels — see
     *  drawFullLineGraph's own doc for how this affects min/max label coloring. HIGHER
     *  (Frametime's opposite; used by nothing yet but kept for symmetry/future use),
     *  LOWER (Frametime: less time per frame is better), and NEUTRAL (CPU/GPU: neither
     *  end of the range is inherently good or bad on its own) are genuinely different
     *  cases, not just two names for the same "false" a plain boolean would have
     *  collapsed them into. */
    private enum Trend {
        HIGHER_IS_BETTER, LOWER_IS_BETTER, NEUTRAL
    }

    /** One full-length results-section graph: title/axis-label row above a plot spanning
     *  every sample in `series` left-to-right. `unit` is appended to the min/max axis
     *  labels (e.g. "%" for CPU/GPU, "ms" for Frametime). `trend` picks which of min/max
     *  (if either) gets colored as "the good end" for a quick-glance read, matching
     *  drawResultsSection's own use of MineTunerDataHolder#fpsColorFor/cpuColorFor
     *  elsewhere on this same screen. */
    private void drawFullLineGraph(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                   float[] series, int lineColor, String title, String unit,
                                   Trend trend) {
        int labelY = y;
        int plotOuterY = y + GRAPH_LABEL_H;
        int plotOuterH = h - GRAPH_LABEL_H;

        g.fill(x, plotOuterY, x + w, plotOuterY + plotOuterH, 0x30FFFFFF);
        g.outline(x, plotOuterY, w, plotOuterH, 0x60FFFFFF);

        int plotX = x + 1, plotY = plotOuterY + 1, plotW = w - 2, plotH = plotOuterH - 2;

        boolean hasData = plotW > 0 && plotH > 0 && series.length > 0;
        float[] bounds = hasData ? seriesBounds(series) : new float[]{0f, 1f};
        float minV = bounds[0], maxV = bounds[1];

        if (hasData) {
            drawLineSeries(g, series, minV, maxV, plotX, plotY, plotW, plotH, lineColor);
        } else {
            // No samples at all for this stat (e.g. GPU usage never once became available
            // during the run because hardware sensors were off) — an empty bordered plot
            // area with just the title/", no data" note reads clearly as "nothing to show
            // here", rather than silently rendering nothing with no explanation.
            String none = I18n.get("gui.minetuner.benchmark.graph.no_data");
            g.text(font, "§7" + none, plotX + 2, plotY + Math.max(0, plotH / 2 - font.lineHeight / 2),
                    COL_TEXT_DIM, false);
        }

        // Title, left-aligned above the plot.
        g.text(font, title, x, labelY, lineColor, false);

        // Min/max axis labels, right-aligned above the plot on the same row as the title —
        // "current" isn't shown here (unlike GraphRenderer's live MIN_CURRENT_MAX mode)
        // since a frozen post-run graph has no single "current" sample that means anything;
        // min/max is what actually characterizes the whole captured run at a glance. Colored
        // by which end is "the good one" for this stat (Frametime: lower; CPU/GPU: neither
        // is strictly good/bad on its own, so both stay neutral) rather than reusing
        // MineTunerDataHolder's live-value color helpers, which read a *current* field this
        // graph — drawing a whole frozen run's history — has no single current sample for.
        if (hasData) {
            String minLabel = formatAxisValue(minV) + unit;
            String maxLabel = formatAxisValue(maxV) + unit;
            int minColor = trend == Trend.LOWER_IS_BETTER ? COL_STOPPED : COL_TEXT_DIM;
            int maxColor = trend == Trend.HIGHER_IS_BETTER ? COL_STOPPED : COL_TEXT_DIM;
            String combined = minLabel + " – " + maxLabel;
            int combinedW = font.width(combined);
            // Falls back to just the max label if the window's too narrow to fit
            // "min – max" without overlapping the title text to its left.
            if (x + w - combinedW - 2 > x + font.width(title) + 6) {
                g.text(font, minLabel, x + w - combinedW - 2, labelY, minColor, false);
                g.text(font, " – ", x + w - combinedW - 2 + font.width(minLabel), labelY, COL_TEXT_DIM, false);
                g.text(font, maxLabel,
                        x + w - font.width(maxLabel) - 2, labelY, maxColor, false);
            } else {
                g.text(font, maxLabel, x + w - font.width(maxLabel) - 2, labelY, maxColor, false);
            }
        }
    }

    /** Small "STAT NAME" label chip in the corner of a graph, used by the live section's
     *  single graph (drawFullLineGraph handles its own title row separately, above the
     *  plot rather than overlaid on it, since the results section has real width budget
     *  for a dedicated label row — the live section's single fixed-height graph doesn't). */
    private void drawGraphLabel(GuiGraphicsExtractor g, int x, int y, int w, String label, int color) {
        if (label.isEmpty()) return;
        int labelW = font.width(label);
        g.fill(x + 2, y + 2, x + 2 + labelW + 2, y + 2 + font.lineHeight, 0x80000000);
        g.text(font, label, x + 3, y + 2, color, false);
    }

    private void drawLineSeries(GuiGraphicsExtractor g, float[] series, float minV, float maxV,
                                int plotX, int plotY, int plotW, int plotH, int color) {
        int n = series.length;
        float range = Math.max(1e-4f, maxV - minV);
        int baseY = plotY + plotH;
        int prevY = -1;
        for (int col = 0; col < plotW; col++) {
            float t = (n <= 1) ? 0f : (col / (float) Math.max(1, plotW - 1)) * (n - 1);
            int i0 = (int) Math.floor(t);
            int i1 = Math.min(n - 1, i0 + 1);
            float frac = t - i0;
            float rawV0 = series[i0], rawV1 = series[i1];
            // A -1-sentinel "unavailable" sample (see seriesBounds()'s own doc on this same
            // convention) is skipped by interpolating from/to its nearest available
            // neighbor instead of blending toward -1, so a brief CPU/GPU sensor gap doesn't
            // draw as a sudden fake dip to the bottom of the graph.
            float v0 = rawV0 >= 0f ? rawV0 : nearestAvailable(series, i0);
            float v1 = rawV1 >= 0f ? rawV1 : nearestAvailable(series, i1);
            float v = v0 + (v1 - v0) * frac;
            float norm = Math.max(0f, Math.min(1f, (v - minV) / range));
            int colY = plotY + plotH - Math.max(1, Math.round(norm * (plotH - 1)));
            int screenX = plotX + col;
            // Soft fill under the line.
            g.fill(screenX, colY, screenX + 1, baseY, withAlpha(color, 0x50));
            if (prevY >= 0) {
                g.fill(screenX, Math.min(colY, prevY), screenX + 1, Math.max(colY, prevY) + 1, withAlpha(color, 0xE0));
            } else {
                g.fill(screenX, colY, screenX + 1, colY + 1, withAlpha(color, 0xE0));
            }
            prevY = colY;
        }
    }

    /** Nearest sample to `fromIdx` (searching outward both directions) that isn't the -1
     *  "unavailable" sentinel, or 0f if the whole series is unavailable (seriesBounds()
     *  already special-cases an all-unavailable series before this is ever reached in
     *  practice, but this stays defined rather than throwing either way). */
    private static float nearestAvailable(float[] series, int fromIdx) {
        int n = series.length;
        for (int d = 0; d < n; d++) {
            int lo = fromIdx - d, hi = fromIdx + d;
            if (lo >= 0 && series[lo] >= 0f) return series[lo];
            if (hi < n && series[hi] >= 0f) return series[hi];
        }
        return 0f;
    }

    // --- Formatting helpers ---

    private static String formatDuration(long nanos) {
        long totalMs = nanos / 1_000_000L;
        long totalSeconds = totalMs / 1000L;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long tenths = (totalMs % 1000L) / 100L;
        return minutes > 0
                ? String.format("%d:%02d.%d", minutes, seconds, tenths)
                : String.format("%d.%ds", seconds, tenths);
    }

    private static String formatFpsValue(float fps) {
        return String.format("%.1f", fps);
    }

    private static String formatAxisValue(float v) {
        return String.format("%.0f", v);
    }

    /** "Not enough samples yet" is a distinct, explicit state from a genuine 0 — see
     *  PercentileLowFps#UNAVAILABLE (NaN) and MineTunerDataHolder's own getFormattedFps1Low/
     *  getFormattedFps01Low, which follow the same "N/A rather than a misleading number"
     *  convention this mirrors. */
    private static String formatPercentileValue(float value) {
        return Float.isNaN(value) ? I18n.get("gui.minetuner.benchmark.not_enough_samples")
                : String.format("%.1f", value);
    }

    private static int percentileColor(float value) {
        return Float.isNaN(value) ? COL_TEXT_DIM : MineTunerDataHolder.fpsColorFor(value);
    }

    private static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    // --- Input ---

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int mx = (int) event.x();
        int my = (int) event.y();

        if (isInside(mx, my, startBtnX, startBtnY, startBtnW, startBtnH)) {
            // Always actionable — see drawStateAndControls's comment on why Start/Reset
            // has no disabled state. Starting a fresh run also clears any leftover
            // save-confirmation message from a previous run, same as it already clears
            // every other frozen-result field.
            BenchmarkSession.start();
            saveMessage = null;
            return true;
        }
        if (BenchmarkSession.isRecording()
                && isInside(mx, my, stopBtnX, stopBtnY, stopBtnW, stopBtnH)) {
            BenchmarkSession.stop();
            return true;
        }
        if (BenchmarkSession.hasFrozenResult()
                && isInside(mx, my, saveBtnX, saveBtnY, saveBtnW, saveBtnH)) {
            Path saved = BenchmarkSession.saveToFolder();
            if (saved != null) {
                saveMessage = I18n.get("gui.minetuner.benchmark.saved", saved.getFileName().toString());
                saveMessageColor = COL_STOPPED;
            } else {
                saveMessage = I18n.get("gui.minetuner.benchmark.save_failed");
                saveMessageColor = COL_RECORDING;
            }
            saveMessageShownAtMs = System.currentTimeMillis();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
