package bottled.mtss.stat.stats;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.stat.StatDefinition;

/** Current game mode (survival/creative/adventure/spectator). */
public final class GameModeStat implements StatDefinition {

    @Override
    public MtssConfig.Stat key() {
        return MtssConfig.Stat.GAME_MODE;
    }

    @Override
    public String token() {
        return "gamemode";
    }

    @Override
    public String format(int decimals) {
        return MtssDataHolder.getFormattedGameMode();
    }
}
