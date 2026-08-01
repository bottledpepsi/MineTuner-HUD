package bottled.mtss.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MtssConfig {

    /**
     * Which screen corner a list is anchored to.
     * anchorDx/anchorDy are pixel offsets FROM that corner, so the list
     * moves with its corner on window resize, keeping inter-list gaps stable.
     */
    public enum Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    /** How this list is anchored to the vertical centre line (x-axis snap). */
    public enum SnapX {
        NONE,
        LEFT_ON_CENTER,    // list's left  edge on the vertical centre line
        CENTER_ON_CENTER,  // list's centre     on the vertical centre line
        RIGHT_ON_CENTER    // list's right edge on the vertical centre line
    }

    /** How this list is anchored to the horizontal centre line (y-axis snap). */
    public enum SnapY {
        NONE,
        TOP_ON_CENTER,     // list's top    edge on the horizontal centre line
        CENTER_ON_CENTER,  // list's centre      on the horizontal centre line
        BOTTOM_ON_CENTER   // list's bottom edge on the horizontal centre line
    }

    public enum Stat {
        TPS, MSPT, FPS, PING, MEMORY, CPU,
        ENTITIES, CHUNKS, RENDERED_SECTIONS,
        COORDS, FACING, SPEED, GC_TIME,
        BIOME, LIGHT_LEVEL, DIMENSION
    }

    /** How much of the graph's current/min/max value readout is drawn. */
    public enum GraphValueDisplay {
        NONE, CURRENT, MIN_CURRENT_MAX
    }

    /** How a graph's plot is colored. */
    public enum GraphColorMode {
        /** Step 1's original behavior: whole graph tinted by the current value's threshold color. */
        CURRENT_THRESHOLD,
        /** Each historical sample colored by its own threshold value at the time it was recorded. */
        PER_SEGMENT_THRESHOLD,
        /** Ignore threshold coloring; use a single user-chosen accent color for the whole graph. */
        FIXED_ACCENT,
        /** Smooth color interpolation across the visible value range (blue→green→yellow→red). */
        GRADIENT
    }

    // ── Root config ───────────────────────────────────────────────────────────
    public int                  nextId = 1;
    public List<StatListConfig> lists  = new ArrayList<>();
    /** Global show/hide switch for the entire overlay */
    public boolean               overlayEnabled = true;

    // ── Per-stat settings ─────────────────────────────────────────────────────
    public static class StatSettings {
        /** Whether to show the label prefix (e.g. "TPS: ") before the value. */
        public boolean showPrefix = true;
        /** Decimal places for numeric stats that support it (TPS, MSPT, CPU, Speed). Ignored otherwise. */
        public int decimals = 1;
        /** Render graph. */
        public boolean renderAsGraph = false;
        /**
         * Visual styling for this stat's graph, used only when renderAsGraph is
         * true. Lazily backfilled (see backFill()/getGraphStyle()) so configs
         * written before this field existed still load cleanly.
         */
        public GraphStyle graphStyle = new GraphStyle();
    }

    /**
     * Per-graph visual settings: panel background, gridlines, peak markers,
     * value readout, smoothing, scale mode, size, and color mode. All fields
     * default to reproducing step 1's original look exactly (flat 80x28
     * single-tone fill colored by current-value threshold) so a user who
     * never opens the (future) styling GUI sees zero behavior change.
     */
    public static class GraphStyle {
        public boolean showPanelBackground = true;
        public boolean showGridlines = true;
        public boolean showPeakMarkers = true;
        public GraphValueDisplay valueDisplay = GraphValueDisplay.CURRENT; // NONE, CURRENT, MIN_CURRENT_MAX
        public int smoothing = 0; // 0 = off, else moving-average window size (2/3/4)
        public boolean autoScale = true;
        public float fixedMin = 0f;
        public float fixedMax = 100f;
        // NOTE: 80x28 (not a generic 60x20) because that's what this codebase's
        // step 1 actually shipped as MtssRenderer.GRAPH_W/GRAPH_H — defaults
        // here must reproduce the *existing* look exactly.
        public int width = 80;   // px, replaces step 1's hardcoded GRAPH_W constant
        public int height = 28;  // px, replaces step 1's hardcoded GRAPH_H constant
        public GraphColorMode colorMode = GraphColorMode.CURRENT_THRESHOLD; // CURRENT_THRESHOLD, PER_SEGMENT_THRESHOLD, FIXED_ACCENT, GRADIENT
        public int accentColor = 0xFF55FF55; // used when colorMode == FIXED_ACCENT

        public GraphStyle() {}

        public GraphStyle copy() {
            GraphStyle c = new GraphStyle();
            c.showPanelBackground = this.showPanelBackground;
            c.showGridlines       = this.showGridlines;
            c.showPeakMarkers     = this.showPeakMarkers;
            c.valueDisplay        = this.valueDisplay;
            c.smoothing           = this.smoothing;
            c.autoScale           = this.autoScale;
            c.fixedMin            = this.fixedMin;
            c.fixedMax            = this.fixedMax;
            c.width                = this.width;
            c.height               = this.height;
            c.colorMode            = this.colorMode;
            c.accentColor          = this.accentColor;
            return c;
        }
    }

    /** Stats for which the rolling graph render mode is available. */
    public static final java.util.Set<Stat> GRAPHABLE_STATS = java.util.Set.of(
            Stat.TPS, Stat.MSPT, Stat.FPS, Stat.CPU, Stat.PING, Stat.MEMORY, Stat.SPEED
    );

    // ── Per-list config ───────────────────────────────────────────────────────
    public static class StatListConfig {
        /** Unique identifier, starts from 0. */
        public int    id;
        /** User-visible name shown in the GUI. Defaults to "List N". */
        public String name;

        // Per-stat enabled flags, display order, and individual settings
        public Map<String, Boolean>      statEnabled  = defaultEnabledMap();
        public List<String>              statOrder    = defaultOrder();
        public Map<String, StatSettings> statSettings = new LinkedHashMap<>();

        // Position — corner anchor + pixel offsets
        public Corner anchorCorner = Corner.TOP_LEFT;
        /** Pixels from the anchor corner's horizontal edge. */
        public int    anchorDx    = 4;
        /** Pixels from the anchor corner's vertical edge. */
        public int    anchorDy    = 4;

        // Appearance
        public boolean showBackground = true;
        public boolean textShadow     = false;
        /** When true, overrideColor replaces each stat's normal/threshold color. */
        public boolean useCustomColor = false;
        /** ARGB color used when useCustomColor is true. Defaults to opaque white. */
        public int     overrideColor  = 0xFFFFFFFF;
        /** Text scale multiplier for this list, 0.5–2.0. */
        public float   textScale      = 1.0f;

        // Snap
        public SnapX snapX = SnapX.NONE;
        public SnapY snapY = SnapY.NONE;

        public StatListConfig() {}

        public StatListConfig(int id) {
            this.id   = id;
            this.name = "List " + id;
        }

        private static Map<String, Boolean> defaultEnabledMap() {
            Map<String, Boolean> m = new LinkedHashMap<>();
            for (Stat s : Stat.values()) {
                m.put(s.name(), s == Stat.TPS || s == Stat.MSPT || s == Stat.FPS);
            }
            return m;
        }

        private static List<String> defaultOrder() {
            List<String> l = new ArrayList<>();
            for (Stat s : Stat.values()) l.add(s.name());
            return l;
        }

        public boolean isEnabled(Stat stat) {
            return statEnabled.getOrDefault(stat.name(), false);
        }

        public void setEnabled(Stat stat, boolean value) {
            statEnabled.put(stat.name(), value);
        }

        /** Returns stats in statOrder order, enabled only, no duplicates. */
        public List<Stat> getVisibleStats() {
            List<Stat> result = new ArrayList<>();
            for (String n : statOrder) {
                try {
                    Stat s = Stat.valueOf(n.toUpperCase());
                    if (isEnabled(s) && !result.contains(s)) result.add(s);
                } catch (IllegalArgumentException ignored) {}
            }
            return result;
        }

        /** Gets (or lazily creates) the settings for a stat. */
        public StatSettings getStatSettings(Stat stat) {
            StatSettings ss = statSettings.computeIfAbsent(stat.name(), k -> new StatSettings());
            if (ss.graphStyle == null) ss.graphStyle = new GraphStyle();
            return ss;
        }

        /** Back-fill any stats added since this config was written. */
        public void backFill() {
            if (name == null) name = "List " + id;
            if (statSettings == null) statSettings = new LinkedHashMap<>();
            for (Stat s : Stat.values()) {
                statEnabled.putIfAbsent(s.name(), false);
                if (!statOrder.contains(s.name())) statOrder.add(s.name());
                StatSettings ss = statSettings.computeIfAbsent(s.name(), k -> new StatSettings());
                if (ss.graphStyle == null) ss.graphStyle = new GraphStyle();
            }
        }

        /** Display name used in the GUI header. */
        public String displayName() {
            return (name == null || name.isBlank()) ? "List " + id : name;
        }

        /** Deep-copies this list's settings into a brand-new list with the given id, offset slightly. */
        public StatListConfig duplicate(int newId) {
            StatListConfig copy = new StatListConfig(newId);
            copy.name           = displayName() + " (copy)";
            copy.statEnabled    = new LinkedHashMap<>(this.statEnabled);
            copy.statOrder      = new ArrayList<>(this.statOrder);
            copy.statSettings   = new LinkedHashMap<>();
            for (Map.Entry<String, StatSettings> e : this.statSettings.entrySet()) {
                StatSettings src = e.getValue();
                StatSettings dst = new StatSettings();
                dst.showPrefix    = src.showPrefix;
                dst.decimals      = src.decimals;
                dst.renderAsGraph = src.renderAsGraph;
                dst.graphStyle    = (src.graphStyle != null ? src.graphStyle : new GraphStyle()).copy();
                copy.statSettings.put(e.getKey(), dst);
            }
            copy.anchorCorner   = this.anchorCorner;
            copy.anchorDx       = this.anchorDx + 12;
            copy.anchorDy       = this.anchorDy + 12;
            copy.showBackground = this.showBackground;
            copy.textShadow     = this.textShadow;
            copy.useCustomColor = this.useCustomColor;
            copy.overrideColor  = this.overrideColor;
            copy.textScale      = this.textScale;
            copy.snapX          = this.snapX;
            copy.snapY          = this.snapY;
            return copy;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public StatListConfig createList() {
        StatListConfig cfg = new StatListConfig(nextId++);
        cfg.anchorDx = 4 + lists.size() * 20;
        cfg.anchorDy = 4 + lists.size() * 20;
        lists.add(cfg);
        return cfg;
    }

    public void removeList(int id) {
        lists.removeIf(l -> l.id == id);
    }

    /** Duplicates the given list (by id) and appends the copy. Returns the new list, or null if not found. */
    public StatListConfig duplicateList(int id) {
        for (StatListConfig lc : lists) {
            if (lc.id == id) {
                StatListConfig copy = lc.duplicate(nextId++);
                lists.add(copy);
                return copy;
            }
        }
        return null;
    }

    // ── Persistence ───────────────────────────────────────────────────────────
    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("mtss.json");

    private static MtssConfig INSTANCE;

    public static MtssConfig getInstance() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    public static MtssConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
                MtssConfig cfg = GSON.fromJson(r, MtssConfig.class);
                if (cfg != null) {
                    // Note: overlayEnabled needs no explicit backfill here. MtssConfig has
                    // an implicit no-arg constructor, so Gson constructs it that way (not via
                    // Unsafe), meaning the `= true` field initializer already runs before the
                    // JSON is applied, a pre-existing config file with no "overlayEnabled" key
                    // simply leaves that default in place.
                    if (cfg.lists == null) cfg.lists = new ArrayList<>();
                    for (StatListConfig list : cfg.lists) {
                        if (list.statEnabled == null) list.statEnabled = new LinkedHashMap<>();
                        if (list.statOrder   == null) list.statOrder   = new ArrayList<>();
                        // anchorCorner field renamed from alignment — treat null as TOP_LEFT
                        if (list.anchorCorner == null) list.anchorCorner = Corner.TOP_LEFT;
                        list.backFill();
                    }
                    return cfg;
                }
            } catch (IOException e) {
                System.err.println("[MTSS] Failed to load config: " + e.getMessage());
            }
        }
        MtssConfig defaults = new MtssConfig();
        defaults.lists.add(new StatListConfig(0));
        defaults.nextId = 1;
        defaults.save();
        return defaults;
    }

    public void save() {
        try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            System.err.println("[MTSS] Failed to save config: " + e.getMessage());
        }
    }
}
