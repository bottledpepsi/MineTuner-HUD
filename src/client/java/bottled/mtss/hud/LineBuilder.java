package bottled.mtss.hud;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.hud.LineCache.GraphEntry;
import bottled.mtss.hud.LineCache.RowKind;
import bottled.mtss.stat.StatDefinition;
import bottled.mtss.stat.StatRegistry;

import java.util.List;


final class LineBuilder {

    private LineBuilder() {
    }

    public static void buildLines(MtssConfig.StatListConfig cfg,
                                  List<String> lines, List<Integer> colors, List<List<TemplateEngine.ColoredRun>> runs,
                                  List<GraphEntry> graphEntries, List<RowKind> rowKinds) {
        // Template mode is a separate path, opt-in per list; the classic-mode loop
        // below is untouched either way.
        if (cfg.useTemplate) {
            buildTemplateLines(cfg, lines, colors, runs, graphEntries, rowKinds);
            return;
        }
        buildClassicLines(cfg, lines, colors, runs, graphEntries, rowKinds);
    }

    /** Template mode. */
    private static void buildTemplateLines(MtssConfig.StatListConfig cfg,
                                           List<String> lines, List<Integer> colors, List<List<TemplateEngine.ColoredRun>> runs,
                                           List<GraphEntry> graphEntries, List<RowKind> rowKinds) {
        int color = cfg.useCustomColor ? cfg.overrideColor : 0xFFFFFFFF;
        List<List<TemplateEngine.Token>> parsedLines = TemplateEngine.getParsedLines(cfg);
        for (List<TemplateEngine.Token> tokens : parsedLines) {
            // A graph row requires the line to be exactly one graph-flagged stat token.
            if (tokens.size() == 1 && tokens.get(0) instanceof TemplateEngine.StatToken st && st.asGraph()) {
                GraphEntry entry = buildGraphEntry(cfg, st.stat());
                if (entry == null) continue; // no history yet, same as classic mode skipping unavailable stats.
                graphEntries.add(entry);
                rowKinds.add(RowKind.GRAPH);
                continue;
            }
            List<TemplateEngine.ColoredRun> lineRuns = TemplateEngine.renderRuns(tokens, color);
            lines.add(TemplateEngine.render(tokens));
            colors.add(color);
            runs.add(lineRuns);
            rowKinds.add(RowKind.TEXT);
        }
    }

    /** Classic mode. */
    private static void buildClassicLines(MtssConfig.StatListConfig cfg,
                                          List<String> lines, List<Integer> colors, List<List<TemplateEngine.ColoredRun>> runs,
                                          List<GraphEntry> graphEntries, List<RowKind> rowKinds) {
        for (MtssConfig.Stat stat : cfg.getVisibleStats()) {
            // Every stat's behavior comes from its StatDefinition now — no
            // per-stat switch here, so a new stat needs no changes in this file.
            StatDefinition def = StatRegistry.get(stat);

            // getStatSettings() lazily creates settings for any stat, so this
            // is safe even for stats with no settings entry written to disk.
            boolean asGraph = def.supportsGraph() && cfg.getStatSettings(stat).renderAsGraph;

            if (asGraph) {
                GraphEntry entry = buildGraphEntry(cfg, stat);
                // Nothing to draw yet (unsupported CPU, remote-server MSPT, etc.).
                if (entry == null) continue;
                graphEntries.add(entry);
                rowKinds.add(RowKind.GRAPH);
                continue;
            }

            // Classic text row: formatting and coloring both come from the stat's
            // own definition, so this block never needs a new case.
            int decimals = cfg.getStatSettings(stat).decimals;
            String text = def.format(decimals);
            int color = def.color(cfg.getThreshold(stat));
            if (text == null || text.isEmpty()) continue;
            // Strip the "Label: " prefix when the user has turned it off.
            if (!cfg.getStatSettings(stat).showPrefix) {
                int sep = text.indexOf(": ");
                if (sep >= 0) text = text.substring(sep + 2);
            }
            // Per-list color override replaces threshold coloring.
            if (cfg.useCustomColor) color = cfg.overrideColor;
            lines.add(text);
            colors.add(color);
            runs.add(List.of(new TemplateEngine.ColoredRun(text, color))); // classic mode has no inline-color concept.
            rowKinds.add(RowKind.TEXT);
        }
    }

    /** Builds a graph row's full {@link GraphEntry} for one stat. */
    private static GraphEntry buildGraphEntry(MtssConfig.StatListConfig cfg, MtssConfig.Stat stat) {
        StatDefinition def = StatRegistry.get(stat);

        MtssConfig.StatSettings statSettings = cfg.getStatSettings(stat);
        int decimals = statSettings.decimals;
        MtssConfig.GraphStyle style = statSettings.graphStyle;

        float[] rawHistory = def.history();
        if (rawHistory.length == 0) return null;

        // Smoothed into a separate array each frame; rawHistory itself is never mutated,
        // since GraphEntry keeps both the raw and smoothed series for the renderer.
        float[] displayHistory = applySmoothing(rawHistory, style.smoothing);

        // Current-value color, also used for CURRENT_THRESHOLD mode and the label color.
        // Stats with no threshold of their own (MSPT/Speed) ignore the custom argument
        // or resolve it to a related stat's threshold — see their StatDefinition for
        // specifics.
        int currentColor = def.color(cfg.getThreshold(stat));
        // Per-list color override beats everything.
        if (cfg.useCustomColor) currentColor = cfg.overrideColor;

        // Same formatted string text mode would show, so switching a stat between
        // text and graph doesn't change how the number reads.
        String label = def.format(decimals);
        if (!statSettings.showPrefix) {
            int sep = label.indexOf(": ");
            if (sep >= 0) label = label.substring(sep + 2);
        }
        // MSPT can go unavailable mid-session (e.g. leaving a singleplayer world), while
        // its history still has old samples. Falling back to the last sample's formatted
        // value (via formatAxisValue, the same helper used for minValueLabel/maxValueLabel
        // below) avoids showing a blank label instead of printing the raw float, so the
        // fallback still respects the stat's normal decimal formatting.
        if (label.isEmpty()) label = def.formatAxisValue(rawHistory[rawHistory.length - 1]);

        // Scale bounds computed once per frame here, not per pixel-column in drawGraph.
        float scaleMin, scaleMax;
        if (style.autoScale) {
            float rawMin = Float.MAX_VALUE, rawMax = -Float.MAX_VALUE;
            for (float v : displayHistory) {
                if (v < rawMin) rawMin = v;
                if (v > rawMax) rawMax = v;
            }
            float range = rawMax - rawMin;
            // 10% headroom so the line doesn't touch the plot edges.
            float pad = range > 1e-4f ? range * 0.10f : 1f;
            scaleMin = rawMin - pad;
            scaleMax = rawMax + pad;
        } else {
            // Fixed mode.
            scaleMin = style.fixedMin;
            scaleMax = style.fixedMax;
        }
        if (scaleMax - scaleMin < 1e-4f) scaleMax = scaleMin + 1f; // guard divide-by-zero below.

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

        return new GraphEntry(stat, rawHistory, displayHistory, currentColor, label,
                minValueLabel, maxValueLabel, scaleMin, scaleMax, peakMinIdx, peakMaxIdx, style, entryThreshold);
    }

    /** Moving average over the last {@code window} samples, into a new array (input is
     *  never mutated). The window ramps up from size 1 at the first sample to full size
     *  once enough samples exist, rather than padding with zeros, so early points aren't
     *  pulled artificially low. {@code window <= 1} is a no-op that returns {@code raw} itself. */
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
