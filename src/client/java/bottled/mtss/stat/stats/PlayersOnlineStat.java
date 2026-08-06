package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Number of players currently in the player list (tab list) — works on singleplayer, LAN, and remote servers alike. */
public final class PlayersOnlineStat implements StatDefinition {

    @Override public MtssConfig.Stat key() { return MtssConfig.Stat.PLAYERS_ONLINE; }
    @Override public String token() { return "players"; }
    @Override public String format(int decimals) { return MtssDataHolder.getFormattedPlayersOnline(); }
}
