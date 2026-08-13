package bottled.mtss.stat;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.stats.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Central lookup from {@link MtssConfig.Stat} to its {@link StatDefinition}. */
public final class StatRegistry {

    private static final Map<MtssConfig.Stat, StatDefinition> BY_KEY = new LinkedHashMap<>();
    private static final Map<String, StatDefinition> BY_TOKEN = new LinkedHashMap<>();

    static {
        register(new TpsStat());
        register(new MsptStat());
        register(new FpsStat());
        register(new FrametimeStat());
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

        // --- Player vitals ---
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

        // --- World state ---
        register(new WeatherStat());
        register(new DifficultyStat());
        register(new SkyLightStat());
        register(new BlockLightStat());
        register(new CanSeeSkyStat());

        // --- Misc world/player ---
        register(new PlayersOnlineStat());
        register(new ChunkPosStat());
        register(new DistanceFromSpawnStat());

        // --- Targeting ---
        register(new LookingAtStat());
        register(new MovingStat());

        // --- Hardware sensors, via LibreHardwareMonitor ---
        // Off by default and simply don't render unless
        // MtssConfig.hardwareSensorsEnabled is true and LHM is reachable — see
        // HardwareSensorPoller's class doc for the full design.
        register(new GpuTempStat());
        register(new GpuClockStat());
        register(new GpuUsageStat());
        register(new VramUsedStat());
    }

    private StatRegistry() {
    }

    private static void register(StatDefinition def) {
        BY_KEY.put(def.key(), def);
        BY_TOKEN.put(def.token(), def);
    }

    /** Looks up a stat's definition. */
    public static StatDefinition get(MtssConfig.Stat stat) {
        StatDefinition def = BY_KEY.get(stat);
        if (def == null) throw new IllegalStateException(
                "No StatDefinition registered for " + stat + " — add one in StatRegistry's static block.");
        return def;
    }

    /** Looks up a stat by its Template Mode token name (case already normalized by
     *  the caller — see {@link bottled.mtss.hud.TemplateEngine#tryParseTokenBody}). */
    public static StatDefinition byToken(String token) {
        return BY_TOKEN.get(token);
    }

    /** All registered definitions, in registration order — which mirrors the {@link
     *  MtssConfig.Stat} enum's grouping by category but isn't guaranteed identical to
     *  the enum's exact declaration order stat-for-stat (e.g. VERTICAL_SPEED is
     *  registered alongside the other player-vitals stats here, but declared later,
     *  grouped with PLAYERS_ONLINE/CHUNK_POS/etc., in the enum itself). Order only
     *  matters here insofar as {@link ReorderPanel}'s category grouping cares about
     *  it; {@link #get}/{@link #byToken} are order-independent map lookups. */
    public static List<StatDefinition> all() {
        return List.copyOf(BY_KEY.values());
    }
}
