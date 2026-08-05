package bottled.mtss.hud;

import bottled.mtss.config.MtssConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public record LineCache(List<String> lines, List<Integer> colors,
                        List<LineCache.GraphEntry> graphEntries, List<LineCache.RowKind> rowKinds) {

    public record GraphEntry(MtssConfig.Stat stat, float[] rawHistory, float[] displayHistory,
                             int color, String label, String minValueLabel, String maxValueLabel,
                             float scaleMin, float scaleMax, int peakMinIdx, int peakMaxIdx,
                             MtssConfig.GraphStyle style, MtssConfig.ThresholdSettings threshold) {}

    /** Which underlying list a given display row pulls from. */
    public enum RowKind { TEXT, GRAPH }

    /** Fallback size for the empty-list placeholder; real graphs size themselves from GraphStyle.width/height. */
    public static final int GRAPH_W = 80;
    public static final int GRAPH_H = 28;

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

    // ── Frame cache ─────────────────────────────────────────────────────────

    private static final Map<Integer, LineCache> FRAME_CACHE = new HashMap<>();
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
            LineBuilder.buildLines(cfg, lines, colors, graphEntries, rowKinds);
            return new LineCache(lines, colors, graphEntries, rowKinds);
        });
    }
}
