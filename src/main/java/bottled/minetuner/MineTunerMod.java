package bottled.minetuner;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MineTunerMod implements ModInitializer {
    public static final String MOD_ID = "minetuner";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("MineTuner HUD initialized.");
    }
}
