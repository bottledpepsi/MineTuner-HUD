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

    // ── Root config ───────────────────────────────────────────────────────────
    public int                  nextId = 1;
    public List<StatListConfig> lists  = new ArrayList<>();

    // ── Per-stat settings ─────────────────────────────────────────────────────
    public static class StatSettings {
        /** Whether to show the label prefix (e.g. "TPS: ") before the value. */
        public boolean showPrefix = true;
        /** Decimal places for numeric stats that support it (TPS, MSPT, CPU, Speed). Ignored otherwise. */
        public int decimals = 1;
        /** Render graph. */
        public boolean renderAsGraph = false;
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
            return statSettings.computeIfAbsent(stat.name(), k -> new StatSettings());
        }

        /** Back-fill any stats added since this config was written. */
        public void backFill() {
            if (name == null) name = "List " + id;
            if (statSettings == null) statSettings = new LinkedHashMap<>();
            for (Stat s : Stat.values()) {
                statEnabled.putIfAbsent(s.name(), false);
                if (!statOrder.contains(s.name())) statOrder.add(s.name());
                statSettings.computeIfAbsent(s.name(), k -> new StatSettings());
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
