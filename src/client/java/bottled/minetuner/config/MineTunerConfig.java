package bottled.minetuner.config;

import bottled.minetuner.MineTunerMod;
import bottled.minetuner.hud.TemplateEngine;
import bottled.minetuner.stat.StatDefinition;
import bottled.minetuner.stat.StatRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class MineTunerConfig {

    /** Stats for which the rolling graph render mode is available. */
    public static final Set<Stat> GRAPHABLE_STATS = StatRegistry.all().stream()
            .filter(StatDefinition::supportsGraph)
            .map(StatDefinition::key)
            .collect(Collectors.toUnmodifiableSet());
    /** Same idea as {@link #GRAPHABLE_STATS}, driven by {@code supportsThreshold()}. */
    public static final Set<Stat> THRESHOLD_STATS = StatRegistry.all().stream()
            .filter(StatDefinition::supportsThreshold)
            .map(StatDefinition::key)
            .collect(Collectors.toUnmodifiableSet());

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("minetuner.json");
    private static MineTunerConfig INSTANCE;

    public int nextId = 1;
    public List<StatListConfig> lists = new ArrayList<>();
    /** Global show/hide switch for the entire overlay. */
    public boolean overlayEnabled = true;

    /** Name of the built-in theme matching MineTuner's original, pre-Themes
     *  hardcoded default appearance (showBackground=true, textShadow=false,
     *  useCustomColor=false, textScale=1.0, default GraphStyle for every stat).
     *  Also the permanent fallback for a dangling {@link #defaultThemeName}. */
    public static final String BUILTIN_DEFAULT_THEME = "Classic";

    /** Named, reusable appearance presets (see {@link ListTheme}), keyed by name.
     *  Global — server-wide, not per-list — so a theme can be applied to any list.
     *  A {@link LinkedHashMap} preserves insertion order for stable GUI display,
     *  the same ordering-preservation approach {@code statOrder} uses for stats. */
    public Map<String, ListTheme> themes = new LinkedHashMap<>();
    /** Name of the theme (a key into {@link #themes}) applied to every brand-new
     *  list created via "Add List". Never affects existing lists. Falls back to
     *  {@link #BUILTIN_DEFAULT_THEME} if it ever points at a deleted theme. */
    public String defaultThemeName = BUILTIN_DEFAULT_THEME;

    public boolean hardwareSensorsEnabled = false;
    public String hardwareSensorBaseUrl = "http://localhost:8085";
    public int hardwareSensorPollIntervalMs = 1500;
    public int hardwareSensorRequestTimeoutMs = 300;

    public int reorderPanelMaxVisibleRows = 16;
    /** Height, in pixels, of one row in every editor panel (stat rows, menu rows, etc). */
    public int panelRowHeight = 13;
    /** Width, in pixels, of the standard (narrow) editor panels. */
    public int panelWidth = 160;

    // Previously hardcoded constants scattered across the editor's panel
    // classes (ReorderPanel, PanelChrome, MineTunerGuiScreen). Centralized here so
    // they're both a single source of truth for the panels and editable from
    // the Cloth Config screen (bottled.minetuner.config.cloth). Defaults below
    // exactly match the old hardcoded values, so existing configs behave
    // identically until a user opts into changing them.
    /** Width, in pixels, of the wider "Edit Stats" reorder panel. */
    public int widePanelWidth = 216;
    /** Inner padding, in pixels, applied on all sides of every editor panel. */
    public int panelPadding = 4;
    /** Distance, in pixels, within which a dragged list snaps to the screen's
     *  edges/center lines (see {@link SnapX}/{@link SnapY}). */
    public int dragSnapThresholdPx = 6;
    /** Lower bound offered by the Text Scale slider/stepper, and the floor a list's
     *  own textScale is clamped to when adjusted via the "-" button or Cloth Config. */
    public float textScaleMin = 0.5f;
    /** Upper bound offered by the Text Scale slider/stepper, and the ceiling a
     *  list's own textScale is clamped to when adjusted via the "+" button or Cloth Config. */
    public float textScaleMax = 2.0f;

    /** Which {@link StatCategory} a stat belongs to in the toggle panel. */
    public static StatCategory categoryOf(Stat stat) {
        return switch (stat) {
            case TPS, MSPT, FPS, FRAMETIME, FPS_AVG, FPS_MIN, FPS_MAX, FPS_1PCT_LOW, FPS_01PCT_LOW, PING, MEMORY, CPU, GC_TIME,
                 RENDERED_SECTIONS, PLAYERS_ONLINE,
                 GPU_TEMP, GPU_CLOCK, GPU_USAGE, VRAM_USED -> StatCategory.PERFORMANCE;
            case HEALTH, HUNGER, SATURATION, ARMOR, AIR, XP_LEVEL, XP_PROGRESS, GAME_MODE, SELECTED_SLOT, HELD_ITEM,
                 SPEED, VERTICAL_SPEED, MOVING -> StatCategory.PLAYER;
            case ENTITIES, CHUNKS, BIOME, DIMENSION, WEATHER, DIFFICULTY, LIGHT_LEVEL, SKY_LIGHT, BLOCK_LIGHT,
                 CAN_SEE_SKY, LOOKING_AT -> StatCategory.WORLD;
            case COORDS, X, Y, Z, FACING, YAW, PITCH, CHUNK_POS, DISTANCE_FROM_SPAWN -> StatCategory.POSITION;
        };
    }

    /** Built straight from each threshold stat's own defaultGoodMin()/defaultWarnMin(). */
    private static Map<String, ThresholdSettings> defaultThresholds() {
        Map<String, ThresholdSettings> m = new LinkedHashMap<>();
        for (Stat s : THRESHOLD_STATS) {
            StatDefinition def = StatRegistry.get(s);
            m.put(s.name(), new ThresholdSettings(false, def.defaultGoodMin(), def.defaultWarnMin()));
        }
        return m;
    }

    public static MineTunerConfig getInstance() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    public static MineTunerConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
                MineTunerConfig cfg = GSON.fromJson(r, MineTunerConfig.class);
                if (cfg != null) {
                    // overlayEnabled needs no backfill: MineTunerConfig has no custom
                    // constructor, so the `= true` field initializer already
                    // ran before the JSON was applied, and an old config file
                    // with no "overlayEnabled" key just leaves it in place.
                    // Same is true of every newly-added primitive field below
                    // (hardwareSensorPollIntervalMs, panelRowHeight, etc.):
                    // Gson only overwrites a field when its key is present in
                    // the JSON, so an old minetuner.json missing these keys just
                    // keeps the `= <default>` values from the field
                    // initializers. clampGuiTuning() below only guards
                    // against out-of-range *values* a user (or a future
                    // Cloth Config edit) actually wrote, not missing ones.
                    cfg.clampGuiTuning();
                    // Old configs (pre-Themes) have no "themes" key at all, and Gson
                    // leaves a null Map field null rather than constructing an empty
                    // one — unlike defaultThemeName just below, which is a simple
                    // String and so behaves the same as overlayEnabled/panelRowHeight
                    // above: its `= BUILTIN_DEFAULT_THEME` field initializer already
                    // ran, so an old config missing the "defaultThemeName" key just
                    // keeps that default. The check here is only a defensive guard
                    // in case a hand-edited config explicitly sets it to null.
                    if (cfg.lists == null) cfg.lists = new ArrayList<>();
                    if (cfg.themes == null) cfg.themes = new LinkedHashMap<>();
                    if (cfg.defaultThemeName == null) cfg.defaultThemeName = BUILTIN_DEFAULT_THEME;
                    // backFillThemes() populates the built-in themes (Classic/Minimal/
                    // High Contrast) into cfg.themes and ensures defaultThemeName
                    // always resolves to a real entry in it.
                    cfg.backFillThemes();
                    for (StatListConfig list : cfg.lists) {
                        if (list.statEnabled == null) list.statEnabled = new LinkedHashMap<>();
                        if (list.statOrder == null) list.statOrder = new ArrayList<>();
                        // Null for configs written before this field existed;
                        // backFill() below fills in the defaults.
                        if (list.statThresholds == null) list.statThresholds = new LinkedHashMap<>();
                        if (list.templateLines == null) list.templateLines = new ArrayList<>();

                        // anchorCorner was renamed from "alignment".
                        if (list.anchorCorner == null) list.anchorCorner = Corner.TOP_LEFT;
                        list.backFill();
                    }
                    return cfg;
                }
            } catch (IOException | com.google.gson.JsonSyntaxException | com.google.gson.JsonIOException e) {
                // Corrupt/truncated JSON (e.g. a crash mid-write before the atomic-save
                // fix, or manual editing gone wrong) used to silently fall through
                // to fresh defaults, discarding the user's lists with no trace.
                // Preserve the broken file next to a timestamped ".bak" instead,
                // so a config that fails to parse is recoverable rather than
                // just gone, then continue on to fresh defaults below.
                MineTunerMod.LOGGER.error("Failed to load config ({}); backing up and starting fresh.", e.getMessage());
                backupCorruptConfig();
            }
        }
        MineTunerConfig defaults = new MineTunerConfig();
        defaults.backFillThemes();
        defaults.lists.add(new StatListConfig(0));
        defaults.nextId = 1;
        defaults.save();
        return defaults;
    }

    /** Copies an unparsable minetuner.json aside as "minetuner.json.bak-<timestamp>" instead. */
    private static void backupCorruptConfig() {
        try {
            Path backup = CONFIG_PATH.resolveSibling(
                    CONFIG_PATH.getFileName() + ".bak-" + System.currentTimeMillis());
            Files.copy(CONFIG_PATH, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            MineTunerMod.LOGGER.error("Backed up unreadable config to {}", backup);
        } catch (IOException copyFailed) {
            MineTunerMod.LOGGER.error("Could not back up unreadable config: {}", copyFailed.getMessage());
        }
    }

    /** Clamps every GUI/hardware-sensor tuning field to a sane floor (and, where it
     *  makes sense — e.g. widePanelWidth vs. panelWidth, textScaleMax vs. textScaleMin —
     *  to be no smaller than a related field), so a hand-edited or corrupted config
     *  value can't produce a broken layout or a runaway polling loop. */
    public void clampGuiTuning() {
        reorderPanelMaxVisibleRows = Math.max(3, reorderPanelMaxVisibleRows);
        panelRowHeight = Math.max(6, panelRowHeight);
        panelWidth = Math.max(60, panelWidth);
        widePanelWidth = Math.max(panelWidth, widePanelWidth);
        panelPadding = Math.max(0, panelPadding);
        dragSnapThresholdPx = Math.max(0, dragSnapThresholdPx);
        textScaleMin = Math.max(0.1f, textScaleMin);
        textScaleMax = Math.max(textScaleMin, textScaleMax);
        hardwareSensorPollIntervalMs = Math.max(100, hardwareSensorPollIntervalMs);
        hardwareSensorRequestTimeoutMs = Math.max(50, hardwareSensorRequestTimeoutMs);

        bottled.minetuner.gui.render.PanelChrome.syncFromConfig(this);
        bottled.minetuner.gui.panel.ReorderPanel.syncFromConfig(this);
    }

    /** Creates a brand-new, empty-starting-point list and applies the configured
     *  {@link #defaultThemeName} theme to it, so a freshly-created list immediately
     *  reflects the user's chosen default appearance instead of MineTuner's
     *  originally-hardcoded appearance defaults. This is the ONLY list-construction
     *  path that applies the default theme — {@link #duplicateList(int)} below
     *  intentionally does not, since duplicating must keep copying the source
     *  list's actual current appearance regardless of defaultThemeName. */
    public StatListConfig createList() {
        StatListConfig cfg = new StatListConfig(nextId++);
        // Stagger new lists diagonally so they don't stack on top of each
        // other — same idea as before but expressed as a screen fraction so
        // the stagger looks the same regardless of GUI scale.
        cfg.anchorFracX = 0.01 + lists.size() * 0.05;
        cfg.anchorFracY = 0.01 + lists.size() * 0.05;
        resolveDefaultTheme().applyTo(cfg);
        lists.add(cfg);
        return cfg;
    }

    public void removeList(int id) {
        lists.removeIf(l -> l.id == id);
        TemplateEngine.invalidate(id); // drop any cached template parse/warn state for the deleted list.
    }

    /** Duplicates the given list (by id) and appends the copy. Deliberately does NOT
     *  touch themes/defaultThemeName — a duplicate always copies its source list's
     *  actual current appearance, exactly as before this feature existed. */
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

    // --- Themes ---

    /** Populates the built-in themes on first run and after loading an old config
     *  that predates this feature. Built-ins are looked up by name and only added
     *  if missing, so this is safe to call on every load without clobbering a
     *  user's own edits to a same-named... well, users can't rename/overwrite
     *  built-ins in the first place (see {@link #saveTheme}/{@link #deleteTheme}),
     *  so built-in entries here are always exactly these hardcoded presets. Also
     *  guarantees defaultThemeName always resolves to a real entry. */
    public void backFillThemes() {
        if (themes == null) themes = new LinkedHashMap<>();

        themes.putIfAbsent(BUILTIN_DEFAULT_THEME, builtinClassicTheme());
        themes.putIfAbsent("Minimal", builtinMinimalTheme());
        themes.putIfAbsent("High Contrast", builtinHighContrastTheme());

        if (defaultThemeName == null || !themes.containsKey(defaultThemeName)) {
            defaultThemeName = BUILTIN_DEFAULT_THEME;
        }
    }

    /** Matches MineTuner's original, pre-Themes hardcoded appearance exactly, so
     *  every list saved before this feature shipped keeps rendering identically. */
    private static ListTheme builtinClassicTheme() {
        ListTheme t = new ListTheme(BUILTIN_DEFAULT_THEME);
        t.builtin = true;
        t.showBackground = true;
        t.textShadow = false;
        t.useCustomColor = false;
        t.overrideColor = 0xFFFFFFFF;
        t.overrideFillColor = 0xB8141820;
        t.overrideOutlineColor = 0x5E9BA9BE;
        t.textScale = 1.0f;
        t.paddingX = 6;
        t.paddingY = 5;
        return t;
    }

    /** No background, no shadow — a lightweight overlay style. */
    private static ListTheme builtinMinimalTheme() {
        ListTheme t = new ListTheme("Minimal");
        t.builtin = true;
        t.showBackground = true;
        t.textShadow = false;
        t.useCustomColor = false;
        t.overrideColor = 0xFFFFFFFF;
        t.overrideFillColor = 0xB8141820;
        t.overrideOutlineColor = 0x00000000;
        t.textScale = 1.0f;
        t.paddingX = 3;
        t.paddingY = 3;
        return t;
    }

    /** Bold, easy-to-read style: background on, shadow on, larger text. */
    private static ListTheme builtinHighContrastTheme() {
        ListTheme t = new ListTheme("High Contrast");
        t.builtin = true;
        t.showBackground = true;
        t.textShadow = false;
        t.useCustomColor = true;
        t.overrideColor = 0xFFFFFF00;
        t.overrideFillColor = 0xFF000000;
        t.overrideOutlineColor = 0xFFFFFFFF;
        t.textScale = 1.0f;
        t.paddingX = 6;
        t.paddingY = 5;
        return t;
    }

    /** The theme {@link #defaultThemeName} refers to, or the built-in default if
     *  it's ever dangling (defensive — {@link #deleteTheme} already resets
     *  defaultThemeName at delete-time, but this guards any other stale case). */
    public ListTheme resolveDefaultTheme() {
        ListTheme t = themes.get(defaultThemeName);
        if (t != null) return t;
        ListTheme fallback = themes.get(BUILTIN_DEFAULT_THEME);
        return fallback != null ? fallback : builtinClassicTheme();
    }

    /** Saves {@code lc}'s current appearance as a theme under {@code name}: creates
     *  a new theme, or re-captures an existing user-created one in place. Returns
     *  false (no-op) if {@code name} already names a built-in theme, since
     *  built-ins can't be overwritten by "save/re-save" — callers should keep the
     *  Theme panel from offering this in the first place rather than relying on
     *  this false return to surface the failure.
     *
     *  <p>Typing an EXISTING user-created theme's name into the Theme panel's
     *  "+ Save as new theme" prompt re-captures that theme in place rather than
     *  erroring or creating a duplicate — this is how "update an existing
     *  theme" is supported without a separate re-save control/interaction in
     *  the panel itself (see item 6 in the feature spec, which allows deferring
     *  a dedicated update action if it would complicate the panel). */
    public boolean saveTheme(String name, StatListConfig lc) {
        if (name == null || name.isBlank()) return false;
        ListTheme existing = themes.get(name);
        if (existing != null && existing.builtin) return false;
        if (existing != null) {
            existing.recaptureFrom(lc);
        } else {
            themes.put(name, ListTheme.captureFrom(name, lc));
        }
        return true;
    }

    /** Deletes a user-created theme by name. No-op (returns false) for built-ins
     *  or unknown names. If defaultThemeName pointed at the deleted theme, resets
     *  it back to the built-in default here at delete-time — the single place this
     *  needs handling, rather than a fallback check scattered across every read
     *  site (resolveDefaultTheme() above still guards defensively either way). */
    public boolean deleteTheme(String name) {
        ListTheme t = themes.get(name);
        if (t == null || t.builtin) return false;
        themes.remove(name);
        if (name.equals(defaultThemeName)) defaultThemeName = BUILTIN_DEFAULT_THEME;
        return true;
    }

    /** Writes the config atomically. */
    public void save() {
        clampGuiTuning();
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
            MineTunerMod.LOGGER.error("Failed to save config: {}", e.getMessage());
            // Best-effort cleanup so a failed save doesn't leave a stray .tmp
            // file behind to confuse the next save attempt.
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
            }
        }
    }

    /** Which screen corner a list is anchored to. */
    public enum Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    /** How this list snaps to the vertical centre line (x-axis). */
    public enum SnapX {
        NONE,
        LEFT_ON_CENTER,    // left edge on the centre line.
        CENTER_ON_CENTER,  // centre on the centre line.
        RIGHT_ON_CENTER    // right edge on the centre line.
    }

    /** How this list snaps to the horizontal centre line (y-axis). */
    public enum SnapY {
        NONE,
        TOP_ON_CENTER,     // top edge on the centre line.
        CENTER_ON_CENTER,  // centre on the centre line.
        BOTTOM_ON_CENTER   // bottom edge on the centre line.
    }

    public enum Stat {
        // --- Performance ---
        TPS, MSPT, FPS, FRAMETIME, FPS_AVG, FPS_MIN, FPS_MAX, FPS_1PCT_LOW, FPS_01PCT_LOW, PING, MEMORY, CPU,
        ENTITIES, CHUNKS, RENDERED_SECTIONS,
        COORDS, X, Y, Z, FACING, YAW, PITCH, SPEED, GC_TIME,
        BIOME, LIGHT_LEVEL, DIMENSION,
        // --- Player vitals ---
        HEALTH, HUNGER, SATURATION, ARMOR, AIR,
        XP_LEVEL, XP_PROGRESS, GAME_MODE, SELECTED_SLOT, HELD_ITEM,
        // --- World state ---
        WEATHER, DIFFICULTY,
        SKY_LIGHT, BLOCK_LIGHT, CAN_SEE_SKY,
        // --- Misc world/player ---
        PLAYERS_ONLINE, DISTANCE_FROM_SPAWN, CHUNK_POS, VERTICAL_SPEED,
        // --- Targeting ---
        LOOKING_AT, MOVING,
        // --- Hardware sensors ---
        GPU_TEMP, GPU_CLOCK, GPU_USAGE, VRAM_USED
    }

    /** Groups {@link Stat} constants for the redesigned toggle/reorder panel — see
     *  {@link #categoryOf(Stat)} for the stat-to-category mapping. */
    public enum StatCategory {
        PERFORMANCE, PLAYER, WORLD, POSITION
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

    public static class StatSettings {
        /** Show the label prefix (e.g. "TPS: " before "20.0") in classic text mode. */
        public boolean showPrefix = true;
        /** Decimal places for stats that support it (TPS, MSPT, CPU, Speed). */
        public int decimals = 1;
        /** Render as a graph instead of text. */
        public boolean renderAsGraph = false;
        /** Graph visuals, used only when renderAsGraph is true. */
        public GraphStyle graphStyle = new GraphStyle();

        public StatSettings copy() {
            StatSettings c = new StatSettings();
            c.showPrefix = showPrefix;
            c.decimals = decimals;
            c.renderAsGraph = renderAsGraph;
            c.graphStyle = (graphStyle != null ? graphStyle : new GraphStyle()).copy();
            return c;
        }
    }

    /** Per-graph visual settings. */
    public static class GraphStyle {
        public boolean showPanelBackground = true;
        public boolean showGridlines = true;
        public boolean showPeakMarkers = true;
        public GraphValueDisplay valueDisplay = GraphValueDisplay.CURRENT; // NONE, CURRENT, MIN_CURRENT_MAX.
        public int smoothing = 0; // 0 = off, else moving-average window size (2/3/4).
        public boolean autoScale = true;
        public float fixedMin = 0f;
        public float fixedMax = 100f;
        public int width = 80;   // px.
        public int height = 28;  // px.
        public GraphColorMode colorMode = GraphColorMode.CURRENT_THRESHOLD; // CURRENT_THRESHOLD, PER_SEGMENT_THRESHOLD, FIXED_ACCENT, GRADIENT.
        public int accentColor = 0xFF55FF55; // used when colorMode == FIXED_ACCENT.

        public GraphStyle() {
        }

        /** Field-by-field copy. */
        public GraphStyle copy() {
            GraphStyle c = new GraphStyle();
            c.showPanelBackground = showPanelBackground;
            c.showGridlines = showGridlines;
            c.showPeakMarkers = showPeakMarkers;
            c.valueDisplay = valueDisplay;
            c.smoothing = smoothing;
            c.autoScale = autoScale;
            c.fixedMin = fixedMin;
            c.fixedMax = fixedMax;
            c.width = width;
            c.height = height;
            c.colorMode = colorMode;
            c.accentColor = accentColor;
            return c;
        }
    }

    /** A user-configurable two-cutoff threshold for a stat's green/yellow/red. */
    public static class ThresholdSettings {
        /** false = ignore goodMin/warnMin and use the built-in default. */
        public boolean enabled = false;
        /** Cutoff for the "good" (green) color band. Whether higher or lower values
         *  count as better depends on the stat (e.g. TPS/FPS are higher-is-better,
         *  while ping/CPU%/GPU temp are lower-is-better) — see each StatDefinition's
         *  color() method for the exact comparison it uses. */
        public float goodMin;
        /** Cutoff for the "warning" (yellow) color band; below/above this (per the
         *  same higher-or-lower-is-better direction as goodMin) is "bad" (red). */
        public float warnMin;

        public ThresholdSettings() {
        }

        public ThresholdSettings(boolean enabled, float goodMin, float warnMin) {
            this.enabled = enabled;
            this.goodMin = goodMin;
            this.warnMin = warnMin;
        }

        public ThresholdSettings copy() {
            return new ThresholdSettings(this.enabled, this.goodMin, this.warnMin);
        }
    }

    public static class StatListConfig {
        /** Unique id, starts from 0. */
        public int id;
        /** User-visible name shown in the GUI. */
        public String name;

        // Per-stat enabled flags, display order, and individual settings.
        public Map<String, Boolean> statEnabled = defaultEnabledMap();
        public List<String> statOrder = defaultOrder();
        public Map<String, StatSettings> statSettings = new LinkedHashMap<>();
        /** Per-stat color thresholds, keyed by Stat.name(). */
        public Map<String, ThresholdSettings> statThresholds = defaultThresholds();

        // Position.
        public Corner anchorCorner = Corner.TOP_LEFT;
        /** Offset from the anchor corner's horizontal edge, normalized as a fraction of
         *  the screen's width so the list stays in the same relative spot at any GUI scale. */
        public double anchorFracX = 0.01;
        /** Offset from the anchor corner's vertical edge, normalized as a fraction of
         *  the screen's height so the list stays in the same relative spot at any GUI scale. */
        public double anchorFracY = 0.01;

        /** Legacy raw-pixel offsets from before positions were normalized. */
        public Integer anchorDx = null;
        public Integer anchorDy = null;

        // Appearance.
        public boolean showBackground = true;
        public boolean textShadow = false;
        /** When true, overrideColor replaces each stat's normal/threshold color. */
        public boolean useCustomColor = false;
        /** ARGB color used when useCustomColor is true. */
        public int overrideColor = 0xFFFFFFFF;

        public int overrideFillColor = 0xB8141820;
        public int overrideOutlineColor = 0x5E9BA9BE;
        /** Text scale multiplier for this list, 0.5–2.0. */
        public float textScale = 1.0f;
        /** Horizontal content padding in GUI pixels at 1x scale. */
        public int paddingX = 6;
        /** Vertical content padding in GUI pixels at 1x scale. */
        public int paddingY = 5;

        // Snap.
        public SnapX snapX = SnapX.NONE;
        public SnapY snapY = SnapY.NONE;

        /** false (default) = classic per-stat-line mode. */
        public boolean useTemplate = false;
        /** One markup string per rendered line, used only when {@link #useTemplate} is true. */
        public List<String> templateLines = new ArrayList<>();

        public StatListConfig() {
        }

        public StatListConfig(int id) {
            this.id = id;
            this.name = "List " + id;
        }

        /** TPS/MSPT/FPS start enabled so a brand-new list shows something useful out of the
         *  box; every other stat starts disabled until the user opts in via the editor. */
        private static Map<String, Boolean> defaultEnabledMap() {
            Map<String, Boolean> m = new LinkedHashMap<>();
            for (Stat s : Stat.values()) {
//                m.put(s.name(), s == Stat.TPS || s == Stat.MSPT || s == Stat.FPS);
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
                } catch (IllegalArgumentException ignored) {
                }
            }
            return result;
        }

        /** Gets (or lazily creates) the settings for a stat. */
        public StatSettings getStatSettings(Stat stat) {
            boolean isNew = !statSettings.containsKey(stat.name());
            StatSettings ss = statSettings.computeIfAbsent(stat.name(), k -> new StatSettings());
            if (ss.graphStyle == null) ss.graphStyle = new GraphStyle();
            if (isNew) ss.decimals = StatRegistry.get(stat).defaultDecimals();
            return ss;
        }

        /** The custom threshold for a stat, or null if the stat isn't threshold-capable
         *  (i.e. not in {@link MineTunerConfig#THRESHOLD_STATS}). */
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
            // configs) into normalized anchorFracX/Y, dividing by a reference
            // 1x-GUI-scale-ish screen size at save time, which we don't know
            // anymore but approximate with REFERENCE_W/H below. This keeps
            // existing lists close to where they were, and from then on their
            // position is scale-stable. anchorDx/Dy are nulled out after
            // migrating so this doesn't re-run on later loads and doesn't
            // clobber a position the user has since re-dragged.
            if (anchorDx != null || anchorDy != null) {
                final double REFERENCE_W = 320.0; // matches Minecraft's default GUI-scaled width at scale 1 on a common 1080p.
                final double REFERENCE_H = 240.0;
                anchorFracX = (anchorDx != null ? anchorDx : 4) / REFERENCE_W;
                anchorFracY = (anchorDy != null ? anchorDy : 4) / REFERENCE_H;
                anchorDx = null;
                anchorDy = null;
            }

            for (Stat s : Stat.values()) {
                statEnabled.putIfAbsent(s.name(), false);
                if (!statOrder.contains(s.name())) statOrder.add(s.name());
                StatSettings ss = statSettings.computeIfAbsent(s.name(),
                        k -> {
                            StatSettings fresh = new StatSettings();
                            fresh.decimals = StatRegistry.get(s).defaultDecimals();
                            return fresh;
                        });
                if (ss.graphStyle == null) ss.graphStyle = new GraphStyle();
            }
            // Built once, not per-stat: defaultThresholds() allocates a whole new
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

        /** Deep-copies this list's settings into a brand-new list with the given id,
         *  nested mutable maps included, so editing the copy can never affect the original. */
        public StatListConfig duplicate(int newId) {
            StatListConfig copy = new StatListConfig(newId);
            copy.name = displayName() + " (copy)";
            copy.statEnabled = new LinkedHashMap<>(statEnabled);
            copy.statOrder = new ArrayList<>(statOrder);

            copy.statSettings = new LinkedHashMap<>();
            statSettings.forEach((key, src) -> copy.statSettings.put(key, src.copy()));

            copy.statThresholds = new LinkedHashMap<>();
            statThresholds.forEach((key, src) -> copy.statThresholds.put(key, src.copy()));

            copy.anchorCorner = anchorCorner;
            // Nudge the copy so it doesn't sit exactly on top of the original;
            // 0.02 of screen size lands close to the old 12px-at-reference-size
            // nudge without hardcoding a pixel amount that would drift when
            // rendered at a different GUI scale than duplication happened at.
            copy.anchorFracX = anchorFracX + 0.02;
            copy.anchorFracY = anchorFracY + 0.02;
            copy.showBackground = showBackground;
            copy.textShadow = textShadow;
            copy.useCustomColor = useCustomColor;
            copy.overrideColor = overrideColor;
            copy.textScale = textScale;
            copy.paddingX = paddingX;
            copy.paddingY = paddingY;
            copy.snapX = snapX;
            copy.snapY = snapY;
            copy.useTemplate = useTemplate;
            copy.templateLines = new ArrayList<>(templateLines);
            return copy;
        }
    }
}
