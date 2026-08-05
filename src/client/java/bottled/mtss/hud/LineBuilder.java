package bottled.mtss.hud;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.hud.LineCache.GraphEntry;
import bottled.mtss.hud.LineCache.RowKind;
import bottled.mtss.stat.StatDefinition;
import bottled.mtss.stat.StatRegistry;

import java.util.List;


final class LineBuilder {

    private LineBuilder() {}

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
}
