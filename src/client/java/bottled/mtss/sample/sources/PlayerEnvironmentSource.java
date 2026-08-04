package bottled.mtss.sample.sources;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.sample.Cadence;
import bottled.mtss.sample.SamplingContext;
import bottled.mtss.sample.StatSource;
import net.minecraft.core.BlockPos;

/**
 * Environmental readings at the player's current block: local light level
 * and biome name. Needs both a player (for the position) and a level (for
 * the world query), unlike {@link PlayerPositionSource} which only needs
 * the player — kept separate so this source's precondition doesn't get
 * looser or stricter than what it actually reads.
 */
public final class PlayerEnvironmentSource implements StatSource {
    @Override public String id() { return "player_environment"; }
    @Override public Cadence cadence() { return Cadence.PER_FRAME; }
    @Override public boolean isAvailable(SamplingContext ctx) { return ctx.hasPlayer() && ctx.hasLevel(); }

    @Override public void sample(SamplingContext ctx) {
        BlockPos pos = ctx.player().blockPosition();
        MtssDataHolder.lightLevel = ctx.level().getMaxLocalRawBrightness(pos);
        var biomeHolder = ctx.level().getBiome(pos);
        MtssDataHolder.biomeName = biomeHolder.unwrapKey()
                .map(key -> key.identifier().getPath())
                .orElse("?");
    }
}
