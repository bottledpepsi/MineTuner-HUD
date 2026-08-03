package bottled.mtss.config;

import bottled.mtss.stat.StatDefinition;
import bottled.mtss.stat.StatRegistry;
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
import java.util.Set;
import java.util.stream.Collectors;

public class MtssConfig {

    /**
     * Which screen corner a list is anchored to. anchorDx/anchorDy are
     * pixel offsets from that corner, so the list moves with it on resize.
     */
    public enum Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    /** How this list snaps to the vertical centre line (x-axis). */
    public enum SnapX {
        NONE,
        LEFT_ON_CENTER,    // left edge on the centre line
        CENTER_ON_CENTER,  // centre on the centre line
        RIGHT_ON_CENTER    // right edge on the centre line
    }

    /** How this list snaps to the horizontal centre line (y-axis). */
    public enum SnapY {
        NONE,
        TOP_ON_CENTER,     // top edge on the centre line
        CENTER_ON_CENTER,  // centre on the centre line
        BOTTOM_ON_CENTER   // bottom edge on the centre line
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
        /** Whole graph tinted by the current value's threshold color. */
        CURRENT_THRESHOLD,
        /** Each historical sample colored by its own value at the time it was recorded. */
        PER_SEGMENT_THRESHOLD,
        /** A single user-chosen accent color for the whole graph. */
        FIXED_ACCENT,
        /** Smooth color interpolation across the visible range (blue→green→yellow→red). */
        GRADIENT
    }

    // ── Root config ───────────────────────────────────────────────────────────
    public int                  nextId = 1;
    public List<StatListConfig> lists  = new ArrayList<>();
    /** Global show/hide switch for the entire overlay */
    public boolean               overlayEnabled = true;

    // ── Per-stat settings ─────────────────────────────────────────────────────
    public static class StatSettings {
        /** Show the label prefix (e.g. "TPS: ") before the value. */
        public boolean showPrefix = true;
        /** Decimal places for stats that support it (TPS, MSPT, CPU, Speed). Ignored otherwise. */
        public int decimals = 1;
        /** Render as a graph instead of text. */
        public boolean renderAsGraph = false;
        /** Graph visuals, used only when renderAsGraph is true. Lazily backfilled so old configs still load. */
        public GraphStyle graphStyle = new GraphStyle();

        public StatSettings copy() {
            StatSettings c = new StatSettings();
            c.showPrefix    = showPrefix;
            c.decimals      = decimals;
            c.renderAsGraph = renderAsGraph;
            c.graphStyle    = (graphStyle != null ? graphStyle : new GraphStyle()).copy();
            return c;
        }
    }

