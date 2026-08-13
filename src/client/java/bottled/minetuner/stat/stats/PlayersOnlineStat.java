package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Number of players currently in the player list (tab list). */
public final class PlayersOnlineStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.PLAYERS_ONLINE;
    }

    @Override
    public String token() {
        return "players";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedPlayersOnline();
    }
}
