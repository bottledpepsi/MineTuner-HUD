package bottled.minetuner.stat.stats;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.stat.StatDefinition;

/** Whether your current block has a clear path to the sky (useful for judging
 *  mob-spawn eligibility or whether it's safe to build without a roof). */
public final class CanSeeSkyStat implements StatDefinition {

    @Override
    public MineTunerConfig.Stat key() {
        return MineTunerConfig.Stat.CAN_SEE_SKY;
    }

    @Override
    public String token() {
        return "canseesky";
    }

    @Override
    public String format(int decimals) {
        return MineTunerDataHolder.getFormattedCanSeeSky();
    }
}