    /**
     * Per-graph visual settings. Defaults reproduce the original look
     * (flat 80x28 fill colored by current-value threshold) so a list that
     * hasn't been styled looks unchanged.
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
        public int width = 80;   // px
        public int height = 28;  // px
        public GraphColorMode colorMode = GraphColorMode.CURRENT_THRESHOLD; // CURRENT_THRESHOLD, PER_SEGMENT_THRESHOLD, FIXED_ACCENT, GRADIENT
        public int accentColor = 0xFF55FF55; // used when colorMode == FIXED_ACCENT

        public GraphStyle() {}

        /** Field-by-field copy — GraphStyle is a flat Gson POJO, so there's no shortcut around listing every field once. */
        public GraphStyle copy() {
            GraphStyle c = new GraphStyle();
            c.showPanelBackground = showPanelBackground;
            c.showGridlines       = showGridlines;
            c.showPeakMarkers     = showPeakMarkers;
            c.valueDisplay        = valueDisplay;
            c.smoothing           = smoothing;
            c.autoScale           = autoScale;
            c.fixedMin            = fixedMin;
            c.fixedMax            = fixedMax;
            c.width               = width;
            c.height              = height;
            c.colorMode           = colorMode;
            c.accentColor         = accentColor;
            return c;
        }
    }

    /**
     * Stats for which the rolling graph render mode is available. Derived
     * from each stat's own {@code StatDefinition.supportsGraph()} — a new
     * stat opts in just by overriding that method, no list to update here.
     */
    public static final Set<Stat> GRAPHABLE_STATS = StatRegistry.all().stream()
            .filter(StatDefinition::supportsGraph)
            .map(StatDefinition::key)
            .collect(Collectors.toUnmodifiableSet());

    /** Same idea as {@link #GRAPHABLE_STATS}, driven by {@code supportsThreshold()}. */
    public static final Set<Stat> THRESHOLD_STATS = StatRegistry.all().stream()
            .filter(StatDefinition::supportsThreshold)
            .map(StatDefinition::key)
            .collect(Collectors.toUnmodifiableSet());

    /**
     * A user-configurable two-cutoff threshold for a stat's green/yellow/red
     * coloring, per stat, per list.
     * <p>
     * Direction matters: for "higher is better" stats (TPS, FPS) green is the
     * high end. For "lower is better" stats (Ping, Memory, CPU) green is the
     * low end — the color functions flip the comparison accordingly, even
     * though the field names (goodMin/warnMin) stay the same.
     * <p>
     * Defaults (see defaultThresholds()) come from each stat's own
     * {@code StatDefinition.defaultGoodMin()}/{@code defaultWarnMin()} — see
     * {@code bottled.mtss.stat.stats} for the actual numbers per stat.
     * Speed isn't part of this system — its gray/yellow/white logic isn't a
     * good/warn/bad scale, so it keeps its own hardcoded behavior
     * ({@code supportsThreshold()} returns false there).
     */
    public static class ThresholdSettings {
        /** false = ignore goodMin/warnMin and use the built-in default. */
        public boolean enabled = false;
        /** Higher-is-better: value at/above this is "good" (green). Lower-is-better: value at/below this is "good" (green). */
        public float goodMin;
        /** Higher-is-better: value at/above this (below goodMin) is "warning" (yellow), below is "bad" (red). Lower-is-better: mirrored. */
        public float warnMin;

        public ThresholdSettings() {}

        public ThresholdSettings(boolean enabled, float goodMin, float warnMin) {
            this.enabled = enabled;
            this.goodMin = goodMin;
            this.warnMin = warnMin;
        }

        public ThresholdSettings copy() {
            return new ThresholdSettings(this.enabled, this.goodMin, this.warnMin);
        }
    }

    /** Built straight from each threshold stat's own defaultGoodMin()/defaultWarnMin() — see the values there, not here. */
    private static Map<String, ThresholdSettings> defaultThresholds() {
        Map<String, ThresholdSettings> m = new LinkedHashMap<>();
        for (Stat s : THRESHOLD_STATS) {
            StatDefinition def = StatRegistry.get(s);
            m.put(s.name(), new ThresholdSettings(false, def.defaultGoodMin(), def.defaultWarnMin()));
        }
        return m;
    }

    // ── Per-list config ───────────────────────────────────────────────────────
    public static class StatListConfig {
        /** Unique id, starts from 0. */
        public int    id;
        /** User-visible name shown in the GUI. Defaults to "List N". */
        public String name;

        // Per-stat enabled flags, display order, and individual settings
        public Map<String, Boolean>      statEnabled  = defaultEnabledMap();
        public List<String>              statOrder    = defaultOrder();
        public Map<String, StatSettings> statSettings = new LinkedHashMap<>();
        /**
         * Per-stat color thresholds, keyed by Stat.name(). Only entries for
         * stats in THRESHOLD_STATS matter. Populated disabled by default so
         * existing configs render the same until a user opts in.
         */
        public Map<String, ThresholdSettings> statThresholds = defaultThresholds();

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

        // ── Template mode ────────────────────────────────────────────────────
        /**
         * false (default) = classic per-stat-line mode. true = render
         * {@link #templateLines} instead. Old configs load as false, so
         * nothing changes unless a list opts in.
         */
        public boolean useTemplate = false;
        /**
         * One markup string per rendered line, used only when
         * {@link #useTemplate} is true. See {@code TemplateEngine} for the
         * token grammar and the README's "Template Mode" section for the
         * full token table.
         */
        public List<String> templateLines = new ArrayList<>();

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

        /**
         * The custom threshold for a stat, or null if the stat isn't
         * threshold-colorable or has no entry. Treat both null and
         * "enabled == false" as "use the built-in default".
         */
        public ThresholdSettings getThreshold(Stat stat) {
            if (!THRESHOLD_STATS.contains(stat)) return null;
            return statThresholds.get(stat.name());
        }

        /** Back-fills any fields added since this config was written. */
        public void backFill() {
            if (name == null) name = "List " + id;
            if (statSettings == null) statSettings = new LinkedHashMap<>();
            if (statThresholds == null) statThresholds = new LinkedHashMap<>();
            // Gson leaves this null (not the field initializer) if it's missing
            // from an old config's JSON.
            if (templateLines == null) templateLines = new ArrayList<>();

            for (Stat s : Stat.values()) {
                statEnabled.putIfAbsent(s.name(), false);
                if (!statOrder.contains(s.name())) statOrder.add(s.name());
                StatSettings ss = statSettings.computeIfAbsent(s.name(), k -> new StatSettings());
                if (ss.graphStyle == null) ss.graphStyle = new GraphStyle();
            }
            // Built once, not per-stat — defaultThresholds() allocates a fresh
            // map every call, so calling it inside the loop below would
            // rebuild the whole thing for every missing entry.
            Map<String, ThresholdSettings> defaults = defaultThresholds();
            for (Stat s : THRESHOLD_STATS) {
                statThresholds.computeIfAbsent(s.name(), defaults::get);
            }
        }

        /** Display name used in the GUI header. */
        public String displayName() {
            return (name == null || name.isBlank()) ? "List " + id : name;
        }

        /** Deep-copies this list's settings into a brand-new list with the given id, offset slightly. */
        public StatListConfig duplicate(int newId) {
            StatListConfig copy = new StatListConfig(newId);
            copy.name         = displayName() + " (copy)";
            copy.statEnabled  = new LinkedHashMap<>(statEnabled);
            copy.statOrder    = new ArrayList<>(statOrder);

            copy.statSettings = new LinkedHashMap<>();
            statSettings.forEach((key, src) -> copy.statSettings.put(key, src.copy()));

            copy.statThresholds = new LinkedHashMap<>();
            statThresholds.forEach((key, src) -> copy.statThresholds.put(key, src.copy()));

            copy.anchorCorner   = anchorCorner;
            copy.anchorDx       = anchorDx + 12;
            copy.anchorDy       = anchorDy + 12;
            copy.showBackground = showBackground;
            copy.textShadow     = textShadow;
            copy.useCustomColor = useCustomColor;
            copy.overrideColor  = overrideColor;
            copy.textScale      = textScale;
            copy.snapX          = snapX;
            copy.snapY          = snapY;
            copy.useTemplate    = useTemplate;
            copy.templateLines  = new ArrayList<>(templateLines);
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
                    // overlayEnabled needs no backfill: Gson uses the no-arg
                    // constructor, so the `= true` field initializer already
                    // ran before the JSON was applied, and an old config file
                    // with no "overlayEnabled" key just leaves it in place.
                    if (cfg.lists == null) cfg.lists = new ArrayList<>();
                    for (StatListConfig list : cfg.lists) {
                        if (list.statEnabled == null) list.statEnabled = new LinkedHashMap<>();
                        if (list.statOrder   == null) list.statOrder   = new ArrayList<>();
                        // Null for configs written before this field existed —
                        // backFill() below fills in the defaults.
                        if (list.statThresholds == null) list.statThresholds = new LinkedHashMap<>();
                        if (list.templateLines == null) list.templateLines = new ArrayList<>();

                        // anchorCorner was renamed from "alignment" — null means TOP_LEFT
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
