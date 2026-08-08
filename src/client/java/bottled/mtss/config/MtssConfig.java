package bottled.mtss.config;

import bottled.mtss.hud.TemplateEngine;
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
     * Which screen corner a list is anchored to. anchorFracX/anchorFracY are
     * offsets from that corner's edges, normalized to [0, 1] as a fraction of
     * the current screen width/height, so the list stays in the same visual
     * spot when the GUI scale (and therefore the effective screen size) changes.
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
        COORDS, X, Y, Z, FACING, YAW, PITCH, SPEED, GC_TIME,
        BIOME, LIGHT_LEVEL, DIMENSION,
        // ── Player vitals ────────────────────────────────────────────────
        HEALTH, HUNGER, SATURATION, ARMOR, AIR,
        XP_LEVEL, XP_PROGRESS, GAME_MODE, SELECTED_SLOT, HELD_ITEM,
        // ── World / environment ─────────────────────────────────────────
        WEATHER, DIFFICULTY,
        SKY_LIGHT, BLOCK_LIGHT, CAN_SEE_SKY,
        // ── Server / session ─────────────────────────────────────────────
        PLAYERS_ONLINE, DISTANCE_FROM_SPAWN, CHUNK_POS, VERTICAL_SPEED,
        // ── Targeting / movement ────────────────────────────────────────
        LOOKING_AT, MOVING
    }

    /**
     * Groups {@link Stat} constants for the redesigned toggle/reorder panel
     * (see {@code bottled.mtss.gui.panel.ReorderPanel}). Purely a GUI
     * organization concern — rendering, sampling, and Template Mode don't
     * care about categories at all, so adding a stat to the wrong category
     * (or forgetting it) only affects where it's *listed* in the editor, not
     * whether it works. {@link #categoryOf(Stat)} is the single source of
     * truth; every {@link Stat} constant must appear in exactly one bucket
     * there or it silently won't show up in the categorized panel.
     */
    public enum StatCategory {
        PERFORMANCE, PLAYER, WORLD, POSITION
    }

    /** Which {@link StatCategory} a stat belongs to in the toggle panel. Every Stat must be covered — see the class doc above. */
    public static StatCategory categoryOf(Stat stat) {
        return switch (stat) {
            case TPS, MSPT, FPS, PING, MEMORY, CPU, GC_TIME, RENDERED_SECTIONS, PLAYERS_ONLINE -> StatCategory.PERFORMANCE;
            case HEALTH, HUNGER, SATURATION, ARMOR, AIR, XP_LEVEL, XP_PROGRESS, GAME_MODE, SELECTED_SLOT, HELD_ITEM, SPEED, VERTICAL_SPEED, MOVING -> StatCategory.PLAYER;
            case ENTITIES, CHUNKS, BIOME, DIMENSION, WEATHER, DIFFICULTY, LIGHT_LEVEL, SKY_LIGHT, BLOCK_LIGHT, CAN_SEE_SKY, LOOKING_AT -> StatCategory.WORLD;
            case COORDS, X, Y, Z, FACING, YAW, PITCH, CHUNK_POS, DISTANCE_FROM_SPAWN -> StatCategory.POSITION;
        };
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

        // Position — corner anchor + normalized offsets
        public Corner anchorCorner = Corner.TOP_LEFT;
        /**
         * Offset from the anchor corner's horizontal edge, normalized as a
         * fraction of screen width (0.0–1.0ish). Resolved to pixels at
         * render/layout time via {@code screenW * anchorFracX}. Kept as a
         * fraction (rather than raw pixels) so the list's on-screen position
         * is stable across GUI scale changes, which change the effective
         * screen width/height without this config changing.
         */
        public double anchorFracX = 0.01;
        /** Offset from the anchor corner's vertical edge, normalized as a fraction of screen height. See {@link #anchorFracX}. */
        public double anchorFracY = 0.01;

        /**
         * Legacy raw-pixel offsets from before positions were normalized.
         * Present only so old config files' "anchorDx"/"anchorDy" JSON keys
         * still deserialize into something {@link #backFill()} can migrate
         * from. Null (the default) means "not present in the loaded JSON" —
         * Gson leaves object-typed fields null rather than running the field
         * initializer when the key is absent. Nulled back out once migrated,
         * so this doesn't re-run on later loads and isn't written by fresh
         * saves going forward.
         */
        public Integer anchorDx = null;
        public Integer anchorDy = null;

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

            // Migrate old raw-pixel anchorDx/anchorDy (pre-normalized-position
            // configs) into normalized anchorFracX/Y. Old configs assumed a
            // 1x-GUI-scale-ish screen size at save time, which we don't know
            // anymore — but converting using a reasonable reference size gets
            // existing lists close to where they were, and from then on their
            // position is scale-stable. Only run once: anchorDx/anchorDy are
            // nulled out after migrating so this doesn't re-run on later loads
            // and doesn't clobber a position the user has since re-dragged.
            if (anchorDx != null || anchorDy != null) {
                final double REFERENCE_W = 320.0; // matches Minecraft's default GUI-scaled width at scale 1 on a common 1080p display
                final double REFERENCE_H = 240.0;
                anchorFracX = (anchorDx != null ? anchorDx : 4) / REFERENCE_W;
                anchorFracY = (anchorDy != null ? anchorDy : 4) / REFERENCE_H;
                anchorDx = null;
                anchorDy = null;
            }

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
            // Nudge the copy so it doesn't sit exactly on top of the original.
            // 0.02 of screen size lands close to the old 12px-at-reference-size
            // nudge without hardcoding a pixel amount that would drift when
            // rendered at a different GUI scale than duplication happened at.
            copy.anchorFracX    = anchorFracX + 0.02;
            copy.anchorFracY    = anchorFracY + 0.02;
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
        // Stagger new lists diagonally so they don't stack on top of each
        // other, same idea as before but expressed as a screen fraction so
        // the stagger looks the same regardless of GUI scale.
        cfg.anchorFracX = 0.01 + lists.size() * 0.05;
        cfg.anchorFracY = 0.01 + lists.size() * 0.05;
        lists.add(cfg);
        return cfg;
    }

    public void removeList(int id) {
        lists.removeIf(l -> l.id == id);
        TemplateEngine.invalidate(id); // drop any cached template parse/warn state for the deleted list
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
            } catch (IOException | com.google.gson.JsonSyntaxException | com.google.gson.JsonIOException e) {
                // Corrupt/truncated JSON (e.g. from a crash before the atomic-save
                // fix, or manual editing gone wrong) used to silently fall through
                // to fresh defaults, discarding the user's lists with no trace.
                // Preserve the broken file next to a timestamped ".bak" instead,
                // so a config that fails to parse is recoverable rather than
                // just gone, then continue on to fresh defaults below.
                System.err.println("[MTSS] Failed to load config (" + e.getMessage() + "); backing up and starting fresh.");
                backupCorruptConfig();
            }
        }
        MtssConfig defaults = new MtssConfig();
        defaults.lists.add(new StatListConfig(0));
        defaults.nextId = 1;
        defaults.save();
        return defaults;
    }

    /** Copies an unparsable mtss.json aside as "mtss.json.bak-<timestamp>" instead of letting it be silently overwritten. Best-effort — a failure here still lets startup continue. */
    private static void backupCorruptConfig() {
        try {
            Path backup = CONFIG_PATH.resolveSibling(
                    CONFIG_PATH.getFileName() + ".bak-" + System.currentTimeMillis());
            Files.copy(CONFIG_PATH, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.err.println("[MTSS] Backed up unreadable config to " + backup);
        } catch (IOException copyFailed) {
            System.err.println("[MTSS] Could not back up unreadable config: " + copyFailed.getMessage());
        }
    }

    /**
     * Writes the config atomically: serialize to a sibling ".tmp" file, then
     * {@link Files#move} it over the real path. A plain
     * {@code Files.newBufferedWriter(CONFIG_PATH)} truncates the existing
     * file before writing a byte of the new content — if the game (or the
     * process) dies mid-write, e.g. a crash, alt-F4, or the OS killing a
     * hung JVM, {@code mtss.json} is left as a partial/empty file and every
     * list the user configured is gone on next launch. {@link Files#move}
     * with {@code ATOMIC_MOVE} guarantees the destination is either the old
     * complete file or the new complete file, never a half-written one, so
     * a crash during save can lose at most the in-flight edit, never the
     * whole config. Falls back to a non-atomic move if the filesystem
     * doesn't support atomic renames across these paths (rare, but some
     * network/overlay filesystems don't) rather than failing the save outright.
     */
    public void save() {
        Path tmp = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
        try {
            try (Writer w = Files.newBufferedWriter(tmp)) {
                GSON.toJson(this, w);
            }
            try {
                Files.move(tmp, CONFIG_PATH,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, CONFIG_PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("[MTSS] Failed to save config: " + e.getMessage());
            // Best-effort cleanup so a failed save doesn't leave a stray .tmp
            // file behind to confuse the next save attempt.
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }
}
