package bottled.mtss.stat;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.stats.BiomeStat;
import bottled.mtss.stat.stats.ChunksStat;
import bottled.mtss.stat.stats.CoordsStat;
import bottled.mtss.stat.stats.CpuStat;
import bottled.mtss.stat.stats.DimensionStat;
import bottled.mtss.stat.stats.EntitiesStat;
import bottled.mtss.stat.stats.FacingStat;
import bottled.mtss.stat.stats.FpsStat;
import bottled.mtss.stat.stats.GcTimeStat;
import bottled.mtss.stat.stats.LightLevelStat;
import bottled.mtss.stat.stats.MemoryStat;
import bottled.mtss.stat.stats.MsptStat;
import bottled.mtss.stat.stats.PingStat;
import bottled.mtss.stat.stats.RenderedSectionsStat;
import bottled.mtss.stat.stats.SpeedStat;
import bottled.mtss.stat.stats.TpsStat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central lookup from {@link MtssConfig.Stat} to its {@link StatDefinition}.
 * <p>
 * <b>To add a new stat:</b>
 * <ol>
 *   <li>Add a constant to {@link MtssConfig.Stat}.</li>
 *   <li>Write a class in {@code bottled.mtss.stat.stats} implementing
 *       {@link StatDefinition} (copy the smallest existing one, e.g.
 *       {@code EntitiesStat}, as a starting point).</li>
 *   <li>Register an instance of it below.</li>
 *   <li>Add its lang keys ({@code stat.mtss.<name>} and {@code mtss.stat.<name>})
 *       to {@code en_us.json}.</li>
 * </ol>
 * That's it — the GUI, renderer, and template engine all read this registry
 * and need no further changes for a plain text/graph stat. Anything reading
 * live game/JVM state should pull it from {@link bottled.mtss.MtssDataHolder}'s
 * raw fields, same as the existing stats do.
 */
public final class StatRegistry {

    private StatRegistry() {}

    private static final Map<MtssConfig.Stat, StatDefinition> BY_KEY = new LinkedHashMap<>();
    private static final Map<String, StatDefinition> BY_TOKEN = new LinkedHashMap<>();

    private static void register(StatDefinition def) {
        BY_KEY.put(def.key(), def);
        BY_TOKEN.put(def.token(), def);
    }

    static {
        register(new TpsStat());
        register(new MsptStat());
        register(new FpsStat());
        register(new PingStat());
        register(new MemoryStat());
        register(new CpuStat());
        register(new EntitiesStat());
        register(new ChunksStat());
        register(new RenderedSectionsStat());
        register(new CoordsStat());
        register(new FacingStat());
        register(new SpeedStat());
        register(new GcTimeStat());
        register(new BiomeStat());
        register(new LightLevelStat());
        register(new DimensionStat());
    }

    /** Looks up a stat's definition. Every {@link MtssConfig.Stat} constant must have one registered above. */
    public static StatDefinition get(MtssConfig.Stat stat) {
        StatDefinition def = BY_KEY.get(stat);
        if (def == null) throw new IllegalStateException(
                "No StatDefinition registered for " + stat + " — add one in StatRegistry's static block.");
        return def;
    }

    /** Looks up a stat by its Template Mode token name (case already normalized by the caller), or null if unrecognized. */
    public static StatDefinition byToken(String token) {
        return BY_TOKEN.get(token);
    }

    /** All registered definitions, in registration order (matches {@link MtssConfig.Stat} declaration order). */
    public static List<StatDefinition> all() {
        return List.copyOf(BY_KEY.values());
    }
}
