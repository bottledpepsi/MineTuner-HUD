package bottled.mtss.sample.sources;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.sample.Cadence;
import bottled.mtss.sample.SamplingContext;
import bottled.mtss.sample.StatSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.storage.LevelData;

/** World/session-level readings that aren't tied to the player's exact block. */
public final class WorldStateSource implements StatSource {
    /** Lowercase name for a { Difficulty}, matching vanilla's own lowercase. */
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

        MtssDataHolder.isRaining = level.isRaining();
        MtssDataHolder.isThundering = level.isThundering();
        MtssDataHolder.difficultyName = difficultyName(level.getDifficulty());

        MtssDataHolder.playersOnline = ctx.conn() != null ? ctx.conn().getOnlinePlayers().size() : 0;

        if (ctx.hasPlayer()) {
            BlockPos pos = ctx.player().blockPosition();
            MtssDataHolder.chunkX = pos.getX() >> 4;
            MtssDataHolder.chunkZ = pos.getZ() >> 4;

            MtssDataHolder.skyLight = level.getBrightness(LightLayer.SKY, pos);
            MtssDataHolder.blockLight = level.getBrightness(LightLayer.BLOCK, pos);
            MtssDataHolder.canSeeSky = level.canSeeSky(pos);

            // Spawn is a RespawnData record ( LevelData), not a bare.
            // BlockPos accessor.
            // shared/respawn position.
            LevelData.RespawnData respawn = level.getRespawnData();
            BlockPos spawn = respawn.pos();
            double dx = ctx.player().getX() - spawn.getX();
            double dz = ctx.player().getZ() - spawn.getZ();
            MtssDataHolder.distanceFromSpawn = Math.sqrt(dx * dx + dz * dz);
        }
    }
}

