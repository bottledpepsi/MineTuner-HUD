package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Current game mode (survival/creative/adventure/spectator). */
public final class GameModeStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.GAME_MODE;
    }

    @Override
    public String token() {
        return "gamemode";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedGameMode();
    }
}
