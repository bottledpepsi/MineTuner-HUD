package bottled.mtss.stat;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.stats.AirStat;
import bottled.mtss.stat.stats.ArmorStat;
import bottled.mtss.stat.stats.BiomeStat;
import bottled.mtss.stat.stats.BlockLightStat;
import bottled.mtss.stat.stats.CanSeeSkyStat;
import bottled.mtss.stat.stats.ChunkPosStat;
import bottled.mtss.stat.stats.ChunksStat;
import bottled.mtss.stat.stats.CoordsStat;
import bottled.mtss.stat.stats.CpuStat;
import bottled.mtss.stat.stats.DifficultyStat;
import bottled.mtss.stat.stats.DimensionStat;
import bottled.mtss.stat.stats.DistanceFromSpawnStat;
import bottled.mtss.stat.stats.EntitiesStat;
import bottled.mtss.stat.stats.FacingStat;
import bottled.mtss.stat.stats.FpsStat;
import bottled.mtss.stat.stats.GameModeStat;
import bottled.mtss.stat.stats.GcTimeStat;
import bottled.mtss.stat.stats.GpuClockStat;
import bottled.mtss.stat.stats.GpuTempStat;
import bottled.mtss.stat.stats.GpuUsageStat;
import bottled.mtss.stat.stats.HealthStat;
import bottled.mtss.stat.stats.HeldItemStat;
import bottled.mtss.stat.stats.HungerStat;
import bottled.mtss.stat.stats.LightLevelStat;
import bottled.mtss.stat.stats.LookingAtStat;
import bottled.mtss.stat.stats.MemoryStat;
import bottled.mtss.stat.stats.MovingStat;
import bottled.mtss.stat.stats.MsptStat;
import bottled.mtss.stat.stats.PingStat;
import bottled.mtss.stat.stats.PitchStat;
import bottled.mtss.stat.stats.PlayersOnlineStat;
import bottled.mtss.stat.stats.RenderedSectionsStat;
import bottled.mtss.stat.stats.SaturationStat;
import bottled.mtss.stat.stats.SelectedSlotStat;
import bottled.mtss.stat.stats.SkyLightStat;
import bottled.mtss.stat.stats.SpeedStat;
import bottled.mtss.stat.stats.TpsStat;
import bottled.mtss.stat.stats.VerticalSpeedStat;
import bottled.mtss.stat.stats.VramUsedStat;
import bottled.mtss.stat.stats.WeatherStat;
import bottled.mtss.stat.stats.XStat;
import bottled.mtss.stat.stats.XpLevelStat;
import bottled.mtss.stat.stats.XpProgressStat;
import bottled.mtss.stat.stats.YStat;
import bottled.mtss.stat.stats.YawStat;
import bottled.mtss.stat.stats.ZStat;

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
        register(new XStat());
        register(new YStat());
        register(new ZStat());
        register(new FacingStat());
        register(new YawStat());
        register(new PitchStat());
        register(new SpeedStat());
        register(new GcTimeStat());
        register(new BiomeStat());
        register(new LightLevelStat());
        register(new DimensionStat());

        // ── Player vitals ────────────────────────────────────────────────
        register(new HealthStat());
        register(new HungerStat());
        register(new SaturationStat());
        register(new ArmorStat());
        register(new AirStat());
        register(new XpLevelStat());
        register(new XpProgressStat());
        register(new GameModeStat());
        register(new SelectedSlotStat());
        register(new HeldItemStat());
        register(new VerticalSpeedStat());

        // ── World / environment ─────────────────────────────────────────
        register(new WeatherStat());
        register(new DifficultyStat());
        register(new SkyLightStat());
        register(new BlockLightStat());
        register(new CanSeeSkyStat());

        // ── Server / session ─────────────────────────────────────────────
        register(new PlayersOnlineStat());
        register(new ChunkPosStat());
        register(new DistanceFromSpawnStat());

        // ── Targeting / movement ─────────────────────────────────────────
        register(new LookingAtStat());
        register(new MovingStat());

        // ── Hardware sensors (opt-in, via LibreHardwareMonitor) ───────────
        // Off by default and simply don't render unless
        // MtssConfig.hardwareSensorsEnabled is true and LHM is reachable —
        // see HardwareSensorPoller's class doc for the full design.
        register(new GpuTempStat());
        register(new GpuClockStat());
        register(new GpuUsageStat());
        register(new VramUsedStat());
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
