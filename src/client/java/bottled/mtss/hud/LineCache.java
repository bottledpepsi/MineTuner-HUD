package bottled.mtss.hud;

import bottled.mtss.config.MtssConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public record LineCache(List<String> lines, List<Integer> colors, List<List<TemplateEngine.ColoredRun>> runs,
                        List<LineCache.GraphEntry> graphEntries, List<LineCache.RowKind> rowKinds) {

    // `lines`/`colors` stay the flat single-color view every existing reader.
    // (width measurement, drag-preview rendering, getListBounds sizing) was.
    // built against.
    // row) and only carries information beyond "one flat color" when a.
    // template line actually used an inline color=# modifier.
    // uses `runs` to draw, everything else keeps reading `lines`/`colors`.
    // unchanged.
    // `runs.get(i)` joined together, so anything measuring wrapped text.
    // width doesn't need to change just because coloring got richer.

    /** Fallback size for the empty-list placeholder. */
    public static final int GRAPH_W = 80;
    public static final int GRAPH_H = 28;
    private static final Map<Integer, LineCache> FRAME_CACHE = new HashMap<>();

    /** Advances the cache generation. */
    public static void tickCache() {
        FRAME_CACHE.clear();
    }

    /** Returns cached lines for this list, building them if needed this frame. */
    public static LineCache getCachedLines(MtssConfig.StatListConfig cfg) {
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
        int textW = lines.stream().mapToInt(font::width).max().orElse(0);
        int graphW = graphEntries.isEmpty() ? 0 : graphEntries.stream()
                .mapToInt(e -> Math.max(e.style().width, font.width(e.label()) + 4))
                .max().orElse(GRAPH_W);
        return Math.max(textW, graphW) + 4;
    }

    // separator

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

    /** Which underlying list a given display row pulls from. */
    public enum RowKind {TEXT, GRAPH}

    public record GraphEntry(MtssConfig.Stat stat, float[] rawHistory, float[] displayHistory,
                             int color, String label, String minValueLabel, String maxValueLabel,
                             float scaleMin, float scaleMax, int peakMinIdx, int peakMaxIdx,
                             MtssConfig.GraphStyle style, MtssConfig.ThresholdSettings threshold) {
    }
}
