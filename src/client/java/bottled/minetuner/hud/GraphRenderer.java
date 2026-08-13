package bottled.minetuner.hud;

import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.hud.LineCache.GraphEntry;
import bottled.minetuner.stat.StatRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;


final class GraphRenderer {

    private GraphRenderer() {
    }

    static void drawGraph(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font,
                          GraphEntry entry, int x, int y, int w, int h) {
        MineTunerConfig.GraphStyle style = entry.style();
        float[] display = entry.displayHistory();
        int n = display.length;

        // Separate from the list's own showBackground, so the graph still
        // reads as its own widget even when the list background is off.
        if (style.showPanelBackground) {
            graphics.fill(x, y, x + w, y + h, 0x30FFFFFF);
            graphics.outline(x, y, w, h, 0x60FFFFFF);
        } else {
            // Still frame the plot area, or the gridlines/plot would float with no border.
            graphics.outline(x, y, w, h, 0x40FFFFFF);
        }

        int plotW = w - 2;   // inset 1px on each side to stay inside the border.
        int plotH = h - 2;
        int plotX = x + 1;
        int plotY = y + 1;
        if (plotW <= 0 || plotH <= 0 || n == 0) return;

        if (style.showGridlines && plotH >= 12) {
            for (float frac : new float[]{0.25f, 0.5f, 0.75f}) {
                int gy = plotY + Math.round(plotH * (1f - frac));
                for (int dx = 0; dx < plotW; dx += 3) {
                    graphics.fill(plotX + dx, gy, plotX + Math.min(dx + 1, plotW), gy + 1, 0x25FFFFFF);
                }
            }
        }

        drawPlotLine(graphics, entry, style, display, n, plotX, plotY, plotW, plotH);

        if (style.showPeakMarkers && n >= 2 && plotW >= 10) {
            float peakMaxTopY = lerpTopY(entry, n, plotH, entry.peakMaxIdx());
            float peakMinTopY = lerpTopY(entry, n, plotH, entry.peakMinIdx());
            drawPeakMarker(graphics, entry.peakMaxIdx(), n, plotX, plotY, plotW, peakMaxTopY, 0xFFFFFFFF);
            drawPeakMarker(graphics, entry.peakMinIdx(), n, plotX, plotY, plotW, peakMinTopY, 0xFFAAAAAA);
        }

        String label = entry.label();
        if (style.valueDisplay != MineTunerConfig.GraphValueDisplay.NONE && !label.isEmpty()) {
            int labelW = font.width(label);
            graphics.fill(x + 1, y + 1, x + 1 + labelW + 2, y + 1 + font.lineHeight, 0x80000000);
            graphics.text(font, label, x + 2, y + 1, entry.color(), false);
        }
        if (style.valueDisplay == MineTunerConfig.GraphValueDisplay.MIN_CURRENT_MAX && n >= 2) {
            String minLabel = entry.minValueLabel();
            String maxLabel = entry.maxValueLabel();
            int axisY = y + h - font.lineHeight;
            graphics.text(font, minLabel, x + 2, axisY, 0xB0CCCCCC, false);
            int maxW = font.width(maxLabel);
            graphics.text(font, maxLabel, x + w - maxW - 2, axisY, 0xB0CCCCCC, false);
        }
    }

    private static void drawPlotLine(GuiGraphicsExtractor graphics, GraphEntry entry,
                                     MineTunerConfig.GraphStyle style, float[] display, int n,
                                     int plotX, int plotY, int plotW, int plotH) {
        if (plotW <= 0 || plotH <= 0) return;

        float scaleMin = entry.scaleMin(), scaleMax = entry.scaleMax();
        float range = Math.max(1e-4f, scaleMax - scaleMin);

        // Precompute each column's interpolated top-Y and nearest sample index.
        // once, shared by the fill and stroke passes below.
        int[] colTopY = new int[plotW];
        int[] colSampleIdx = new int[plotW];
        for (int col = 0; col < plotW; col++) {
            // Fractional position along the sample series this column represents,.
            // interpolated between the two nearest samples.
            float t = (n <= 1) ? 0f : (col / (float) Math.max(1, plotW - 1)) * (n - 1);
            int i0 = (int) Math.floor(t);
            int i1 = Math.min(n - 1, i0 + 1);
            float frac = t - i0;
            float v = display[i0] + (display[i1] - display[i0]) * frac;
            float norm = Math.max(0f, Math.min(1f, (v - scaleMin) / range)); // 0-1, clamped.
            int barH = Math.max(1, Math.round(norm * (plotH - 1)));
            colTopY[col] = plotY + plotH - barH;
            colSampleIdx[col] = Math.round(t);
        }

        int baseY = plotY + plotH; // exclusive bottom, shared by every column.

        // Precompute each column's fill color once, so the run-merging loops.
        // below don't re-invoke the threshold/gradient lookup per column.
        int[] colColor = new int[plotW];
        for (int col = 0; col < plotW; col++) {
            colColor[col] = colorForColumn(entry, style, colSampleIdx[col]);
        }

        // Adjacent columns sharing the same (topY, color) are drawn as a single
        // wide fill() run instead of one fill() per column. This isn't just a
        // micro-optimization: it's common for many adjacent columns to collapse
        // into the same run, since norm rounds to integer pixel heights and color
        // only changes at threshold boundaries, so one wide fill() replaces
        // many single-pixel ones — worth doing since this runs every frame, and
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
                graphics.fill(runX0, topY, runX1, Math.min(split, baseY), ColorMath.withAlpha(fillColor, 0xC8));
                if (split < baseY) graphics.fill(runX0, split, runX1, baseY, ColorMath.withAlpha(fillColor, 0x60));
            }
            runStart = runEnd;
        }

        // Second pass so the stroke line draws on top of neighboring fills, using the
        // same run-merging approach as the fill pass above but keyed on
        // (topY, strokeColor) instead of (topY, fillColor).
        int[] strokeColor = new int[plotW];
        for (int col = 0; col < plotW; col++) {
            strokeColor[col] = ColorMath.withAlpha(ColorMath.brighten(colColor[col]), 0xFF);
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

    private static int colorForColumn(GraphEntry entry, MineTunerConfig.GraphStyle style, int sampleIdx) {
        return switch (style.colorMode) {
            case CURRENT_THRESHOLD -> entry.color();
            case FIXED_ACCENT -> style.accentColor;
            case PER_SEGMENT_THRESHOLD -> StatRegistry.get(entry.stat())
                    .colorFor(entry.displayHistory()[sampleIdx], entry.threshold());
            case GRADIENT ->
                    gradientColorForValue(entry.displayHistory()[sampleIdx], entry.scaleMin(), entry.scaleMax());
        };
    }

    private static int gradientColorForValue(float value, float scaleMin, float scaleMax) {
        float range = scaleMax - scaleMin;
        float t = range > 1e-4f ? (value - scaleMin) / range : 0.5f;
        t = Math.max(0f, Math.min(1f, t));
        // 4-stop gradient.
        int[][] stops = {{0x40, 0x80, 0xFF}, {0x55, 0xFF, 0x55}, {0xFF, 0xFF, 0x55}, {0xFF, 0x55, 0x55}};
        float scaled = t * (stops.length - 1);
        int idx = Math.min(stops.length - 2, (int) scaled);
        float localT = scaled - idx;
        int r = Math.round(stops[idx][0] + (stops[idx + 1][0] - stops[idx][0]) * localT);
        int g = Math.round(stops[idx][1] + (stops[idx + 1][1] - stops[idx][1]) * localT);
        int b = Math.round(stops[idx][2] + (stops[idx + 1][2] - stops[idx][2]) * localT);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
