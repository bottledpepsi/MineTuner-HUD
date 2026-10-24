package bottled.minetuner.hud;

import bottled.minetuner.config.MineTunerConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public record LineCache(List<String> lines, List<Integer> colors, List<List<TemplateEngine.ColoredRun>> runs,
                        List<LineCache.GraphEntry> graphEntries, List<LineCache.RowKind> rowKinds) {

    // `lines`/`colors` stay the flat single-color view every existing reader
    // (width measurement, drag-preview rendering, getListBounds sizing) was
    // built against — one entry per row, one color per row. `runs` only carries
    // information beyond "one flat color" when a template line actually used an
    // inline color=# modifier. Only MineTunerRenderer uses `runs` to draw,
    // everything else keeps reading `lines`/`colors` unchanged. `lines` still
    // holds the same text as `runs.get(i)` joined together, so anything
    // measuring row width doesn't need to change just because coloring got richer.

    /** Fallback size for the empty-list placeholder. */
    public static final int GRAPH_W = 80;
    public static final int GRAPH_H = 28;
    private static final Map<Integer, LineCache> FRAME_CACHE = new HashMap<>();

    /** Advances the cache generation. */
    public static void tickCache() {
        FRAME_CACHE.clear();
    }

    /** Returns cached lines for this list, building them if needed this frame. */
    public static LineCache getCachedLines(MineTunerConfig.StatListConfig cfg) {
        return FRAME_CACHE.computeIfAbsent(cfg.id, id -> {
            List<String> lines = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            List<List<TemplateEngine.ColoredRun>> runs = new ArrayList<>();
            List<GraphEntry> graphEntries = new ArrayList<>();
            List<RowKind> rowKinds = new ArrayList<>();
            LineBuilder.buildLines(cfg, lines, colors, runs, graphEntries, rowKinds);
            return new LineCache(lines, colors, runs, graphEntries, rowKinds);
        });
    }

    public int boxW(net.minecraft.client.gui.Font font) {
        return boxW(font, HudPanelChrome.PADDING_X);
    }

    public int boxW(net.minecraft.client.gui.Font font, int paddingX) {
        // Plain loops — the stream versions allocated a pipeline + lambdas twice per
        // frame per list (boxW is called for both measurement and drawing).
        int textW = 0;
        for (String line : lines) {
            textW = Math.max(textW, font.width(line));
        }
        int graphW = 0;
        for (GraphEntry e : graphEntries) {
            graphW = Math.max(graphW, Math.max(e.style().width, font.width(e.label()) + 4));
        }
        int px = HudPanelChrome.paddingX(paddingX);
        return Math.max(textW, graphW) + px * 2;
    }

    public int boxH(net.minecraft.client.gui.Font font) {
        return boxH(font, HudPanelChrome.PADDING_Y);
    }

    public int boxH(net.minecraft.client.gui.Font font, int paddingY) {
        int lineH = font.lineHeight + HudPanelChrome.ROW_GAP;
        int py = HudPanelChrome.paddingY(paddingY);
        int h = py * 2;
        for (int i = 0, g = 0; i < rowKinds.size(); i++) {
            if (rowKinds.get(i) == RowKind.GRAPH) {
                h += graphEntries.get(g).style().height + 1;
                g++;
            } else {
                h += lineH;
            }
        }
        // Each row reserves a trailing gap for the row below it; the last row has
        // none, so drop it or the box ends up bottom-heavy (more space under the
        // final row than above the first).
        if (!rowKinds.isEmpty()) {
            h -= rowKinds.get(rowKinds.size() - 1) == RowKind.GRAPH ? 1 : HudPanelChrome.ROW_GAP;
        }
        return h;
    }

    /** Which underlying list a given display row pulls from. */
    public enum RowKind {TEXT, GRAPH}

    public record GraphEntry(MineTunerConfig.Stat stat, float[] rawHistory, float[] displayHistory,
                             int color, String label, String minValueLabel, String maxValueLabel,
                             float scaleMin, float scaleMax, int peakMinIdx, int peakMaxIdx,
                             MineTunerConfig.GraphStyle style, MineTunerConfig.ThresholdSettings threshold) {
    }
}
