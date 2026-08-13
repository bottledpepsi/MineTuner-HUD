package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.storage.LevelData;

/** World/session-level readings that aren't tied to the player's exact block. */
public final class WorldStateSource implements StatSource {
    /** Lowercase name for a {@link Difficulty}, matching vanilla's own lowercase
     *  naming convention for difficulty levels (as used by e.g. the /difficulty
     *  command). Capitalized for display later, in MineTunerDataHolder.getFormattedDifficulty(). */
    private static String difficultyName(Difficulty difficulty) {
        return difficulty.name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public String id() {
        return "world_state";
    }

    @Override
    public Cadence cadence() {
        return Cadence.PER_FRAME;
    }

    @Override
    public boolean isAvailable(SamplingContext ctx) {
        return ctx.hasLevel();
    }

    @Override
    public void sample(SamplingContext ctx) {
        var level = ctx.level();

        MineTunerDataHolder.isRaining = level.isRaining();
        MineTunerDataHolder.isThundering = level.isThundering();
        MineTunerDataHolder.difficultyName = difficultyName(level.getDifficulty());

        MineTunerDataHolder.playersOnline = ctx.conn() != null ? ctx.conn().getOnlinePlayers().size() : 0;

        if (ctx.hasPlayer()) {
            BlockPos pos = ctx.player().blockPosition();
            MineTunerDataHolder.chunkX = pos.getX() >> 4;
            MineTunerDataHolder.chunkZ = pos.getZ() >> 4;

            MineTunerDataHolder.skyLight = level.getBrightness(LightLayer.SKY, pos);
            MineTunerDataHolder.blockLight = level.getBrightness(LightLayer.BLOCK, pos);
            MineTunerDataHolder.canSeeSky = level.canSeeSky(pos);

            // Spawn is a RespawnData record ({@link LevelData}), not a bare BlockPos
            // accessor — respawn.pos() below extracts the position component.
            LevelData.RespawnData respawn = level.getRespawnData();
            BlockPos spawn = respawn.pos();
            double dx = ctx.player().getX() - spawn.getX();
            double dz = ctx.player().getZ() - spawn.getZ();
            MineTunerDataHolder.distanceFromSpawn = Math.sqrt(dx * dx + dz * dz);
        }
    }
}

