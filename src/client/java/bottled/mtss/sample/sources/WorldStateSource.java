package bottled.mtss.sample.sources;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.sample.Cadence;
import bottled.mtss.sample.SamplingContext;
import bottled.mtss.sample.StatSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.storage.LevelData;

/**
 * World/session-level readings that aren't tied to the player's exact block
 * (unlike {@link PlayerEnvironmentSource}'s light/biome, which are position
 * queries): weather, difficulty, and the current player-list size. Also
 * covers chunk position and distance-from-spawn, which need the player's
 * position but are otherwise "where am I in the world" rather than "what's
 * around me" — grouped here rather than in {@link PlayerPositionSource} to
 * keep that source's scope to raw position/orientation/speed, matching its
 * existing doc comment.
 */
public final class WorldStateSource implements StatSource {
    @Override public String id() { return "world_state"; }
    @Override public Cadence cadence() { return Cadence.PER_FRAME; }
    @Override public boolean isAvailable(SamplingContext ctx) { return ctx.hasLevel(); }

    @Override public void sample(SamplingContext ctx) {
        var level = ctx.level();

        MtssDataHolder.isRaining      = level.isRaining();
        MtssDataHolder.isThundering   = level.isThundering();
        MtssDataHolder.difficultyName = difficultyName(level.getDifficulty());

        MtssDataHolder.playersOnline = ctx.conn() != null ? ctx.conn().getOnlinePlayers().size() : 0;

        if (ctx.hasPlayer()) {
            BlockPos pos = ctx.player().blockPosition();
            MtssDataHolder.chunkX = pos.getX() >> 4;
            MtssDataHolder.chunkZ = pos.getZ() >> 4;

            MtssDataHolder.skyLight   = level.getBrightness(LightLayer.SKY, pos);
            MtssDataHolder.blockLight = level.getBrightness(LightLayer.BLOCK, pos);
            MtssDataHolder.canSeeSky  = level.canSeeSky(pos);

            // Spawn is a RespawnData record (see LevelData), not a bare
            // BlockPos accessor — getRespawnData().pos() is the world's
            // shared/respawn position.
            LevelData.RespawnData respawn = level.getRespawnData();
            BlockPos spawn = respawn.pos();
            double dx = ctx.player().getX() - spawn.getX();
            double dz = ctx.player().getZ() - spawn.getZ();
            MtssDataHolder.distanceFromSpawn = Math.sqrt(dx * dx + dz * dz);
        }
    }

    /**
     * Lowercase name for a {@link Difficulty}, matching vanilla's own
     * lowercase identifiers (peaceful/easy/normal/hard) rather than the
     * enum constant's own SCREAMING_CASE name.
     */
    private static String difficultyName(Difficulty difficulty) {
        return difficulty.name().toLowerCase(java.util.Locale.ROOT);
    }
}

