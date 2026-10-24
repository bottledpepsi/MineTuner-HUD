package bottled.minetuner.gui;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.benchmark.BenchmarkSession;
import bottled.minetuner.core.SeriesPlot;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
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
    // Per-graph cache of display-ready plot data, keyed on the frozen series instance and
    // the plot width, so drawing never scans or copies the (up to 432,000 sample) series.
    private static final int GRAPH_FRAMETIME = 0, GRAPH_CPU = 1, GRAPH_GPU = 2;
    private final float[][] plotSeries = new float[3][];
    private final int[] plotWidths = new int[3];
    private final SeriesPlot.Plot[] plots = new SeriesPlot.Plot[3];
    /**
     * True between clicking Save and the background write reporting its outcome.
     */
    private boolean awaitingSaveResult = false;

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

        String startLabel = switch (state) {
            case STOPPED -> I18n.get("gui.minetuner.benchmark.button.reset");
            // While RECORDING the button discards the in-progress capture and starts
            // fresh (RTSS F9 semantics) — label it "Restart" so that's visible instead
            // of a plain "Start" that reads like a no-op while a run is already live.
            case RECORDING -> I18n.get("gui.minetuner.benchmark.button.restart");
            default -> I18n.get("gui.minetuner.benchmark.button.start");
        };
        drawButton(g, mx, my, startBtnX, startBtnY, startBtnW, startBtnH, startLabel, true);
        drawButton(g, mx, my, stopBtnX, stopBtnY, stopBtnW, stopBtnH,
                I18n.get("gui.minetuner.benchmark.button.stop"), stopEnabled);
        drawButton(g, mx, my, saveBtnX, saveBtnY, saveBtnW, saveBtnH,
                I18n.get("gui.minetuner.benchmark.button.save"), saveEnabled);

        y += BUTTON_H;

        // Save progress / confirmation under the button row. "Saving..." stays up while the
        // background write runs; the outcome message then auto-hides after
        // SAVE_MESSAGE_DURATION_MS.
        BenchmarkSession.SaveState saveState = BenchmarkSession.saveState();
        if (saveState == BenchmarkSession.SaveState.SAVING) {
            g.centeredText(font, I18n.get("gui.minetuner.benchmark.saving"), x + w / 2, y + 4, COL_TEXT_DIM);
        } else {
            if (awaitingSaveResult && saveState != BenchmarkSession.SaveState.IDLE) {
                awaitingSaveResult = false;
                Path saved = BenchmarkSession.lastSavedPath();
                if (saveState == BenchmarkSession.SaveState.SAVED && saved != null) {
                    saveMessage = I18n.get("gui.minetuner.benchmark.saved", saved.getFileName().toString());
                    saveMessageColor = COL_STOPPED;
                } else {
                    saveMessage = I18n.get("gui.minetuner.benchmark.save_failed");
                    saveMessageColor = COL_RECORDING;
                }
                saveMessageShownAtMs = System.currentTimeMillis();
            }
            if (saveMessage != null) {
                if (System.currentTimeMillis() - saveMessageShownAtMs > SAVE_MESSAGE_DURATION_MS) {
                    saveMessage = null;
                } else {
                    g.centeredText(font, saveMessage, x + w / 2, y + 4, saveMessageColor);
                }
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
        // Auto-stop notice, centered on the duration row (no layout-height change):
        // the log line is easy to miss, so the "you forgot it was running" case is
        // surfaced right on the frozen result it produced.
        if (BenchmarkSession.wasAutoStopped()) {
            String note = I18n.get("gui.minetuner.benchmark.auto_stopped", BenchmarkSession.MAX_SAMPLES);
            g.centeredText(font, "§e" + note, x + w / 2, innerY, COL_RECORDING);
        }
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
        // Avg CPU is the JVM *process* load (time-weighted over the run), not whole-system
        // CPU, so the label says so; Task Manager's system figure will differ.
        double avgCpu = BenchmarkSession.finalAvgCpuPercent();
        String cpuValue = avgCpu >= 0
                ? String.format("%.1f%%", avgCpu)
                : I18n.get("gui.minetuner.benchmark.not_enough_samples");
        String cpuLabel = I18n.get("gui.minetuner.benchmark.avg_cpu_process") + ": ";
        g.text(font, cpuLabel, innerX, innerY, COL_TEXT, false);
        g.text(font, cpuValue, innerX + font.width(cpuLabel), innerY,
                MineTunerDataHolder.cpuColorFor(avgCpu), false);
        innerY += RESULT_ROW_H;

        int graphX = x + 6;
        int graphW = w - 12;
        int graphH = singleGraphHeight();

        // Zero-copy views of the frozen series; plot data is cached per (series, width).
        drawFullLineGraph(g, graphX, innerY, graphW, graphH, GRAPH_FRAMETIME,
                BenchmarkSession.finalFrametimeSeriesView(), COL_FRAMETIME_LINE,
                I18n.get("gui.minetuner.benchmark.graph.frametime"), "ms", Trend.LOWER_IS_BETTER);
        innerY += graphH + GRAPH_GAP;

        drawFullLineGraph(g, graphX, innerY, graphW, graphH, GRAPH_CPU,
                BenchmarkSession.finalCpuSeriesView(), COL_CPU_LINE,
                I18n.get("gui.minetuner.benchmark.graph.cpu_process"), "%", Trend.NEUTRAL);
        innerY += graphH + GRAPH_GAP;

        drawFullLineGraph(g, graphX, innerY, graphW, graphH, GRAPH_GPU,
                BenchmarkSession.finalGpuSeriesView(), COL_GPU_LINE,
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
    private void drawFullLineGraph(GuiGraphicsExtractor g, int x, int y, int w, int h, int slot,
                                   float[] series, int lineColor, String title, String unit,
                                   Trend trend) {
        int labelY = y;
        int plotOuterY = y + GRAPH_LABEL_H;
        int plotOuterH = h - GRAPH_LABEL_H;

        g.fill(x, plotOuterY, x + w, plotOuterY + plotOuterH, 0x30FFFFFF);
        g.outline(x, plotOuterY, w, plotOuterH, 0x60FFFFFF);

        int plotX = x + 1, plotY = plotOuterY + 1, plotW = w - 2, plotH = plotOuterH - 2;

        // "Has data" means at least one real (non-sentinel) sample, decided by SeriesPlot once
        // per (series, width) rather than by scanning the series every frame.
        SeriesPlot.Plot plot = plotFor(slot, series, plotW);
        boolean hasData = plotW > 0 && plotH > 0 && plot.hasData();
        float minV = plot.min(), maxV = plot.max();

        if (hasData) {
            drawLineSeries(g, plot.columns(), minV, maxV, plotX, plotY, plotW, plotH, lineColor);
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

    /**
     * Cached plot data for one graph slot; recomputed only when the frozen series instance or
     * the plot width changes.
     */
    private SeriesPlot.Plot plotFor(int slot, float[] series, int plotW) {
        if (plots[slot] == null || plotSeries[slot] != series || plotWidths[slot] != plotW) {
            plots[slot] = SeriesPlot.compute(series, plotW);
            plotSeries[slot] = series;
            plotWidths[slot] = plotW;
        }
        return plots[slot];
    }

    /**
     * Draws precomputed per-column values (gap-filled by SeriesPlot) as a soft-filled line.
     */
    private void drawLineSeries(GuiGraphicsExtractor g, float[] columns, float minV, float maxV,
                                int plotX, int plotY, int plotW, int plotH, int color) {
        float range = Math.max(1e-4f, maxV - minV);
        int baseY = plotY + plotH;
        int prevY = -1;
        int cols = Math.min(plotW, columns.length);
        for (int col = 0; col < cols; col++) {
            float v = columns[col];
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
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);
        int mx = (int) event.x();
        int my = (int) event.y();

        if (isInside(mx, my, startBtnX, startBtnY, startBtnW, startBtnH)) {
            // Always actionable — see drawStateAndControls's comment on why Start/Reset
            // has no disabled state. Starting a fresh run also clears any leftover
            // save-confirmation message from a previous run, same as it already clears
            // every other frozen-result field.
            BenchmarkSession.start();
            saveMessage = null;
            awaitingSaveResult = false;
            return true;
        }
        if (BenchmarkSession.isRecording()
                && isInside(mx, my, stopBtnX, stopBtnY, stopBtnW, stopBtnH)) {
            BenchmarkSession.stop();
            return true;
        }
        if (BenchmarkSession.hasFrozenResult()
                && isInside(mx, my, saveBtnX, saveBtnY, saveBtnW, saveBtnH)) {
            // Written on a background thread; the outcome is picked up in
            // drawStateAndControls via BenchmarkSession.saveState().
            if (BenchmarkSession.saveState() != BenchmarkSession.SaveState.SAVING) {
                BenchmarkSession.saveToFolderAsync();
                awaitingSaveResult = true;
                saveMessage = null;
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
