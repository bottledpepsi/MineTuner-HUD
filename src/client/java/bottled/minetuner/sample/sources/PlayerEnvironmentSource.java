package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;
import net.minecraft.core.BlockPos;

import java.util.Set;

/** Environmental readings at the player's current block. */
public final class PlayerEnvironmentSource implements StatSource {
    @Override
    public String id() {
        return "player_environment";
    }

    @Override
    public Set<MineTunerConfig.Stat> feeds() {
        return Set.of(MineTunerConfig.Stat.LIGHT_LEVEL, MineTunerConfig.Stat.BIOME);
    }

    @Override
    public Cadence cadence() {
        return Cadence.PER_TICK;
    }

    @Override
    public boolean isAvailable(SamplingContext ctx) {
        return ctx.hasPlayer() && ctx.hasLevel();
    }

    @Override
    public void sample(SamplingContext ctx) {
        BlockPos pos = ctx.player().blockPosition();
        MineTunerDataHolder.lightLevel = ctx.level().getMaxLocalRawBrightness(pos);
        var biomeHolder = ctx.level().getBiome(pos);
        MineTunerDataHolder.biomeName = biomeHolder.unwrapKey()
                .map(key -> key.identifier().getPath())
                .orElse("?");
    }
}
